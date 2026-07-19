package galacticwars.clonewars.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic, bounded search policy for relocating a naturally generated faction outpost.
 * Candidate coordinates are relative to the original outpost anchor and never imply a chunk load.
 */
public final class FactionOutpostSitePlan {
    public static final int GRID_STEP_BLOCKS = 4;
    public static final int SEARCH_RING_COUNT = 6;
    public static final int MAX_HORIZONTAL_DISTANCE = GRID_STEP_BLOCKS * SEARCH_RING_COUNT;
    public static final int MAX_VERTICAL_DISTANCE = 6;
    public static final int CANDIDATES_PER_ATTEMPT = 4;
    public static final long ATTEMPT_RETRY_TICKS = 20L;
    public static final long EXHAUSTED_RETRY_TICKS = 1_200L;

    private static final List<Offset> OFFSETS = createOffsets();

    private FactionOutpostSitePlan() {
    }

    public static int candidateCount() {
        return OFFSETS.size();
    }

    public static Offset candidate(int index) {
        if (index < 0 || index >= OFFSETS.size()) {
            throw new IndexOutOfBoundsException("outpost site candidate " + index);
        }
        return OFFSETS.get(index);
    }

    public static int verticalCandidateCount() {
        return MAX_VERTICAL_DISTANCE * 2 + 1;
    }

    /** Returns vertical offsets in the order 0, +1, -1, +2, -2, and so on. */
    public static int verticalOffset(int index) {
        if (index < 0 || index >= verticalCandidateCount()) {
            throw new IndexOutOfBoundsException("outpost site vertical candidate " + index);
        }
        if (index == 0) {
            return 0;
        }
        int magnitude = (index + 1) / 2;
        return (index & 1) == 1 ? magnitude : -magnitude;
    }

    public static AttemptWindow attemptWindow(int nextCandidateIndex) {
        if (nextCandidateIndex < 0 || nextCandidateIndex >= candidateCount()) {
            throw new IllegalArgumentException("invalid next outpost site candidate " + nextCandidateIndex);
        }
        int endExclusive = Math.min(candidateCount(), nextCandidateIndex + CANDIDATES_PER_ATTEMPT);
        return new AttemptWindow(
                nextCandidateIndex,
                endExclusive,
                endExclusive == candidateCount());
    }

    private static List<Offset> createOffsets() {
        ArrayList<Offset> offsets = new ArrayList<>((SEARCH_RING_COUNT * 2 + 1)
                * (SEARCH_RING_COUNT * 2 + 1));
        offsets.add(new Offset(0, 0));
        for (int ring = 1; ring <= SEARCH_RING_COUNT; ring++) {
            int minimum = -ring;
            int maximum = ring;
            for (int x = minimum; x <= maximum; x++) {
                offsets.add(scaled(x, minimum));
            }
            for (int z = minimum + 1; z <= maximum; z++) {
                offsets.add(scaled(maximum, z));
            }
            for (int x = maximum - 1; x >= minimum; x--) {
                offsets.add(scaled(x, maximum));
            }
            for (int z = maximum - 1; z > minimum; z--) {
                offsets.add(scaled(minimum, z));
            }
        }
        return List.copyOf(offsets);
    }

    private static Offset scaled(int gridX, int gridZ) {
        return new Offset(gridX * GRID_STEP_BLOCKS, gridZ * GRID_STEP_BLOCKS);
    }

    public record Offset(int x, int z) {
    }

    public record AttemptWindow(int startInclusive, int endExclusive, boolean completesSweep) {
        public AttemptWindow {
            if (startInclusive < 0 || endExclusive <= startInclusive
                    || endExclusive > candidateCount()) {
                throw new IllegalArgumentException("invalid outpost site attempt window");
            }
            if (completesSweep != (endExclusive == candidateCount())) {
                throw new IllegalArgumentException("outpost site sweep completion does not match window");
            }
        }

        public int size() {
            return endExclusive - startInclusive;
        }
    }
}
