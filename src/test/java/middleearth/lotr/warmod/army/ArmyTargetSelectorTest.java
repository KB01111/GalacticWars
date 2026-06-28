package middleearth.lotr.warmod.army;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import middleearth.lotr.warmod.faction.FactionCatalog;
import middleearth.lotr.warmod.faction.FactionDefinition;
import middleearth.lotr.warmod.faction.FactionId;

public final class ArmyTargetSelectorTest {
    private static final FactionId GONDOR = FactionId.of("gondor");
    private static final FactionId ROHAN = FactionId.of("rohan");
    private static final FactionId MORDOR = FactionId.of("mordor");
    private static final FactionId UNKNOWN = FactionId.of("unknown");
    private static final ArmyPosition ORIGIN = new ArmyPosition(0, 64, 0);

    private ArmyTargetSelectorTest() {
    }

    public static void main(String[] args) {
        ignoresNonEnemiesAndOutOfRangeEnemies();
        prioritizesOwnerAttackerOverCloserEnemy();
        prioritizesRecruitAttackerOverHigherThreatEnemy();
        usesThreatBeforeDistanceForOrdinaryEnemies();
        usesDistanceAndUuidTieBreakers();
        rejectsInvalidInputs();

        System.out.println("ArmyTargetSelectorTest passed");
    }

    private static void ignoresNonEnemiesAndOutOfRangeEnemies() {
        Optional<ArmyTargetSelection> selection = ArmyTargetSelector.selectTarget(
                GONDOR,
                ORIGIN,
                List.of(
                        candidate("00000000-0000-0000-0000-000000000201", GONDOR, new ArmyPosition(1, 64, 0), false, false, 100),
                        candidate("00000000-0000-0000-0000-000000000202", ROHAN, new ArmyPosition(2, 64, 0), false, false, 100),
                        candidate("00000000-0000-0000-0000-000000000203", UNKNOWN, new ArmyPosition(3, 64, 0), false, false, 100),
                        candidate("00000000-0000-0000-0000-000000000204", MORDOR, new ArmyPosition(100, 64, 0), false, false, 100)),
                catalog(),
                16);

        assertFalse(selection.isPresent(), "non-enemies and out-of-range enemies ignored");
    }

    private static void prioritizesOwnerAttackerOverCloserEnemy() {
        UUID ownerAttacker = UUID.fromString("00000000-0000-0000-0000-000000000212");

        ArmyTargetSelection selection = ArmyTargetSelector.selectTarget(
                GONDOR,
                ORIGIN,
                List.of(
                        candidate("00000000-0000-0000-0000-000000000211", MORDOR, new ArmyPosition(1, 64, 0), false, false, 100),
                        new ArmyTargetCandidate(ownerAttacker, MORDOR, new ArmyPosition(10, 64, 0), true, false, 0)),
                catalog(),
                16).orElseThrow();

        assertEquals(ownerAttacker, selection.targetId(), "owner attacker target");
        assertEquals("protect_owner", selection.reasonCode(), "owner attacker reason");
    }

    private static void prioritizesRecruitAttackerOverHigherThreatEnemy() {
        UUID recruitAttacker = UUID.fromString("00000000-0000-0000-0000-000000000222");

        ArmyTargetSelection selection = ArmyTargetSelector.selectTarget(
                GONDOR,
                ORIGIN,
                List.of(
                        candidate("00000000-0000-0000-0000-000000000221", MORDOR, new ArmyPosition(1, 64, 0), false, false, 100),
                        new ArmyTargetCandidate(recruitAttacker, MORDOR, new ArmyPosition(12, 64, 0), false, true, 0)),
                catalog(),
                16).orElseThrow();

        assertEquals(recruitAttacker, selection.targetId(), "recruit attacker target");
        assertEquals("self_defense", selection.reasonCode(), "recruit attacker reason");
    }

    private static void usesThreatBeforeDistanceForOrdinaryEnemies() {
        UUID highThreat = UUID.fromString("00000000-0000-0000-0000-000000000232");

        ArmyTargetSelection selection = ArmyTargetSelector.selectTarget(
                GONDOR,
                ORIGIN,
                List.of(
                        candidate("00000000-0000-0000-0000-000000000231", MORDOR, new ArmyPosition(1, 64, 0), false, false, 10),
                        new ArmyTargetCandidate(highThreat, MORDOR, new ArmyPosition(12, 64, 0), false, false, 100)),
                catalog(),
                16).orElseThrow();

        assertEquals(highThreat, selection.targetId(), "high threat target");
        assertEquals("hostile_threat", selection.reasonCode(), "ordinary hostile reason");
    }

    private static void usesDistanceAndUuidTieBreakers() {
        UUID closer = UUID.fromString("00000000-0000-0000-0000-000000000242");
        UUID lowerUuid = UUID.fromString("00000000-0000-0000-0000-000000000243");

        ArmyTargetSelection distanceSelection = ArmyTargetSelector.selectTarget(
                GONDOR,
                ORIGIN,
                List.of(
                        candidate("00000000-0000-0000-0000-000000000241", MORDOR, new ArmyPosition(8, 64, 0), false, false, 50),
                        new ArmyTargetCandidate(closer, MORDOR, new ArmyPosition(2, 64, 0), false, false, 50)),
                catalog(),
                16).orElseThrow();

        ArmyTargetSelection uuidSelection = ArmyTargetSelector.selectTarget(
                GONDOR,
                ORIGIN,
                List.of(
                        candidate("00000000-0000-0000-0000-000000000244", MORDOR, new ArmyPosition(4, 64, 0), false, false, 50),
                        new ArmyTargetCandidate(lowerUuid, MORDOR, new ArmyPosition(4, 64, 0), false, false, 50)),
                catalog(),
                16).orElseThrow();

        assertEquals(closer, distanceSelection.targetId(), "closer tie target");
        assertEquals(lowerUuid, uuidSelection.targetId(), "uuid tie target");
    }

    private static void rejectsInvalidInputs() {
        assertThrows(NullPointerException.class,
                () -> new ArmyTargetCandidate(null, MORDOR, ORIGIN, false, false, 0),
                "null candidate id");
        assertThrows(IllegalArgumentException.class,
                () -> new ArmyTargetCandidate(UUID.randomUUID(), MORDOR, ORIGIN, false, false, -1),
                "negative threat");
        assertThrows(IllegalArgumentException.class,
                () -> new ArmyTargetCandidate(UUID.randomUUID(), MORDOR, ORIGIN, false, false, 101),
                "threat above range");
        assertThrows(IllegalArgumentException.class,
                () -> new ArmyTargetSelection(UUID.randomUUID(), ORIGIN, " ", 1),
                "blank reason");
        assertThrows(NullPointerException.class,
                () -> ArmyTargetSelector.selectTarget(null, ORIGIN, List.of(), catalog(), 16),
                "null own faction");
        assertThrows(IllegalArgumentException.class,
                () -> ArmyTargetSelector.selectTarget(GONDOR, ORIGIN, List.of(), catalog(), -1),
                "negative max range");
    }

    private static ArmyTargetCandidate candidate(
            String entityId,
            FactionId factionId,
            ArmyPosition position,
            boolean attackingOwner,
            boolean attackingRecruit,
            int threat
    ) {
        return new ArmyTargetCandidate(UUID.fromString(entityId), factionId, position, attackingOwner, attackingRecruit, threat);
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
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertFalse(boolean condition, String label) {
        if (condition) {
            throw new AssertionError(label + " expected to be false");
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
