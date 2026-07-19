package galacticwars.clonewars.conquest;

import java.util.UUID;

public final class ConquestCaptureServiceTest {
    private ConquestCaptureServiceTest() {
    }

    public static void main(String[] args) {
        sameKingdomCollaboratorsShareProgress();
        competingAttackersCannotInheritProgress();
        progressMathIsBoundedAndValidated();
        System.out.println("ConquestCaptureServiceTest passed");
    }

    private static void sameKingdomCollaboratorsShareProgress() {
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();
        String kingdomId = UUID.randomUUID().toString();
        String firstAuthority = ConquestCaptureService.captureAuthority(firstPlayer, kingdomId);
        String secondAuthority = ConquestCaptureService.captureAuthority(secondPlayer, kingdomId);
        ConquestControlState state = state().withProgress(firstAuthority, 80);

        ConquestControlState advanced = ConquestCaptureService.advanceProgress(
                state, secondAuthority, 20, 200);

        assertEquals(firstAuthority, secondAuthority, "shared kingdom authority");
        assertEquals(100, advanced.progress(), "collaborative kingdom progress");
    }

    private static void competingAttackersCannotInheritProgress() {
        String firstAuthority = ConquestCaptureService.captureAuthority(
                UUID.randomUUID(), UUID.randomUUID().toString());
        String secondAuthority = ConquestCaptureService.captureAuthority(
                UUID.randomUUID(), UUID.randomUUID().toString());
        ConquestControlState state = state().withProgress(firstAuthority, 180);

        ConquestControlState advanced = ConquestCaptureService.advanceProgress(
                state, secondAuthority, 20, 200);

        assertEquals(secondAuthority, advanced.capturingPlayer(), "new capture authority");
        assertEquals(20, advanced.progress(), "competing attacker reset");
    }

    private static void progressMathIsBoundedAndValidated() {
        String authority = ConquestCaptureService.captureAuthority(UUID.randomUUID(), "");
        ConquestControlState capped = ConquestCaptureService.advanceProgress(
                state().withProgress(authority, 190), authority, Integer.MAX_VALUE, 200);
        assertEquals(200, capped.progress(), "capture progress ceiling");
        assertThrows(() -> ConquestCaptureService.advanceProgress(state(), "", 20, 200));
        assertThrows(() -> ConquestCaptureService.advanceProgress(state(), authority, -1, 200));
        assertThrows(() -> ConquestCaptureService.advanceProgress(state(), authority, 1, 0));
    }

    private static ConquestControlState state() {
        return new ConquestControlState(
                "test", "minecraft:overworld", 0, 64, 0,
                "galacticwars:separatist", "", "", 0, 0L);
    }

    private static void assertThrows(Runnable action) {
        try {
            action.run();
            throw new AssertionError("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
