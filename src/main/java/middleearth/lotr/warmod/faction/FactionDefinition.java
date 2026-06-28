package middleearth.lotr.warmod.faction;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public record FactionDefinition(
        FactionId id,
        String displayName,
        int hireCost,
        int minimumHiringAlignment,
        int maxOwnedRecruits,
        Set<FactionId> allies,
        Set<FactionId> enemies
) {
    public FactionDefinition {
        Objects.requireNonNull(id, "id");
        displayName = requireNonBlank(displayName, "displayName");
        if (hireCost < 0) {
            throw new IllegalArgumentException("hireCost cannot be negative");
        }
        if (maxOwnedRecruits < 0) {
            throw new IllegalArgumentException("maxOwnedRecruits cannot be negative");
        }
        allies = immutableCopy(allies, "allies");
        enemies = immutableCopy(enemies, "enemies");
    }

    private static Set<FactionId> immutableCopy(Set<FactionId> values, String label) {
        Objects.requireNonNull(values, label);
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    private static String requireNonBlank(String value, String label) {
        Objects.requireNonNull(value, label);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return trimmed;
    }
}
