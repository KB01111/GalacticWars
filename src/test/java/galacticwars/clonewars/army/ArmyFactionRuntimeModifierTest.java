package galacticwars.clonewars.army;

public final class ArmyFactionRuntimeModifierTest {
    private ArmyFactionRuntimeModifierTest() {
    }

    public static void main(String[] args) {
        coordinatedCadenceUsesExactInversePercentage();
        commandNodePenaltyCombinesBeforeCadence();
        absentCommanderPreservesBaseCadence();
        mobilityScalingIsPositiveAndBounded();
        virtualSimulationRetainsItsIntentionalSpeedFloor();
        System.out.println("ArmyFactionRuntimeModifierTest passed");
    }

    private static void coordinatedCadenceUsesExactInversePercentage() {
        assertEquals(18, ArmyRecruitRuntimeController.coordinatedAttackCooldownTicks(20, 115, true),
                "115 percent melee cadence");
        assertEquals(21, ArmyRecruitRuntimeController.coordinatedAttackCooldownTicks(24, 115, true),
                "115 percent bow cadence");
        assertEquals(25, ArmyRecruitRuntimeController.coordinatedAttackCooldownTicks(20, 80, true),
                "80 percent melee cadence");
        assertEquals(Integer.MAX_VALUE, ArmyRecruitRuntimeController.coordinatedAttackCooldownTicks(
                Integer.MAX_VALUE, 1, true), "overflow-safe cadence ceiling");
        assertThrows(() -> ArmyRecruitRuntimeController.coordinatedAttackCooldownTicks(0, 100, true),
                "non-positive base cooldown");
    }

    private static void commandNodePenaltyCombinesBeforeCadence() {
        assertEquals(115, ArmyRecruitRuntimeController.effectiveCoordinationPercent(115, 80, true),
                "active command node ignores absence penalty");
        int penalized = ArmyRecruitRuntimeController.effectiveCoordinationPercent(115, 80, false);
        assertEquals(92, penalized, "missing command node effective coordination");
        assertEquals(22, ArmyRecruitRuntimeController.coordinatedAttackCooldownTicks(20, penalized, true),
                "missing command node cadence");
        assertEquals(1, ArmyRecruitRuntimeController.effectiveCoordinationPercent(100, 0, false),
                "zero percent command-node policy remains positive");
    }

    private static void absentCommanderPreservesBaseCadence() {
        assertEquals(20, ArmyRecruitRuntimeController.coordinatedAttackCooldownTicks(20, 500, false),
                "melee cadence without commander");
        assertEquals(24, ArmyRecruitRuntimeController.coordinatedAttackCooldownTicks(24, 1, false),
                "bow cadence without commander");
    }

    private static void mobilityScalingIsPositiveAndBounded() {
        assertDouble(0.336D, ArmyRuntimeEvents.effectiveMovementSpeed(0.28D, 120),
                "Mandalorian mobility");
        assertDouble(0.01D, ArmyRuntimeEvents.effectiveMovementSpeed(0.28D, 0),
                "zero mobility safety floor");
        assertDouble(4.0D, ArmyRuntimeEvents.effectiveMovementSpeed(1.0D, 700),
                "mobility ceiling");
        assertThrows(() -> ArmyRuntimeEvents.effectiveMovementSpeed(Double.NaN, 100),
                "non-finite movement speed");
        assertThrows(() -> ArmyRuntimeEvents.effectiveMovementSpeed(-0.1D, 100),
                "negative movement speed");
    }

    private static void virtualSimulationRetainsItsIntentionalSpeedFloor() {
        double degradedSpeed = ArmyRuntimeEvents.effectiveMovementSpeed(0.28D, 0);
        assertDouble(1.0D, VirtualArmyMovementPlanner.blocksPerSecond(degradedSpeed),
                "virtual armies continue making bounded progress at the simulation floor");
    }

    private static void assertThrows(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " expected an exception");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void assertDouble(double expected, double actual, String label) {
        if (Math.abs(expected - actual) > 0.000001D) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }
}
