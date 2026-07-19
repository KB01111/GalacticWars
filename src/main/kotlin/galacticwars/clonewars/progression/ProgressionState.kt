package galacticwars.clonewars.progression

import java.util.Collections
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Objects
import java.util.UUID

/** Immutable campaign state. All incoming and nested collections are defensively copied. */
class ProgressionState(
    schemaVersion: Int,
    playerId: UUID,
    factionId: String?,
    credits: Int,
    processedEventIds: Set<UUID>,
    eventTotals: Map<ProgressionEventType, Int>,
    eventSubjects: Map<ProgressionEventType, Set<String>>,
    unlocks: Set<String>,
) {
    val schemaVersion: Int = schemaVersion
    val playerId: UUID = playerId
    val factionId: String = factionId.orEmpty()
    val credits: Int = credits
    val processedEventIds: Set<UUID> = immutableRecentSet(
        processedEventIds,
        "processedEventIds",
        MAX_PROCESSED_EVENT_IDS,
    )
    val eventTotals: Map<ProgressionEventType, Int> = immutableMap(eventTotals, "eventTotals")
    val eventSubjects: Map<ProgressionEventType, Set<String>> = immutableSubjectMap(eventSubjects)
    val unlocks: Set<String> = immutableSet(unlocks, "unlocks")

    init {
        require(schemaVersion == CURRENT_SCHEMA_VERSION) {
            "Unsupported progression schema $schemaVersion"
        }
        require(credits >= 0) { "credits cannot be negative" }
    }

    fun schemaVersion(): Int = schemaVersion

    fun playerId(): UUID = playerId

    fun factionId(): String = factionId

    fun credits(): Int = credits

    fun processedEventIds(): Set<UUID> = processedEventIds

    fun eventTotals(): Map<ProgressionEventType, Int> = eventTotals

    fun eventSubjects(): Map<ProgressionEventType, Set<String>> = eventSubjects

    fun unlocks(): Set<String> = unlocks

    fun processed(eventId: UUID): Boolean = eventId in processedEventIds

    /** Pending physical Credit Chips earned by campaign rewards. */
    fun pendingCreditRewards(): Int = credits

    fun clearPendingCreditRewards(): ProgressionState = if (credits == 0) {
        this
    } else {
        ProgressionState(
            schemaVersion,
            playerId,
            factionId,
            0,
            processedEventIds,
            eventTotals,
            eventSubjects,
            unlocks,
        )
    }

    fun total(type: ProgressionEventType): Int = eventTotals[type] ?: 0

    fun hasSubject(type: ProgressionEventType, subjectId: String): Boolean =
        eventSubjects[type]?.contains(subjectId) == true

    fun hasSubjectPath(type: ProgressionEventType, subjectPath: String): Boolean =
        eventSubjects[type].orEmpty().any { subject ->
            subject.substringAfter(':', subject) == subjectPath
        }

    fun apply(event: ProgressionEvent, faction: String, addedUnlocks: Set<String>): ProgressionState {
        if (playerId != event.playerId) {
            throw SecurityException("Progression event belongs to another player")
        }
        if (processed(event.id)) {
            return this
        }

        val creditDelta = when (event.type) {
            ProgressionEventType.QUEST_ADVANCED -> LaunchContentCatalog.questRewardCredits(event.subjectId)
            ProgressionEventType.REGION_CAPTURED -> LaunchContentCatalog.regionRewardCredits(event.subjectId)
            else -> 0
        }
        val updatedCredits = Math.addExact(credits, creditDelta)
        check(updatedCredits >= 0) { "insufficient_credits" }

        val ids = LinkedHashSet(processedEventIds)
        ids.add(event.id)

        val totals = LinkedHashMap(eventTotals)
        totals.merge(event.type, maxOf(1, event.amount)) { current, increment ->
            Math.addExact(current, increment)
        }

        val subjects = LinkedHashMap(eventSubjects)
        val values = LinkedHashSet(subjects[event.type].orEmpty())
        values.add(event.subjectId)
        subjects[event.type] = values

        val updatedUnlocks = LinkedHashSet(unlocks)
        updatedUnlocks.addAll(addedUnlocks)
        return ProgressionState(
            schemaVersion,
            playerId,
            faction,
            updatedCredits,
            ids,
            totals,
            subjects,
            updatedUnlocks,
        )
    }

    override fun equals(other: Any?): Boolean = this === other ||
        other is ProgressionState &&
        schemaVersion == other.schemaVersion &&
        playerId == other.playerId &&
        factionId == other.factionId &&
        credits == other.credits &&
        processedEventIds == other.processedEventIds &&
        eventTotals == other.eventTotals &&
        eventSubjects == other.eventSubjects &&
        unlocks == other.unlocks

    override fun hashCode(): Int {
        var result = schemaVersion
        result = 31 * result + playerId.hashCode()
        result = 31 * result + factionId.hashCode()
        result = 31 * result + credits
        result = 31 * result + processedEventIds.hashCode()
        result = 31 * result + eventTotals.hashCode()
        result = 31 * result + eventSubjects.hashCode()
        return 31 * result + unlocks.hashCode()
    }

    override fun toString(): String = "ProgressionState[" +
        "schemaVersion=$schemaVersion, playerId=$playerId, factionId=$factionId, credits=$credits, " +
        "processedEventIds=$processedEventIds, eventTotals=$eventTotals, " +
        "eventSubjects=$eventSubjects, unlocks=$unlocks]"

    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 3
        const val MAX_PROCESSED_EVENT_IDS: Int = 512
        /**
         * Server-side history bound, deliberately separate from the attachment protocol's
         * 64-entry client projection. Sync snapshots do not modify this authoritative collection.
         */
        const val MAX_AUTHORITATIVE_SUBJECTS_PER_TYPE: Int = 1_024
        const val MAX_RECENT_DELIVERY_SUBJECTS: Int = 32

        @JvmStatic
        fun create(playerId: UUID): ProgressionState = ProgressionState(
            CURRENT_SCHEMA_VERSION,
            playerId,
            "",
            0,
            emptySet(),
            emptyMap(),
            emptyMap(),
            setOf("intro_quest"),
        )

        private fun <T : Any> immutableSet(source: Set<T>, label: String): Set<T> {
            val copy = LinkedHashSet<T>(source.size)
            source.forEach { copy.add(Objects.requireNonNull(it, label)) }
            return Collections.unmodifiableSet(copy)
        }

        private fun <T : Any> immutableRecentSet(
            source: Set<T>,
            label: String,
            limit: Int,
        ): Set<T> {
            require(limit >= 0) { "limit cannot be negative" }
            val skip = maxOf(0, source.size - limit)
            val copy = LinkedHashSet<T>(minOf(source.size, limit))
            source.forEachIndexed { index, value ->
                if (index >= skip) {
                    copy.add(Objects.requireNonNull(value, label))
                }
            }
            return Collections.unmodifiableSet(copy)
        }

        private fun <K : Any, V : Any> immutableMap(source: Map<K, V>, label: String): Map<K, V> {
            val copy = LinkedHashMap<K, V>(source.size)
            source.forEach { (key, value) ->
                copy[Objects.requireNonNull(key, "$label key")] =
                    Objects.requireNonNull(value, "$label value")
            }
            return Collections.unmodifiableMap(copy)
        }

        private fun immutableSubjectMap(
            source: Map<ProgressionEventType, Set<String>>,
        ): Map<ProgressionEventType, Set<String>> {
            val copy = LinkedHashMap<ProgressionEventType, Set<String>>(source.size)
            source.forEach { (type, subjects) ->
                val normalizedType = Objects.requireNonNull(type, "eventSubjects type")
                val limit = if (normalizedType == ProgressionEventType.DELIVERY_COMPLETED) {
                    MAX_RECENT_DELIVERY_SUBJECTS
                } else {
                    MAX_AUTHORITATIVE_SUBJECTS_PER_TYPE
                }
                copy[normalizedType] = immutableRecentSet(
                    Objects.requireNonNull(subjects, "eventSubjects values"),
                    "eventSubjects subject",
                    limit,
                )
            }
            return Collections.unmodifiableMap(copy)
        }
    }
}
