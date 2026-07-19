package galacticwars.clonewars.progression;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class KotlinProgressionValueInteropTest {
    public static void main(String[] args) {
        progressionEventRetainsRecordStyleJavaApi();
        progressionStateOwnsDeepImmutableCopies();
        progressionReplayLedgersRemainBoundedAndAggregate();
        forceStateBoundsAndOwnsImmutableCopies();
        decisionsRetainRecordStyleJavaApi();
        System.out.println("KotlinProgressionValueInteropTest passed");
    }

    private static void progressionEventRetainsRecordStyleJavaApi() {
        UUID id = UUID.randomUUID();
        UUID player = UUID.randomUUID();
        ProgressionEvent event = new ProgressionEvent(
                id, player, ProgressionEventType.RECRUIT_HIRED,
                " GalacticWars:Clone_Trooper ", 1);
        assertEquals(id, event.id(), "event id accessor");
        assertEquals(player, event.playerId(), "event player accessor");
        assertEquals(ProgressionEventType.RECRUIT_HIRED, event.type(), "event type accessor");
        assertEquals("galacticwars:clone_trooper", event.subjectId(), "event normalization");
        assertEquals(1, event.amount(), "event amount accessor");
        assertEquals(event, new ProgressionEvent(
                id, player, ProgressionEventType.RECRUIT_HIRED,
                "galacticwars:clone_trooper", 1), "event value equality");
    }

    private static void progressionStateOwnsDeepImmutableCopies() {
        UUID player = UUID.randomUUID();
        UUID processedId = UUID.randomUUID();
        LinkedHashSet<UUID> processed = new LinkedHashSet<>(Set.of(processedId));
        LinkedHashMap<ProgressionEventType, Integer> totals = new LinkedHashMap<>();
        totals.put(ProgressionEventType.RECRUIT_HIRED, 1);
        LinkedHashSet<String> recruitSubjects = new LinkedHashSet<>(Set.of("clone_trooper"));
        LinkedHashMap<ProgressionEventType, Set<String>> subjects = new LinkedHashMap<>();
        subjects.put(ProgressionEventType.RECRUIT_HIRED, recruitSubjects);
        LinkedHashSet<String> unlocks = new LinkedHashSet<>(Set.of("recruitment"));

        ProgressionState state = new ProgressionState(
                ProgressionState.CURRENT_SCHEMA_VERSION, player, null, 5,
                processed, totals, subjects, unlocks);
        processed.clear();
        totals.put(ProgressionEventType.RECRUIT_HIRED, 99);
        recruitSubjects.add("invented_trooper");
        subjects.clear();
        unlocks.add("fabricated_unlock");

        assertEquals("", state.factionId(), "null faction normalization");
        assertTrue(state.processed(processedId), "processed event copy");
        assertEquals(1, state.total(ProgressionEventType.RECRUIT_HIRED), "event total copy");
        assertTrue(state.hasSubject(ProgressionEventType.RECRUIT_HIRED, "clone_trooper"),
                "nested event subject copy");
        assertTrue(!state.hasSubject(ProgressionEventType.RECRUIT_HIRED, "invented_trooper"),
                "nested subject mutation isolated");
        assertTrue(!state.unlocks().contains("fabricated_unlock"), "unlock mutation isolated");

        assertUnsupported(() -> state.processedEventIds().add(UUID.randomUUID()),
                "processed event view immutable");
        assertUnsupported(() -> state.eventTotals().put(ProgressionEventType.RECRUIT_HIRED, 2),
                "event total view immutable");
        assertUnsupported(() -> state.eventSubjects().get(ProgressionEventType.RECRUIT_HIRED).add("other"),
                "nested subject view immutable");
        assertUnsupported(() -> state.unlocks().add("other"), "unlock view immutable");
        assertEquals(0, state.clearPendingCreditRewards().pendingCreditRewards(),
                "pending rewards clear through immutable replacement");
    }

    private static void forceStateBoundsAndOwnsImmutableCopies() {
        LinkedHashMap<String, Long> cooldowns = new LinkedHashMap<>();
        cooldowns.put("light_push", 120L);
        List<UUID> activationOrder = new ArrayList<>();
        LinkedHashSet<UUID> processed = new LinkedHashSet<>();
        for (int index = 0; index < ForceRuntimeState.MAX_PROCESSED_ACTIVATIONS + 2; index++) {
            UUID id = UUID.randomUUID();
            activationOrder.add(id);
            processed.add(id);
        }

        ForceRuntimeState state = new ForceRuntimeState(null, 90, cooldowns, processed);
        cooldowns.put("dark_choke", 999L);
        processed.clear();

        assertEquals("", state.path(), "null Force path normalization");
        assertEquals(90, state.energy(), "Force energy accessor");
        assertEquals(120L, state.cooldownEnds().get("light_push"), "cooldown copy");
        assertTrue(!state.cooldownEnds().containsKey("dark_choke"), "cooldown mutation isolated");
        assertEquals(ForceRuntimeState.MAX_PROCESSED_ACTIVATIONS,
                state.processedActivationIds().size(), "activation replay bound");
        assertTrue(!state.processedActivationIds().contains(activationOrder.get(0))
                        && !state.processedActivationIds().contains(activationOrder.get(1)),
                "oldest activation ids evicted first");
        assertUnsupported(() -> state.cooldownEnds().put("other", 1L), "cooldown view immutable");
        assertUnsupported(() -> state.processedActivationIds().add(UUID.randomUUID()),
                "activation view immutable");
        assertEquals(ForceRuntimeState.MAX_ENERGY,
                state.regenerate(Integer.MAX_VALUE).energy(), "overflow-safe regeneration cap");
    }

    private static void progressionReplayLedgersRemainBoundedAndAggregate() {
        UUID player = UUID.randomUUID();
        LinkedHashSet<UUID> processed = new LinkedHashSet<>();
        UUID firstEvent = UUID.randomUUID();
        processed.add(firstEvent);
        UUID lastEvent = firstEvent;
        for (int index = 1; index < 8_194; index++) {
            lastEvent = UUID.randomUUID();
            processed.add(lastEvent);
        }
        LinkedHashSet<String> deliverySubjects = new LinkedHashSet<>();
        String firstDelivery = "courier/" + UUID.randomUUID();
        deliverySubjects.add(firstDelivery);
        String lastDelivery = firstDelivery;
        for (int index = 1; index < 4_098; index++) {
            lastDelivery = "courier/" + UUID.randomUUID();
            deliverySubjects.add(lastDelivery);
        }

        ProgressionState state = new ProgressionState(
                ProgressionState.CURRENT_SCHEMA_VERSION,
                player,
                "galacticwars:republic",
                0,
                processed,
                Map.of(ProgressionEventType.DELIVERY_COMPLETED, 7_000),
                Map.of(ProgressionEventType.DELIVERY_COMPLETED, deliverySubjects),
                Set.of("workforce"));

        assertEquals(ProgressionState.MAX_PROCESSED_EVENT_IDS,
                state.processedEventIds().size(), "processed-event replay window is bounded");
        assertTrue(!state.processed(firstEvent) && state.processed(lastEvent),
                "processed-event replay window retains the newest server actions");
        assertEquals(ProgressionState.MAX_RECENT_DELIVERY_SUBJECTS,
                state.eventSubjects().get(ProgressionEventType.DELIVERY_COMPLETED).size(),
                "delivery detail is reduced to bounded recent history");
        assertTrue(!state.hasSubject(ProgressionEventType.DELIVERY_COMPLETED, firstDelivery)
                        && state.hasSubject(ProgressionEventType.DELIVERY_COMPLETED, lastDelivery),
                "delivery detail retains the newest authoritative subjects");
        assertEquals(7_000, state.total(ProgressionEventType.DELIVERY_COMPLETED),
                "aggregate delivery total remains exact");
    }

    private static void decisionsRetainRecordStyleJavaApi() {
        ProgressionState state = ProgressionState.create(UUID.randomUUID());
        ProgressionDecision duplicate = ProgressionDecision.accepted(state, state);
        assertTrue(duplicate.accepted() && !duplicate.changed()
                        && duplicate.reason().equals("duplicate_event") && duplicate.state() == state,
                "progression decision static factory and accessors");
        ForceAbilityRuntimeService.ActivationDecision activation =
                new ForceAbilityRuntimeService.ActivationDecision(
                        true, "accepted", ForceRuntimeState.full(), 20, 60);
        assertTrue(activation.accepted() && activation.reason().equals("accepted")
                        && activation.energySpent() == 20 && activation.cooldownTicks() == 60,
                "nested Force decision constructor and accessors");
    }

    private static void assertUnsupported(Runnable action, String label) {
        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError(label);
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label);
        }
    }
}
