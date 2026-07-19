package galacticwars.clonewars.progression

import kotlin.jvm.JvmRecord

/** Result of applying a progression fact without mutating its input state. */
@JvmRecord
data class ProgressionDecision(
    val accepted: Boolean,
    val changed: Boolean,
    val reason: String,
    val state: ProgressionState,
) {
    companion object {
        @JvmStatic
        fun accepted(before: ProgressionState, after: ProgressionState): ProgressionDecision =
            ProgressionDecision(
                accepted = true,
                changed = before !== after,
                reason = if (before === after) "duplicate_event" else "accepted",
                state = after,
            )

        @JvmStatic
        fun rejected(reason: String, state: ProgressionState): ProgressionDecision =
            ProgressionDecision(false, false, reason, state)
    }
}
