package galacticwars.clonewars.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.data.SavedDataSchemaPolicy;
import galacticwars.clonewars.economy.CreditTransactionService;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ProgressionSavedData extends SavedData {
    private static final Codec<Map<String, Map<String, Integer>>> EVENT_SUBJECT_TOTALS_CODEC =
            Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, Codec.INT));

    private static final Codec<PlayerState> PLAYER_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("player_id").forGetter(PlayerState::playerId),
            Codec.STRING.optionalFieldOf("faction_id", "").forGetter(PlayerState::factionId),
            Codec.INT.optionalFieldOf("credits", 0).forGetter(PlayerState::credits),
            UUIDUtil.CODEC.listOf().optionalFieldOf("processed_events", List.of()).forGetter(PlayerState::processedEvents),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("event_totals", Map.of()).forGetter(PlayerState::eventTotals),
            Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()).optionalFieldOf("event_subjects", Map.of()).forGetter(PlayerState::eventSubjects),
            EVENT_SUBJECT_TOTALS_CODEC.optionalFieldOf("event_subject_totals", Map.of())
                    .forGetter(PlayerState::eventSubjectTotals),
            Codec.STRING.listOf().optionalFieldOf("unlocks", List.of()).forGetter(PlayerState::unlocks)
    ).apply(instance, PlayerState::new));

    public static final Codec<ProgressionSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("schema_version", ProgressionState.CURRENT_SCHEMA_VERSION)
                    .forGetter(data -> ProgressionState.CURRENT_SCHEMA_VERSION),
            PLAYER_CODEC.listOf().optionalFieldOf("players", List.of()).forGetter(ProgressionSavedData::serialized)
    ).apply(instance, ProgressionSavedData::new));

    public static final SavedDataType<ProgressionSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "progression"),
            ProgressionSavedData::new,
            CODEC,
            null);

    private final Map<UUID, ProgressionState> states = new LinkedHashMap<>();

    public ProgressionSavedData() {
    }

    private ProgressionSavedData(int schemaVersion, List<PlayerState> players) {
        SavedDataSchemaPolicy.migrate(
                schemaVersion, ProgressionState.CURRENT_SCHEMA_VERSION, "progression");
        for (PlayerState player : players) {
            HashMap<ProgressionEventType, Integer> totals = new HashMap<>();
            player.eventTotals().forEach((key, value) -> totals.put(type(key), Math.max(0, value)));
            HashMap<ProgressionEventType, Set<String>> subjects = new HashMap<>();
            player.eventSubjects().forEach((key, values) ->
                    subjects.put(type(key), new LinkedHashSet<>(values)));
            HashMap<ProgressionEventType, Map<String, Integer>> subjectTotals = new HashMap<>();
            subjects.forEach((eventType, values) -> {
                LinkedHashMap<String, Integer> migrated = new LinkedHashMap<>();
                values.forEach(subject -> migrated.put(subject, 1));
                subjectTotals.put(eventType, migrated);
            });
            player.eventSubjectTotals().forEach((key, values) -> {
                LinkedHashMap<String, Integer> parsed = new LinkedHashMap<>();
                values.forEach((subject, count) -> {
                    if (subject != null && !subject.isBlank() && count != null && count > 0) {
                        parsed.put(subject, count);
                    }
                });
                subjectTotals.put(type(key), parsed);
            });
            states.put(player.playerId(), new ProgressionState(
                    ProgressionState.CURRENT_SCHEMA_VERSION,
                    player.playerId(),
                    player.factionId(),
                    Math.max(0, player.credits()),
                    new LinkedHashSet<>(player.processedEvents()),
                    totals,
                    subjects,
                    subjectTotals,
                    Set.copyOf(player.unlocks())));
        }
    }

    public static ProgressionSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public ProgressionState state(UUID playerId) {
        return states.getOrDefault(playerId, ProgressionState.create(playerId));
    }

    public boolean hasStoredState(UUID playerId) {
        return states.containsKey(playerId);
    }

    /**
     * Compare-and-restore hook for a server-tick gameplay transaction that failed after
     * progression committed. It refuses to overwrite intervening progression changes.
     */
    public boolean restoreAfterFailedTransaction(
            UUID playerId,
            ProgressionState expectedCurrent,
            ProgressionState previous,
            boolean previousWasStored
    ) {
        if (!state(playerId).equals(expectedCurrent) || !previous.playerId().equals(playerId)) {
            return false;
        }
        if (previousWasStored) {
            states.put(playerId, previous);
        } else {
            states.remove(playerId);
        }
        this.setDirty();
        return true;
    }

    public ProgressionDecision apply(ProgressionEvent event) {
        ProgressionState before = state(event.playerId());
        ProgressionDecision decision = CampaignRuntimeService.record(before, event);
        if (decision.accepted() && decision.changed()) {
            states.put(event.playerId(), decision.state());
            this.setDirty();
        }
        return decision;
    }

    public ProgressionDecision commitEvaluated(
            ProgressionEvent event,
            ProgressionState expectedState,
            ProgressionDecision evaluated
    ) {
        ProgressionState current = state(event.playerId());
        if (!current.equals(expectedState)) {
            return apply(event);
        }
        if (!evaluated.accepted() || !evaluated.changed()
                || !evaluated.state().playerId().equals(event.playerId())
                || !evaluated.state().processed(event.id())) {
            throw new IllegalArgumentException("invalid evaluated progression decision");
        }
        ProgressionState completed = CampaignRuntimeService.completeEligibleQuests(evaluated.state());
        states.put(event.playerId(), completed);
        this.setDirty();
        return new ProgressionDecision(true, true, "accepted", completed);
    }

    public int pendingCreditRewards(UUID playerId) {
        return state(playerId).pendingCreditRewards();
    }

    /** Atomically retires pending numeric rewards and materializes them as Credit Chip items. */
    public int claimCreditRewards(ServerPlayer player) {
        ProgressionState current = state(player.getUUID());
        int amount = current.pendingCreditRewards();
        if (amount <= 0) {
            return 0;
        }
        states.put(player.getUUID(), current.clearPendingCreditRewards());
        this.setDirty();
        CreditTransactionService.refundPlayer(player, amount);
        return amount;
    }

    private List<PlayerState> serialized() {
        ArrayList<PlayerState> players = new ArrayList<>();
        for (ProgressionState state : states.values()) {
            LinkedHashMap<String, Integer> totals = new LinkedHashMap<>();
            state.eventTotals().forEach((type, value) -> totals.put(key(type), value));
            LinkedHashMap<String, List<String>> subjects = new LinkedHashMap<>();
            state.eventSubjects().forEach((type, values) ->
                    subjects.put(key(type), List.copyOf(values)));
            LinkedHashMap<String, Map<String, Integer>> subjectTotals = new LinkedHashMap<>();
            state.eventSubjectTotals().forEach((type, values) ->
                    subjectTotals.put(key(type), Map.copyOf(values)));
            players.add(new PlayerState(
                    state.playerId(), state.factionId(), state.credits(),
                    List.copyOf(state.processedEventIds()), totals, subjects, subjectTotals,
                    state.unlocks().stream().sorted().toList()));
        }
        return List.copyOf(players);
    }

    private static String key(ProgressionEventType type) {
        return type.name().toLowerCase(Locale.ROOT);
    }

    private static ProgressionEventType type(String value) {
        if (value.equalsIgnoreCase("force_ability_unlocked")) {
            return ProgressionEventType.FORCE_ABILITY_USED;
        }
        return ProgressionEventType.valueOf(value.toUpperCase(Locale.ROOT));
    }

    private record PlayerState(
            UUID playerId,
            String factionId,
            int credits,
            List<UUID> processedEvents,
            Map<String, Integer> eventTotals,
            Map<String, List<String>> eventSubjects,
            Map<String, Map<String, Integer>> eventSubjectTotals,
            List<String> unlocks
    ) {
        private PlayerState {
            processedEvents = List.copyOf(processedEvents);
            eventTotals = Map.copyOf(eventTotals);
            eventSubjects = Map.copyOf(eventSubjects);
            LinkedHashMap<String, Map<String, Integer>> copiedSubjectTotals = new LinkedHashMap<>();
            eventSubjectTotals.forEach((type, values) ->
                    copiedSubjectTotals.put(type, Map.copyOf(values)));
            eventSubjectTotals = Map.copyOf(copiedSubjectTotals);
            unlocks = List.copyOf(unlocks);
        }
    }
}
