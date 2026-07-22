package galacticwars.clonewars.progression

import galacticwars.clonewars.data.GameplayDataManager
import galacticwars.clonewars.workforce.WorkerProfession
import java.util.UUID
import kotlin.jvm.JvmRecord

/**
 * Dependency-light, server-authoritative validation for progression facts.
 *
 * A progression event is a record of gameplay that has already happened, not a
 * request to invent arbitrary progress. Every subject therefore has to resolve
 * to current launch content or to one of the fixed runtime identifiers below,
 * and every recorded occurrence has an exact authoritative amount.
 */
object ProgressionIntegrityPolicy {
    private const val MOD_NAMESPACE = "galacticwars"
    private const val CAMPAIGN_RECHECK_SUBJECT = "eligible_quests"

    private val coreBuildingSubjects = setOf(
        "command_center",
        "forward_base",
        "supply_depot",
    )

    @JvmStatic
    fun validate(event: ProgressionEvent): Validation {
        if (event.type == ProgressionEventType.CREDIT_TRANSACTION) {
            return Validation.rejected("physical_currency_required")
        }

        val expectedAmount = if (event.type == ProgressionEventType.CAMPAIGN_RECHECK) 0 else 1
        if (event.amount != expectedAmount) {
            return Validation.rejected("invalid_event_amount")
        }

        val definitions = LaunchContentCatalog.data()
        val subjectValid = when (event.type) {
            ProgressionEventType.CAMPAIGN_RECHECK -> event.subjectId == CAMPAIGN_RECHECK_SUBJECT
            ProgressionEventType.FACTION_PLEDGED ->
                matchesCatalogSubject(event.subjectId, LaunchContentCatalog.factions())
            ProgressionEventType.RECRUIT_HIRED -> matchesCatalogSubject(
                event.subjectId,
                LaunchContentCatalog.recruitSubjects().values.flatten(),
            )
            ProgressionEventType.PROFESSION_ASSIGNED -> matchesFixedSubject(
                event.subjectId,
                WorkerProfession.entries.map(WorkerProfession::id).toSet(),
            )
            ProgressionEventType.DELIVERY_COMPLETED -> isCourierSubject(event.subjectId)
            ProgressionEventType.BUILDING_COMPLETED -> matchesFixedSubject(
                event.subjectId,
                buildSet {
                    addAll(coreBuildingSubjects)
                    addAll(GameplayDataManager.snapshot().blueprints().keys.map(::subjectPath))
                },
            )
            ProgressionEventType.PLANET_VISITED ->
                matchesCatalogSubject(event.subjectId, definitions.planets().keys)
            ProgressionEventType.VEHICLE_ACQUIRED ->
                matchesCatalogSubject(event.subjectId, definitions.vehicles().keys)
            ProgressionEventType.FORCE_ABILITY_USED ->
                matchesCatalogSubject(event.subjectId, definitions.forceAbilities().keys)
            ProgressionEventType.MISSION_STARTED,
            ProgressionEventType.MISSION_FAILED,
            ProgressionEventType.MISSION_OBJECTIVE_COMPLETED,
            ProgressionEventType.MISSION_COMPLETED ->
                matchesCatalogSubject(event.subjectId, definitions.missions().keys)
            ProgressionEventType.REGION_DEFENDED ->
                matchesCatalogSubject(event.subjectId, definitions.conquestRegions().keys)
            ProgressionEventType.QUEST_ADVANCED ->
                matchesCatalogSubject(event.subjectId, definitions.quests().keys)
            ProgressionEventType.CAMPAIGN_COMPLETED -> event.subjectId in campaignSubjects(
                definitions.quests().keys,
            )
            ProgressionEventType.TRADE_COMPLETED ->
                matchesCatalogSubject(event.subjectId, definitions.trades().keys)
            ProgressionEventType.REGION_CAPTURED ->
                matchesCatalogSubject(event.subjectId, definitions.conquestRegions().keys)
            ProgressionEventType.CREDIT_TRANSACTION -> false
        }

        return if (subjectValid) {
            Validation.ACCEPTED
        } else {
            Validation.rejected(rejectionReason(event.type))
        }
    }

    private fun campaignSubjects(questIds: Collection<String>): Set<String> = questIds.mapNotNull { questId ->
        val marker = "_chapter_"
        val markerIndex = questId.lastIndexOf(marker)
        val chapter = if (markerIndex >= 0) questId.substring(markerIndex + marker.length) else ""
        if (markerIndex > 0 && chapter.isNotEmpty() && chapter.all(Char::isDigit)) {
            questId.substring(0, markerIndex) + "_campaign"
        } else {
            null
        }
    }.toSet()

    private fun matchesFixedSubject(subjectId: String, allowedPaths: Set<String>): Boolean {
        val path = modSubjectPath(subjectId) ?: return false
        return path in allowedPaths
    }

    private fun isCourierSubject(subjectId: String): Boolean {
        if (!subjectId.startsWith("courier/")) {
            return false
        }
        val workOrderId = subjectId.substringAfter("courier/")
        return runCatching { UUID.fromString(workOrderId).toString() == workOrderId }.getOrDefault(false)
    }

    private fun matchesCatalogSubject(subjectId: String, catalogIds: Collection<String>): Boolean {
        if (subjectId in catalogIds) {
            return true
        }
        val subjectPath = modSubjectPath(subjectId) ?: return false
        return catalogIds.any { catalogId ->
            catalogId == subjectPath || catalogId == "$MOD_NAMESPACE:$subjectPath"
        }
    }

    private fun modSubjectPath(subjectId: String): String? {
        val separator = subjectId.indexOf(':')
        if (separator < 0) {
            return subjectId
        }
        if (separator != subjectId.lastIndexOf(':') || subjectId.substring(0, separator) != MOD_NAMESPACE) {
            return null
        }
        return subjectId.substring(separator + 1).takeIf(String::isNotEmpty)
    }

    private fun subjectPath(subjectId: String): String = subjectId.substringAfter(':', subjectId)

    private fun rejectionReason(type: ProgressionEventType): String = when (type) {
        ProgressionEventType.CAMPAIGN_RECHECK -> "invalid_campaign_recheck"
        ProgressionEventType.FACTION_PLEDGED -> "unknown_faction"
        ProgressionEventType.CREDIT_TRANSACTION -> "physical_currency_required"
        ProgressionEventType.RECRUIT_HIRED -> "unknown_recruit"
        ProgressionEventType.PROFESSION_ASSIGNED -> "unknown_profession"
        ProgressionEventType.DELIVERY_COMPLETED -> "unknown_delivery"
        ProgressionEventType.BUILDING_COMPLETED -> "unknown_building"
        ProgressionEventType.PLANET_VISITED -> "unknown_planet"
        ProgressionEventType.VEHICLE_ACQUIRED -> "unknown_vehicle"
        ProgressionEventType.FORCE_ABILITY_USED -> "unknown_force_ability"
        ProgressionEventType.MISSION_STARTED,
        ProgressionEventType.MISSION_FAILED,
        ProgressionEventType.MISSION_OBJECTIVE_COMPLETED,
        ProgressionEventType.MISSION_COMPLETED -> "unknown_mission"
        ProgressionEventType.REGION_DEFENDED -> "unknown_region"
        ProgressionEventType.QUEST_ADVANCED -> "unknown_quest"
        ProgressionEventType.CAMPAIGN_COMPLETED -> "unknown_campaign"
        ProgressionEventType.TRADE_COMPLETED -> "unknown_trade"
        ProgressionEventType.REGION_CAPTURED -> "unknown_region"
    }

    @JvmRecord
    data class Validation(val accepted: Boolean, val reason: String) {
        init {
            require(reason.isNotBlank()) { "Progression validation reason cannot be blank" }
            require(accepted == (reason == "accepted")) {
                "Accepted progression validation must use the accepted reason"
            }
        }

        companion object {
            internal val ACCEPTED = Validation(true, "accepted")

            internal fun rejected(reason: String) = Validation(false, reason)
        }
    }
}
