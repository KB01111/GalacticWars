package galacticwars.clonewars.data;

import java.util.Objects;

/** Shared validation for SavedData schemas that support migration from older versions. */
public final class SavedDataSchemaPolicy {
    private SavedDataSchemaPolicy() {
    }

    public static int migrate(int persistedVersion, int currentVersion, String domain) {
        Objects.requireNonNull(domain, "domain");
        if (persistedVersion < 0) {
            throw new IllegalArgumentException("Invalid " + domain + " schema " + persistedVersion);
        }
        if (persistedVersion > currentVersion) {
            throw new IllegalArgumentException("Unsupported " + domain + " schema " + persistedVersion);
        }
        return currentVersion;
    }
}
