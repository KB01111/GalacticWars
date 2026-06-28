package middleearth.lotr.warmod.faction;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record FactionAlignment(UUID playerId, Map<FactionId, Integer> scores) {
    public FactionAlignment {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(scores, "scores");
        scores = Collections.unmodifiableMap(new LinkedHashMap<>(scores));
    }

    public static FactionAlignment empty(UUID playerId) {
        return new FactionAlignment(playerId, Map.of());
    }

    public int score(FactionId factionId) {
        return scores.getOrDefault(Objects.requireNonNull(factionId, "factionId"), 0);
    }

    public FactionAlignment withAddedScore(FactionId factionId, int delta) {
        Objects.requireNonNull(factionId, "factionId");
        LinkedHashMap<FactionId, Integer> updatedScores = new LinkedHashMap<>(scores);
        updatedScores.merge(factionId, delta, Integer::sum);
        return new FactionAlignment(playerId, updatedScores);
    }
}
