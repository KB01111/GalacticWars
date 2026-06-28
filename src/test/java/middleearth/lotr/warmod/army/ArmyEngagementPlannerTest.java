package middleearth.lotr.warmod.army;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import middleearth.lotr.warmod.faction.FactionCatalog;
import middleearth.lotr.warmod.faction.FactionDefinition;
import middleearth.lotr.warmod.faction.FactionId;

public final class ArmyEngagementPlannerTest {
    private static final FactionId GONDOR = FactionId.of("gondor");
    private static final FactionId ROHAN = FactionId.of("rohan");
    private static final FactionId MORDOR = FactionId.of("mordor");
    private static final ArmyPosition ORIGIN = new ArmyPosition(0, 64, 0);
    private static final UUID OWNER_ATTACKER_ID = UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final UUID ORDINARY_ENEMY_ID = UUID.fromString("00000000-0000-0000-0000-000000000502");

    private ArmyEngagementPlannerTest() {
    }

    public static void main(String[] args) {
        passiveStanceIdlesEvenWhenThreatened();
        defensiveStanceEngagesActiveThreats();
        defensiveStanceIdlesWithoutActiveThreat();
        aggressiveStanceEngagesOrdinaryEnemy();
        rejectsInvalidInputs();

        System.out.println("ArmyEngagementPlannerTest passed");
    }

    private static void passiveStanceIdlesEvenWhenThreatened() {
        ArmyEngagementDecision decision = ArmyEngagementPlanner.plan(
                ArmyEngagementStance.PASSIVE,
                GONDOR,
                ORIGIN,
                List.of(ownerAttacker()),
                catalog(),
                16);

        assertFalse(decision.engaging(), "passive engaging");
        assertEquals(ArmyBehaviorIntent.IDLE, decision.behaviorDecision().intent(), "passive intent");
        assertEquals(null, decision.targetSelection(), "passive target");
        assertEquals("passive_stance", decision.reasonCode(), "passive reason");
    }

    private static void defensiveStanceEngagesActiveThreats() {
        ArmyEngagementDecision decision = ArmyEngagementPlanner.plan(
                ArmyEngagementStance.DEFENSIVE,
                GONDOR,
                ORIGIN,
                List.of(ordinaryEnemy(), ownerAttacker()),
                catalog(),
                16);

        assertTrue(decision.engaging(), "defensive engaging");
        assertEquals(ArmyBehaviorIntent.ATTACK_TARGET, decision.behaviorDecision().intent(), "defensive attack intent");
        assertEquals(OWNER_ATTACKER_ID, decision.behaviorDecision().attackTargetId(), "defensive target id");
        assertEquals("protect_owner", decision.behaviorDecision().reasonCode(), "defensive behavior reason");
        assertEquals("engaging", decision.reasonCode(), "defensive decision reason");
    }

    private static void defensiveStanceIdlesWithoutActiveThreat() {
        ArmyEngagementDecision decision = ArmyEngagementPlanner.plan(
                ArmyEngagementStance.DEFENSIVE,
                GONDOR,
                ORIGIN,
                List.of(ordinaryEnemy()),
                catalog(),
                16);

        assertFalse(decision.engaging(), "defensive idle engaging");
        assertEquals(ArmyBehaviorIntent.IDLE, decision.behaviorDecision().intent(), "defensive idle intent");
        assertEquals("no_defensive_threat", decision.reasonCode(), "defensive idle reason");
    }

    private static void aggressiveStanceEngagesOrdinaryEnemy() {
        ArmyEngagementDecision decision = ArmyEngagementPlanner.plan(
                ArmyEngagementStance.AGGRESSIVE,
                GONDOR,
                ORIGIN,
                List.of(ordinaryEnemy()),
                catalog(),
                16);

        assertTrue(decision.engaging(), "aggressive engaging");
        assertEquals(ORDINARY_ENEMY_ID, decision.behaviorDecision().attackTargetId(), "aggressive target id");
        assertEquals("hostile_threat", decision.behaviorDecision().reasonCode(), "aggressive behavior reason");
    }

    private static void rejectsInvalidInputs() {
        ArmyTargetSelection selection = new ArmyTargetSelection(ORDINARY_ENEMY_ID, ORIGIN, "hostile_threat", 100);

        assertThrows(NullPointerException.class,
                () -> ArmyEngagementPlanner.plan(null, GONDOR, ORIGIN, List.of(), catalog(), 16),
                "null stance");
        assertThrows(IllegalArgumentException.class,
                () -> ArmyEngagementPlanner.plan(ArmyEngagementStance.AGGRESSIVE, GONDOR, ORIGIN, List.of(), catalog(), -1),
                "negative range");
        assertThrows(NullPointerException.class,
                () -> ArmyEngagementDecision.engage(null),
                "null engagement selection");
        assertThrows(NullPointerException.class,
                () -> new ArmyEngagementDecision(true, ArmyBehaviorDecision.attack(ORDINARY_ENEMY_ID, "hostile_threat"), null, "engaging"),
                "engaging without target");
        assertThrows(IllegalArgumentException.class,
                () -> new ArmyEngagementDecision(false, ArmyBehaviorDecision.idle("idle"), selection, "idle"),
                "idle with target");
        assertThrows(IllegalArgumentException.class,
                () -> new ArmyEngagementDecision(false, ArmyBehaviorDecision.idle("idle"), null, " "),
                "blank reason");
    }

    private static ArmyTargetCandidate ownerAttacker() {
        return new ArmyTargetCandidate(OWNER_ATTACKER_ID, MORDOR, new ArmyPosition(10, 64, 0), true, false, 20);
    }

    private static ArmyTargetCandidate ordinaryEnemy() {
        return new ArmyTargetCandidate(ORDINARY_ENEMY_ID, MORDOR, new ArmyPosition(1, 64, 0), false, false, 100);
    }

    private static FactionCatalog catalog() {
        FactionDefinition gondor = new FactionDefinition(
                GONDOR,
                "Gondor",
                25,
                10,
                12,
                Set.of(ROHAN),
                Set.of(MORDOR));
        FactionDefinition rohan = new FactionDefinition(
                ROHAN,
                "Rohan",
                20,
                8,
                10,
                Set.of(GONDOR),
                Set.of(MORDOR));
        FactionDefinition mordor = new FactionDefinition(
                MORDOR,
                "Mordor",
                30,
                15,
                16,
                Set.of(),
                Set.of(GONDOR, ROHAN));

        return new FactionCatalog(Map.of(
                GONDOR, gondor,
                ROHAN, rohan,
                MORDOR, mordor));
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertFalse(boolean condition, String label) {
        if (condition) {
            throw new AssertionError(label + " expected to be false");
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label + " expected to be true");
        }
    }

    private static <T extends Throwable> void assertThrows(Class<T> expectedType, ThrowingRunnable runnable, String label) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                return;
            }
            throw new AssertionError(label + " threw " + throwable.getClass().getName() + " instead of "
                    + expectedType.getName(), throwable);
        }

        throw new AssertionError(label + " did not throw " + expectedType.getName());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
