package middleearth.lotr.warmod.world;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record MiddleEarthRegionId(String namespace, String path) {
    public static final String DEFAULT_NAMESPACE = "kingdomwarsmiddleearth";
    private static final Pattern VALID_PART = Pattern.compile("[a-z0-9_.-]+");

    public MiddleEarthRegionId {
        namespace = normalizePart(namespace, "namespace");
        path = normalizePart(path, "path");
    }

    public static MiddleEarthRegionId of(String value) {
        Objects.requireNonNull(value, "value");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Middle-earth region id cannot be empty");
        }

        int separator = trimmed.indexOf(':');
        if (separator >= 0) {
            return new MiddleEarthRegionId(trimmed.substring(0, separator), trimmed.substring(separator + 1));
        }

        return new MiddleEarthRegionId(DEFAULT_NAMESPACE, trimmed);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    private static String normalizePart(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (!VALID_PART.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid Middle-earth region id " + label + ": " + value);
        }
        return normalized;
    }
}
