package galacticwars.clonewars.world;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Persisted lease and cursor ensuring only one resident searches for an outpost site at a time. */
public record FactionOutpostSiteProgress(
        UUID outpostId,
        int nextCandidateIndex,
        long nextAttemptGameTime
) {
    public FactionOutpostSiteProgress {
        Objects.requireNonNull(outpostId, "outpostId");
        if (nextCandidateIndex < 0 || nextCandidateIndex >= FactionOutpostSitePlan.candidateCount()) {
            throw new IllegalArgumentException("invalid next outpost site candidate " + nextCandidateIndex);
        }
        if (nextAttemptGameTime < 0L) {
            throw new IllegalArgumentException("next outpost site attempt cannot be negative");
        }
    }

    public static FactionOutpostSiteProgress initial(UUID outpostId) {
        return new FactionOutpostSiteProgress(outpostId, 0, 0L);
    }

    public Optional<Claim> claim(long gameTime) {
        if (gameTime < 0L) {
            throw new IllegalArgumentException("game time cannot be negative");
        }
        if (gameTime < nextAttemptGameTime) {
            return Optional.empty();
        }
        FactionOutpostSitePlan.AttemptWindow window =
                FactionOutpostSitePlan.attemptWindow(nextCandidateIndex);
        long delay = window.completesSweep()
                ? FactionOutpostSitePlan.EXHAUSTED_RETRY_TICKS
                : FactionOutpostSitePlan.ATTEMPT_RETRY_TICKS;
        int followingCandidate = window.completesSweep() ? 0 : window.endExclusive();
        FactionOutpostSiteProgress following = new FactionOutpostSiteProgress(
                outpostId,
                followingCandidate,
                saturatedAdd(gameTime, delay));
        return Optional.of(new Claim(window, following));
    }

    private static long saturatedAdd(long value, long increment) {
        return value > Long.MAX_VALUE - increment ? Long.MAX_VALUE : value + increment;
    }

    public record Claim(
            FactionOutpostSitePlan.AttemptWindow window,
            FactionOutpostSiteProgress followingProgress
    ) {
        public Claim {
            Objects.requireNonNull(window, "window");
            Objects.requireNonNull(followingProgress, "followingProgress");
        }
    }
}
