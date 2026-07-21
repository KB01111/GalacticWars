package galacticwars.clonewars.data;

import java.util.Objects;

/**
 * Describes whether the server has a complete, validated gameplay-content snapshot.
 * A rejected hot reload remains usable when a prior generation exists.
 */
public record GameplayContentState(
        Status status,
        long generation,
        String contentHash,
        String diagnostic
) {
    private static final int MAX_DIAGNOSTIC_LENGTH = 2_048;

    public GameplayContentState {
        Objects.requireNonNull(status, "status");
        if (generation < 0) {
            throw new IllegalArgumentException("generation cannot be negative");
        }
        contentHash = contentHash == null ? "" : contentHash;
        diagnostic = diagnostic == null ? "" : diagnostic;
        if (diagnostic.length() > MAX_DIAGNOSTIC_LENGTH) {
            diagnostic = diagnostic.substring(0, MAX_DIAGNOSTIC_LENGTH);
        }
        if (generation == 0 && !contentHash.isEmpty()) {
            throw new IllegalArgumentException("uninitialized content cannot have a hash");
        }
        if (generation > 0 && contentHash.isBlank()) {
            throw new IllegalArgumentException("usable content requires a hash");
        }
    }

    public static GameplayContentState uninitialized() {
        return new GameplayContentState(Status.UNINITIALIZED, 0, "", "");
    }

    public static GameplayContentState ready(long generation, String contentHash) {
        return new GameplayContentState(Status.READY, generation, contentHash, "");
    }

    public static GameplayContentState initialFailure(String diagnostic) {
        return new GameplayContentState(Status.FAILED_INITIAL_LOAD, 0, "", diagnostic);
    }

    public static GameplayContentState rejectedReload(
            long generation,
            String contentHash,
            String diagnostic
    ) {
        return new GameplayContentState(Status.RELOAD_REJECTED, generation, contentHash, diagnostic);
    }

    public boolean hasUsableSnapshot() {
        return generation > 0;
    }

    public enum Status {
        UNINITIALIZED,
        READY,
        RELOAD_REJECTED,
        FAILED_INITIAL_LOAD
    }
}
