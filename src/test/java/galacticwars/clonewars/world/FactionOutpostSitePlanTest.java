package galacticwars.clonewars.world;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class FactionOutpostSitePlanTest {
    private FactionOutpostSitePlanTest() {
    }

    public static void main(String[] args) {
        candidatesCoverTheBoundedSearchGridExactlyOnce();
        verticalSearchIsNearestFirstAndBounded();
        attemptWindowsBoundPerTickWork();
        persistedProgressExcludesDuplicateResidentClaims();
        exhaustedSweepBacksOffAndRestartsDeterministically();
        System.out.println("FactionOutpostSitePlanTest passed");
    }

    private static void candidatesCoverTheBoundedSearchGridExactlyOnce() {
        int width = FactionOutpostSitePlan.SEARCH_RING_COUNT * 2 + 1;
        assertEquals(width * width, FactionOutpostSitePlan.candidateCount(), "candidate count");
        Set<FactionOutpostSitePlan.Offset> unique = new LinkedHashSet<>();
        for (int index = 0; index < FactionOutpostSitePlan.candidateCount(); index++) {
            FactionOutpostSitePlan.Offset offset = FactionOutpostSitePlan.candidate(index);
            assertTrue(unique.add(offset), "duplicate candidate " + offset);
            assertTrue(Math.abs(offset.x()) <= FactionOutpostSitePlan.MAX_HORIZONTAL_DISTANCE,
                    "candidate x bound");
            assertTrue(Math.abs(offset.z()) <= FactionOutpostSitePlan.MAX_HORIZONTAL_DISTANCE,
                    "candidate z bound");
            assertEquals(0, Math.floorMod(offset.x(), FactionOutpostSitePlan.GRID_STEP_BLOCKS),
                    "candidate x grid");
            assertEquals(0, Math.floorMod(offset.z(), FactionOutpostSitePlan.GRID_STEP_BLOCKS),
                    "candidate z grid");
        }
        assertTrue(unique.contains(new FactionOutpostSitePlan.Offset(0, 0)), "original anchor");
        assertTrue(unique.contains(new FactionOutpostSitePlan.Offset(
                FactionOutpostSitePlan.MAX_HORIZONTAL_DISTANCE,
                FactionOutpostSitePlan.MAX_HORIZONTAL_DISTANCE)), "positive search corner");
        assertTrue(unique.contains(new FactionOutpostSitePlan.Offset(
                -FactionOutpostSitePlan.MAX_HORIZONTAL_DISTANCE,
                -FactionOutpostSitePlan.MAX_HORIZONTAL_DISTANCE)), "negative search corner");
    }

    private static void verticalSearchIsNearestFirstAndBounded() {
        assertEquals(FactionOutpostSitePlan.MAX_VERTICAL_DISTANCE * 2 + 1,
                FactionOutpostSitePlan.verticalCandidateCount(), "vertical candidate count");
        int[] expectedPrefix = {0, 1, -1, 2, -2, 3, -3};
        for (int index = 0; index < expectedPrefix.length; index++) {
            assertEquals(expectedPrefix[index], FactionOutpostSitePlan.verticalOffset(index),
                    "vertical ordering " + index);
        }
        for (int index = 0; index < FactionOutpostSitePlan.verticalCandidateCount(); index++) {
            assertTrue(Math.abs(FactionOutpostSitePlan.verticalOffset(index))
                            <= FactionOutpostSitePlan.MAX_VERTICAL_DISTANCE,
                    "vertical bound");
        }
    }

    private static void attemptWindowsBoundPerTickWork() {
        int cursor = 0;
        int covered = 0;
        while (true) {
            FactionOutpostSitePlan.AttemptWindow window =
                    FactionOutpostSitePlan.attemptWindow(cursor);
            assertTrue(window.size() <= FactionOutpostSitePlan.CANDIDATES_PER_ATTEMPT,
                    "bounded attempt size");
            assertEquals(cursor, window.startInclusive(), "contiguous attempt start");
            covered += window.size();
            if (window.completesSweep()) {
                break;
            }
            cursor = window.endExclusive();
        }
        assertEquals(FactionOutpostSitePlan.candidateCount(), covered, "complete candidate sweep");
    }

    private static void persistedProgressExcludesDuplicateResidentClaims() {
        UUID outpostId = UUID.randomUUID();
        FactionOutpostSiteProgress initial = FactionOutpostSiteProgress.initial(outpostId);
        FactionOutpostSiteProgress.Claim first = initial.claim(100L).orElseThrow();
        assertTrue(first.followingProgress().claim(100L).isEmpty(), "same-tick duplicate claim");
        assertTrue(first.followingProgress().claim(
                100L + FactionOutpostSitePlan.ATTEMPT_RETRY_TICKS - 1L).isEmpty(),
                "early duplicate claim");
        FactionOutpostSiteProgress.Claim next = first.followingProgress().claim(
                100L + FactionOutpostSitePlan.ATTEMPT_RETRY_TICKS).orElseThrow();
        assertEquals(first.window().endExclusive(), next.window().startInclusive(),
                "persisted cursor advancement");
        assertTrue(next.followingProgress().outpostId().equals(outpostId), "outpost identity");
    }

    private static void exhaustedSweepBacksOffAndRestartsDeterministically() {
        long gameTime = 1_000L;
        FactionOutpostSiteProgress progress = FactionOutpostSiteProgress.initial(UUID.randomUUID());
        FactionOutpostSiteProgress.Claim claim;
        do {
            claim = progress.claim(gameTime).orElseThrow();
            progress = claim.followingProgress();
            gameTime = progress.nextAttemptGameTime();
        } while (!claim.window().completesSweep());

        assertEquals(0, progress.nextCandidateIndex(), "sweep cursor reset");
        long restartTime = progress.nextAttemptGameTime();
        assertTrue(progress.claim(restartTime - 1L).isEmpty(), "exhausted sweep backoff");
        FactionOutpostSiteProgress.Claim restarted = progress.claim(restartTime).orElseThrow();
        assertEquals(0, restarted.window().startInclusive(), "deterministic sweep restart");
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label);
        }
    }

    private static void assertEquals(long expected, long actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
