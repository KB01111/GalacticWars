package galacticwars.clonewars.world;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.army.ArmyTravelService;
import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.kingdom.KingdomRecord;
import galacticwars.clonewars.kingdom.KingdomPermission;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.kingdom.KingdomActionId;
import galacticwars.clonewars.kingdom.KingdomGameplayAction;
import galacticwars.clonewars.kingdom.KingdomGameplayResult;
import galacticwars.clonewars.kingdom.KingdomGameplayRuntimeService;
import galacticwars.clonewars.kingdom.KingdomGameplayTransactionService;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.progression.ProgressionState;
import galacticwars.clonewars.settlement.CommandCenterBlockEntity;
import galacticwars.clonewars.vehicle.GalacticVehicleEntity;
import galacticwars.clonewars.vehicle.VehiclePlanetTravelPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PlanetTravelService {
    public static final String HOME_DESTINATION_ID = PlanetTravelPolicy.HOME_DESTINATION_ID;

    private PlanetTravelService() {
    }

    public static List<String> navigationDestinations() {
        LinkedHashSet<String> destinations = new LinkedHashSet<>();
        destinations.add(HOME_DESTINATION_ID);
        destinations.addAll(GameplayDataManager.snapshot().launchContent().planetIds());
        return List.copyOf(destinations);
    }

    /**
     * Captures a side-effect-free, server-authored availability snapshot for the navigation UI.
     * Travel still repeats every check immediately before moving the player.
     */
    public static List<NavigationDestination> navigationOptions(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        ServerLevel source = player.level();
        KingdomSavedData kingdoms = KingdomSavedData.get(source);
        ProgressionState state = ProgressionSavedData.get(source).state(player.getUUID());
        ResolvedCommandCenter commandCenter = resolveCommandCenter(player, kingdoms).orElse(null);
        return navigationDestinations().stream().map(destinationId -> {
            boolean returningHome = HOME_DESTINATION_ID.equals(destinationId);
            LaunchContentDefinitions.PlanetDefinition planet = returningHome
                    ? null
                    : GameplayDataManager.snapshot().launchContent().planets().get(destinationId);
            ResourceKey<Level> destinationKey = returningHome && commandCenter != null
                    ? commandCenter.level().dimension()
                    : dimensionKey(planet);
            ServerLevel destination = returningHome && commandCenter != null
                    ? commandCenter.level()
                    : destinationKey == null ? null : source.getServer().getLevel(destinationKey);
            PlanetTravelPolicy.TravelAuthorization authorization = PlanetTravelPolicy.authorize(
                    destinationId,
                    commandCenter != null
                            && commandCenter.hall().canUse(player, KingdomPermission.TRAVEL),
                    commandCenter != null,
                    !state.factionId().isEmpty(),
                    state.unlocks().contains("planet_travel"),
                    commandCenter != null && commandCenter.hall().upkeepPaid(),
                    destination != null,
                    destinationKey != null && source.dimension().equals(destinationKey));
            return new NavigationDestination(
                    destinationId, authorization.accepted(), authorization.reason(),
                    returningHome ? "home" : planet == null ? "unknown" : planet.theme(),
                    returningHome ? "command_center" : planet == null ? "unknown" : planet.arrival());
        }).toList();
    }

    public static TravelResult travel(ServerPlayer player, String destinationId) {
        ServerLevel source = player.level();
        KingdomSavedData kingdoms = KingdomSavedData.get(source);
        ProgressionSavedData progression = ProgressionSavedData.get(source);
        ProgressionState state = progression.state(player.getUUID());
        ResolvedCommandCenter commandCenter = resolveCommandCenter(player, kingdoms).orElse(null);
        boolean returningHome = HOME_DESTINATION_ID.equals(destinationId);
        LaunchContentDefinitions.PlanetDefinition planet = returningHome
                ? null
                : GameplayDataManager.snapshot().launchContent().planets().get(destinationId);
        ResourceKey<Level> destinationKey = returningHome && commandCenter != null
                ? commandCenter.level().dimension()
                : dimensionKey(planet);
        ServerLevel destination = returningHome && commandCenter != null
                ? commandCenter.level()
                : destinationKey == null ? null : source.getServer().getLevel(destinationKey);
        PlanetTravelPolicy.TravelAuthorization authorization = PlanetTravelPolicy.authorize(
                destinationId,
                commandCenter != null && commandCenter.hall().canUse(player, KingdomPermission.TRAVEL),
                commandCenter != null,
                !state.factionId().isEmpty(),
                state.unlocks().contains("planet_travel"),
                commandCenter != null && commandCenter.hall().upkeepPaid(),
                destination != null,
                destinationKey != null && source.dimension().equals(destinationKey));
        if (!authorization.accepted()) {
            return TravelResult.rejected(authorization.reason());
        }

        Optional<BlockPos> arrival = returningHome
                ? PlanetArrivalService.findHomeArrival(
                        destination,
                        commandCenter.position(),
                        player.getVehicle() instanceof GalacticVehicleEntity vehicle ? vehicle : player)
                : PlanetArrivalService.findOrCreate(
                        destination, planet,
                        GameplayDataManager.snapshot().launchContent().conquestRegions().values());
        if (arrival.isEmpty()) {
            return TravelResult.rejected("safe_arrival_unavailable");
        }
        BlockPos target = arrival.orElseThrow();
        if (!galacticwars.clonewars.conquest.ConquestRuntimeEvents
                .arrivalClear(destination, player, target)) {
            return TravelResult.rejected("hostile_arrival_blocked");
        }
        return executeAuthorizedTransfer(player, destinationId, destination, target, kingdoms, progression);
    }

    static TravelResult executeAuthorizedTransfer(
            ServerPlayer player,
            String destinationId,
            ServerLevel destination,
            BlockPos target
    ) {
        ServerLevel source = player.level();
        return executeAuthorizedTransfer(
                player,
                destinationId,
                destination,
                target,
                KingdomSavedData.get(source),
                ProgressionSavedData.get(source));
    }

    private static TravelResult executeAuthorizedTransfer(
            ServerPlayer player,
            String destinationId,
            ServerLevel destination,
            BlockPos target,
            KingdomSavedData kingdoms,
            ProgressionSavedData progression
    ) {
        ServerLevel source = player.level();
        VehiclePlanetTravelPlan vehicleTravel = VehiclePlanetTravelPlan.prepare(player, destination, target);
        if (!vehicleTravel.accepted()) {
            return TravelResult.rejected(vehicleTravel.reason());
        }
        VisitProgressionPlan visitPlan = VisitProgressionPlan.prepare(
                progression, vehicleTravel.travelers(), destinationId);
        if (!visitPlan.accepted()) {
            return TravelResult.rejected(visitPlan.reason());
        }
        ArmyTravelService.TravelPlan squadTravel = ArmyTravelService.prepare(
                kingdoms, player, destination, target);
        if (!squadTravel.accepted()) {
            return TravelResult.rejected(squadTravel.reason());
        }
        if (!squadTravel.reserve()) {
            return TravelResult.rejected("squad_transfer_conflict");
        }
        if (!visitPlan.commit()) {
            if (!squadTravel.rollback(source.getGameTime())) {
                GalacticWars.LOGGER.error(
                        "Failed to release squad travel reservation after progression rejection for {}",
                        player.getGameProfile().name());
            }
            return TravelResult.rejected(visitPlan.reason());
        }
        boolean teleported = vehicleTravel.transfersVehicle() ? vehicleTravel.transfer() : player.teleportTo(
                destination,
                target.getX() + 0.5D,
                target.getY(),
                target.getZ() + 0.5D,
                Set.<Relative>of(),
                player.getYRot(),
                player.getXRot(),
                false);
        if (!teleported) {
            if (!squadTravel.rollback(source.getGameTime())) {
                GalacticWars.LOGGER.error("Failed to roll back squad travel after player teleport failure for {}",
                        player.getGameProfile().name());
            }
            if (!visitPlan.rollback()) {
                GalacticWars.LOGGER.error(
                        "Failed to roll back one or more party visit events after teleport failure for {}",
                        player.getGameProfile().name());
            }
            return TravelResult.rejected("teleport_failed");
        }
        squadTravel.commit();
        for (ServerPlayer traveler : vehicleTravel.travelers()) {
            traveler.setRespawnPosition(new ServerPlayer.RespawnConfig(
                    LevelData.RespawnData.of(
                            destination.dimension(), target,
                            traveler.getYRot(), traveler.getXRot()), true), false);
        }
        return TravelResult.accepted(destinationId, target, squadTravel.transfersSquad());
    }

    private static final class VisitProgressionPlan {
        private final ProgressionSavedData progression;
        private final List<VisitEntry> entries;
        private boolean accepted;
        private String reason;

        private VisitProgressionPlan(
                ProgressionSavedData progression,
                List<VisitEntry> entries,
                boolean accepted,
                String reason
        ) {
            this.progression = progression;
            this.entries = entries;
            this.accepted = accepted;
            this.reason = reason;
        }

        static VisitProgressionPlan prepare(
                ProgressionSavedData progression,
                List<ServerPlayer> travelers,
                String destinationId
        ) {
            java.util.ArrayList<VisitEntry> entries = new java.util.ArrayList<>();
            if (HOME_DESTINATION_ID.equals(destinationId)) {
                return new VisitProgressionPlan(progression, entries, true, "accepted");
            }
            for (ServerPlayer traveler : travelers) {
                ProgressionState before = progression.state(traveler.getUUID());
                if (before.hasSubject(ProgressionEventType.PLANET_VISITED, destinationId)) {
                    continue;
                }
                KingdomGameplayAction action = new KingdomGameplayAction(
                        KingdomActionId.of("planet_visited", traveler.getUUID(), destinationId),
                        traveler.getUUID(), ProgressionEventType.PLANET_VISITED, destinationId, 1);
                KingdomGameplayResult evaluation = KingdomGameplayTransactionService.evaluate(before, action);
                if (!evaluation.accepted() || !evaluation.changed()) {
                    String reason = evaluation.accepted()
                            ? "progression_conflict" : evaluation.reason();
                    return new VisitProgressionPlan(progression, entries, false, reason);
                }
                entries.add(new VisitEntry(
                        traveler.getUUID(), action, before,
                        progression.hasStoredState(traveler.getUUID())));
            }
            return new VisitProgressionPlan(progression, entries, true, "accepted");
        }

        boolean commit() {
            if (!accepted) {
                return false;
            }
            for (VisitEntry entry : entries) {
                KingdomGameplayResult committed = KingdomGameplayRuntimeService.applyProgression(
                        progression, entry.action);
                if (!committed.accepted() || !committed.changed()) {
                    accepted = false;
                    reason = committed.accepted()
                            ? "progression_conflict" : committed.reason();
                    if (!rollback()) {
                        reason = "transaction_failed";
                    }
                    return false;
                }
                entry.after = committed.progressionState();
                entry.committed = true;
            }
            return true;
        }

        boolean rollback() {
            boolean restored = true;
            for (int index = entries.size() - 1; index >= 0; index--) {
                VisitEntry entry = entries.get(index);
                if (!entry.committed) {
                    continue;
                }
                boolean entryRestored = progression.restoreAfterFailedTransaction(
                        entry.playerId, entry.after, entry.before, entry.beforeWasStored);
                restored &= entryRestored;
                if (entryRestored) {
                    entry.committed = false;
                }
            }
            return restored;
        }

        boolean accepted() {
            return accepted;
        }

        String reason() {
            return reason;
        }
    }

    private static final class VisitEntry {
        private final UUID playerId;
        private final KingdomGameplayAction action;
        private final ProgressionState before;
        private final boolean beforeWasStored;
        private ProgressionState after;
        private boolean committed;

        private VisitEntry(
                UUID playerId,
                KingdomGameplayAction action,
                ProgressionState before,
                boolean beforeWasStored
        ) {
            this.playerId = playerId;
            this.action = action;
            this.before = before;
            this.beforeWasStored = beforeWasStored;
            this.after = before;
        }
    }

    public static boolean hasActiveCommandCenter(ServerPlayer player) {
        return resolveCommandCenter(player, KingdomSavedData.get(player.level())).isPresent();
    }

    private static Optional<ResolvedCommandCenter> resolveCommandCenter(
            ServerPlayer player,
            KingdomSavedData kingdoms
    ) {
        KingdomRecord kingdom = kingdoms.kingdomForPlayer(player.getUUID()).orElse(null);
        if (kingdom == null || !kingdom.allows(player.getUUID(), KingdomPermission.TRAVEL)
                || !kingdoms.isHallActive(kingdom.ownerId())) {
            return Optional.empty();
        }
        var settlement = kingdom.settlement();
        ResourceKey<Level> hallDimension = ResourceKey.create(
                Registries.DIMENSION, Identifier.parse(settlement.dimensionId()));
        ServerLevel hallLevel = player.level().getServer().getLevel(hallDimension);
        if (hallLevel == null) {
            return Optional.empty();
        }
        BlockPos hallPos = new BlockPos(settlement.hallX(), settlement.hallY(), settlement.hallZ());
        hallLevel.getChunkAt(hallPos);
        if (!(hallLevel.getBlockEntity(hallPos) instanceof CommandCenterBlockEntity hall)
                || !hall.canUse(player, KingdomPermission.TRAVEL)) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedCommandCenter(hall, hallLevel, hallPos));
    }

    private static ResourceKey<Level> dimensionKey(
            LaunchContentDefinitions.PlanetDefinition planet
    ) {
        if (planet == null) {
            return null;
        }
        try {
            return ResourceKey.create(Registries.DIMENSION, Identifier.parse(planet.dimensionId()));
        } catch (RuntimeException invalidDimension) {
            return null;
        }
    }

    public record TravelResult(
            boolean accepted,
            String reason,
            String destinationId,
            BlockPos arrival,
            boolean squadTransferred
    ) {
        private static TravelResult accepted(String destinationId, BlockPos arrival, boolean squadTransferred) {
            return new TravelResult(true, "accepted", destinationId, arrival, squadTransferred);
        }

        private static TravelResult rejected(String reason) {
            return new TravelResult(false, reason, "", BlockPos.ZERO, false);
        }
    }

    public record NavigationDestination(
            String destinationId,
            boolean available,
            String reason,
            String theme,
            String arrivalProfile
    ) {
        public NavigationDestination {
            destinationId = Objects.requireNonNull(destinationId, "destinationId").trim();
            reason = Objects.requireNonNull(reason, "reason").trim();
            theme = Objects.requireNonNull(theme, "theme").trim();
            arrivalProfile = Objects.requireNonNull(arrivalProfile, "arrivalProfile").trim();
            if (destinationId.isEmpty() || reason.isEmpty()
                    || theme.isEmpty() || arrivalProfile.isEmpty()) {
                throw new IllegalArgumentException("navigation destination fields cannot be blank");
            }
            if (available != reason.equals("accepted")) {
                throw new IllegalArgumentException(
                        "navigation availability and reason must agree");
            }
        }

        public NavigationDestination(String destinationId, boolean available, String reason) {
            this(destinationId, available, reason, "unknown", "unknown");
        }
    }

    private record ResolvedCommandCenter(
            CommandCenterBlockEntity hall,
            ServerLevel level,
            BlockPos position
    ) {
    }

}
