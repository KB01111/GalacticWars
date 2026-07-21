package galacticwars.clonewars.kingdom;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.progression.ProgressionDecision;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.progression.ProgressionState;
import java.util.Objects;

/** Minecraft runtime adapter that commits evaluated kingdom gameplay actions to SavedData. */
public final class KingdomGameplayRuntimeService {
    private KingdomGameplayRuntimeService() {
    }

    public static KingdomGameplayResult applyProgression(
            ProgressionSavedData progression,
            KingdomGameplayAction action
    ) {
        Objects.requireNonNull(progression, "progression");
        Objects.requireNonNull(action, "action");
        ProgressionState before = progression.state(action.playerId());
        ProgressionDecision evaluated = KingdomGameplayTransactionService.evaluateDecision(before, action);
        if (!evaluated.accepted() || !evaluated.changed()) {
            return KingdomGameplayResult.from(evaluated);
        }
        try {
            ProgressionDecision committed = progression.commitEvaluated(
                    action.progressionEvent(), before, evaluated);
            return KingdomGameplayResult.from(committed);
        } catch (RuntimeException failure) {
            GalacticWars.LOGGER.error("Progression transaction {} failed for {}",
                    action.id(), action.playerId(), failure);
            return new KingdomGameplayResult(false, false, "transaction_failed", before);
        }
    }
}
