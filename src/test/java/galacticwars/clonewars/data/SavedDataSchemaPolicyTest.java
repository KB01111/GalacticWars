package galacticwars.clonewars.data;

public final class SavedDataSchemaPolicyTest {
    private SavedDataSchemaPolicyTest() {
    }

    public static void main(String[] args) {
        assertEquals(6, SavedDataSchemaPolicy.migrate(5, 6, "kingdom"),
                "older kingdom schema migrates");
        assertEquals(6, SavedDataSchemaPolicy.migrate(6, 6, "kingdom"),
                "current kingdom schema remains current");
        assertThrows(() -> SavedDataSchemaPolicy.migrate(7, 6, "kingdom"),
                "future kingdom schema is rejected");
        assertThrows(() -> SavedDataSchemaPolicy.migrate(-1, 2, "progression"),
                "invalid progression schema is rejected");
        System.out.println("SavedDataSchemaPolicyTest passed");
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
        }
    }

    private static void assertThrows(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
