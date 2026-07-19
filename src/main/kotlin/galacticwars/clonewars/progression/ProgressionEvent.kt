package galacticwars.clonewars.progression

import java.util.Locale
import java.util.UUID

/** Immutable, normalized fact emitted by an authoritative gameplay system. */
class ProgressionEvent(
    val id: UUID,
    val playerId: UUID,
    val type: ProgressionEventType,
    subjectId: String,
    val amount: Int,
) {
    val subjectId: String = normalizeSubject(subjectId)

    init {
        if (type != ProgressionEventType.CREDIT_TRANSACTION && amount < 0) {
            throw IllegalArgumentException("Only credit transactions may have a negative amount")
        }
    }

    fun id(): UUID = id

    fun playerId(): UUID = playerId

    fun type(): ProgressionEventType = type

    fun subjectId(): String = subjectId

    fun amount(): Int = amount

    override fun equals(other: Any?): Boolean = this === other ||
        other is ProgressionEvent &&
        id == other.id &&
        playerId == other.playerId &&
        type == other.type &&
        subjectId == other.subjectId &&
        amount == other.amount

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + playerId.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + subjectId.hashCode()
        return 31 * result + amount
    }

    override fun toString(): String =
        "ProgressionEvent[id=$id, playerId=$playerId, type=$type, subjectId=$subjectId, amount=$amount]"

    private companion object {
        private val subjectPattern = Regex("[a-z0-9_:.\\-/]+")

        fun normalizeSubject(value: String): String {
            val normalized = value.trim().lowercase(Locale.ROOT)
            require(normalized.isNotEmpty() && subjectPattern.matches(normalized)) {
                "Invalid progression subject: $value"
            }
            return normalized
        }
    }
}
