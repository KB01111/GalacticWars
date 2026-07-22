package galacticwars.clonewars.conquest;

public final class ConquestCounterattackStateTest {
    private ConquestCounterattackStateTest() {
    }

    public static void main(String[] args) {
        ConquestControlState captured = new ConquestControlState(
                "region", "galacticwars:tatooine", 8, 64, 8,
                "galacticwars:republic", "kingdom", "", 0, 4L)
                .captured("galacticwars:mandalorian", "kingdom", 24000L,
                        "galacticwars:hutt_cartel");
        assertEquals(24000L, captured.counterattackAt(), "scheduled attack");
        ConquestControlState active = captured.startCounterattack(26400L).advanceDefense(80);
        if (!active.counterattackActive()) throw new AssertionError("counterattack did not activate");
        assertEquals(80, active.counterattackProgress(), "defense progress");
        ConquestControlState defended = active.defended(50400L);
        assertEquals("galacticwars:mandalorian", defended.controllingFaction(), "defended ownership");
        assertEquals(0, defended.counterattackProgress(), "defense reset");
        ConquestControlState lost = active.counterattackLost();
        assertEquals("galacticwars:hutt_cartel", lost.controllingFaction(), "lost ownership");
        assertEquals("", lost.controllingKingdom(), "lost kingdom authority");
        System.out.println("ConquestCounterattackStateTest passed");
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
