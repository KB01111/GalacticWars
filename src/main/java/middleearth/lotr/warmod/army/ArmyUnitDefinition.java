package middleearth.lotr.warmod.army;

import java.util.Objects;

import middleearth.lotr.warmod.faction.FactionId;

public record ArmyUnitDefinition(
        ArmyUnitId id,
        String displayName,
        FactionId factionId,
        ArmyUnitRole role,
        int hireCost,
        int maxHealth,
        int attackDamage,
        ArmyFormation defaultFormation
) {
    public ArmyUnitDefinition {
        Objects.requireNonNull(id, "id");
        displayName = requireNonBlank(displayName, "displayName");
        Objects.requireNonNull(factionId, "factionId");
        Objects.requireNonNull(role, "role");
        requireNonNegative(hireCost, "hireCost");
        requireNonNegative(maxHealth, "maxHealth");
        requireNonNegative(attackDamage, "attackDamage");
        Objects.requireNonNull(defaultFormation, "defaultFormation");
    }

    private static void requireNonNegative(int value, String label) {
        if (value < 0) {
            throw new IllegalArgumentException(label + " cannot be negative");
        }
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
