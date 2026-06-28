package middleearth.lotr.warmod.army;

import java.util.Objects;
import java.util.UUID;

import middleearth.lotr.warmod.faction.FactionId;

public record ArmyTargetCandidate(
        UUID entityId,
        FactionId factionId,
        ArmyPosition position,
        boolean attackingOwner,
        boolean attackingRecruit,
        int threat
) {
    public ArmyTargetCandidate {
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(factionId, "factionId");
        Objects.requireNonNull(position, "position");
        if (threat < 0 || threat > 100) {
            throw new IllegalArgumentException("threat must be between 0 and 100");
        }
    }
}
