package galacticwars.clonewars.classes;

public final class ClassProgressionMilestonesTest {
    private ClassProgressionMilestonesTest() {
    }

    public static void main(String[] args) {
        assertEquals(1, ClassProgressionMilestones.requiredRankForAbilityIndex(0), "signature rank");
        assertEquals(3, ClassProgressionMilestones.requiredRankForAbilityIndex(1), "secondary rank");
        assertEquals(20, ClassProgressionMilestones.resourceCost(20, 4), "pre-rank-five cost");
        assertEquals(18, ClassProgressionMilestones.resourceCost(20, 5), "rank-five cost");
        assertEquals(17, ClassProgressionMilestones.cooldownTicks(20, 7), "rank-seven cooldown");
        assertEquals(15, ClassProgressionMilestones.cooldownTicks(20, 10), "master cooldown");
        assertEquals(1.25D, ClassProgressionMilestones.potency(10), "master potency");
        System.out.println("ClassProgressionMilestonesTest passed");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
