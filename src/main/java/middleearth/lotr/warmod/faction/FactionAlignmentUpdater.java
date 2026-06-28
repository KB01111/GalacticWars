package middleearth.lotr.warmod.faction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FactionAlignmentUpdater {
    private FactionAlignmentUpdater() {
    }

    public static FactionAlignmentUpdateResult apply(
            FactionAlignment alignment,
            FactionCatalog catalog,
            FactionId sourceFaction,
            FactionAlignmentRule rule
    ) {
        Objects.requireNonNull(alignment, "alignment");
        Objects.requireNonNull(catalog, "catalog");
        Objects.requireNonNull(sourceFaction, "sourceFaction");
        Objects.requireNonNull(rule, "rule");
        if (!catalog.contains(sourceFaction)) {
            throw new IllegalArgumentException("Unknown source faction: " + sourceFaction);
        }

        FactionAlignment updated = alignment;
        ArrayList<FactionAlignmentChange> changes = new ArrayList<>();
        AlignmentApplication direct = applyDelta(updated, sourceFaction, rule.directDelta(), rule.reasonCode());
        updated = direct.alignment();
        changes.addAll(direct.changes());

        for (FactionId candidate : catalog.definitions().keySet()) {
            if (candidate.equals(sourceFaction)) {
                continue;
            }
            FactionRelation relation = catalog.relation(sourceFaction, candidate);
            int delta = switch (relation) {
                case ALLY -> rule.allyDelta();
                case ENEMY -> rule.enemyDelta();
                case SELF, NEUTRAL -> 0;
            };
            AlignmentApplication application = applyDelta(updated, candidate, delta, rule.reasonCode());
            updated = application.alignment();
            changes.addAll(application.changes());
        }

        return new FactionAlignmentUpdateResult(updated, changes);
    }

    private static AlignmentApplication applyDelta(
            FactionAlignment alignment,
            FactionId factionId,
            int delta,
            String reasonCode
    ) {
        if (delta == 0) {
            return new AlignmentApplication(alignment, List.of());
        }

        int beforeScore = alignment.score(factionId);
        FactionAlignment updated = alignment.withAddedScore(factionId, delta);
        return new AlignmentApplication(updated, List.of(new FactionAlignmentChange(
                factionId,
                beforeScore,
                delta,
                updated.score(factionId),
                reasonCode)));
    }

    private record AlignmentApplication(FactionAlignment alignment, List<FactionAlignmentChange> changes) {
    }
}
