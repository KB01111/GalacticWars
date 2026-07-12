package galacticwars.clonewars.kingdom;

import galacticwars.clonewars.progression.ProgressionDecision;
import galacticwars.clonewars.progression.ProgressionState;
import java.util.Objects;

public record KingdomGameplayResult(
        boolean accepted,
        boolean changed,
        String reason,
        ProgressionState progressionState
) {
    public KingdomGameplayResult {
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(progressionState, "progressionState");
    }

    public static KingdomGameplayResult from(ProgressionDecision decision) {
        Objects.requireNonNull(decision, "decision");
        String reason = decision.accepted()
                ? (decision.changed() ? "accepted" : "duplicate_action")
                : decision.reason();
        return new KingdomGameplayResult(
                decision.accepted(), decision.changed(), reason, decision.state());
    }
}
