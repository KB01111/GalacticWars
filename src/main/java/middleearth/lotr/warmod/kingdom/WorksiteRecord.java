package middleearth.lotr.warmod.kingdom;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public record WorksiteRecord(
        UUID id,
        String type,
        String dimensionId,
        int x,
        int y,
        int z,
        int radius,
        int capacity
) {
    public WorksiteRecord {
        Objects.requireNonNull(id, "id");
        type = normalize(type, "type");
        dimensionId = normalize(dimensionId, "dimensionId");
        if (radius < 1 || radius > 32) {
            throw new IllegalArgumentException("radius must be between 1 and 32");
        }
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be positive");
        }
    }

    private static String normalize(String value, String label) {
        Objects.requireNonNull(value, label);
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return normalized;
    }
}
