package galacticwars.clonewars.army;

public final class ArmySupplyPolicyTest {
    private ArmySupplyPolicyTest() {
    }

    public static void main(String[] args) {
        energyCellYieldUsesFloorRounding();
        energyCellYieldRemainsPositiveAndBounded();
        blasterAvailabilityRequiresOnePersistedUnit();
        System.out.println("ArmySupplyPolicyTest passed");
    }

    private static void energyCellYieldUsesFloorRounding() {
        assertEquals(16, ArmySupplyPolicy.unitsPerEnergyCell(100), "neutral supply yield");
        assertEquals(14, ArmySupplyPolicy.unitsPerEnergyCell(90), "reduced supply yield");
        assertEquals(20, ArmySupplyPolicy.unitsPerEnergyCell(125), "improved supply yield");
    }

    private static void energyCellYieldRemainsPositiveAndBounded() {
        assertEquals(1, ArmySupplyPolicy.unitsPerEnergyCell(0), "zero efficiency safety floor");
        assertEquals(1, ArmySupplyPolicy.unitsPerEnergyCell(-100), "negative efficiency clamp");
        assertEquals(80, ArmySupplyPolicy.unitsPerEnergyCell(500), "maximum supported efficiency");
        assertEquals(80, ArmySupplyPolicy.unitsPerEnergyCell(Integer.MAX_VALUE),
                "overflow-safe efficiency clamp");
    }

    private static void blasterAvailabilityRequiresOnePersistedUnit() {
        assertTrue(!ArmySupplyPolicy.canFireBlaster(-1), "negative supply rejected");
        assertTrue(!ArmySupplyPolicy.canFireBlaster(0), "empty supply rejected");
        assertTrue(ArmySupplyPolicy.canFireBlaster(1), "single shot supply accepted");
        assertTrue(ArmySupplyPolicy.canFireBlaster(Integer.MAX_VALUE), "large valid supply accepted");
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label + " expected to be true");
        }
    }
}
