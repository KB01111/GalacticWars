package galacticwars.clonewars.world

import galacticwars.clonewars.data.GameplayDataSnapshot
import galacticwars.clonewars.data.LaunchContentDefinitions
import galacticwars.clonewars.faction.FactionId
import galacticwars.clonewars.recruitment.NpcServiceBranch
import java.util.Collections
import java.util.LinkedHashSet
import java.util.Locale
import kotlin.jvm.JvmRecord

/**
 * Loader-neutral authority for deciding which data-driven units belong in a planet dimension.
 *
 * Planet ownership comes from the launch planet catalog. Unit ownership and entity identity come
 * from the accepted gameplay snapshot, so loader adapters never maintain parallel faction lists.
 */
object PlanetFactionSpawnPolicy {
    @JvmStatic
    fun evaluate(
        snapshot: GameplayDataSnapshot,
        dimensionId: String,
        entityTypeId: String,
    ): Evaluation {
        val normalizedDimension = normalizeIdentifier(dimensionId)
        val planet = planetForDimension(snapshot.launchContent(), normalizedDimension)
            ?: return Evaluation(false, false, "", "", "", null)
        val factionId = FactionId.of(planet.factionId()).toString()
        val normalizedEntityType = normalizeIdentifier(entityTypeId)
        val unit = snapshot.unitForEntityType(normalizedEntityType).orElse(null)
        val civilian = snapshot.civilianArchetypeForEntity(normalizedEntityType).orElse(null)
        check(unit == null || civilian == null) {
            "Entity type $normalizedEntityType is both a military unit and civilian archetype"
        }
        if (unit != null) {
            return Evaluation(
                knownPlanetDimension = true,
                allowed = unit.factionId().toString() == factionId,
                planetId = planet.id(),
                factionId = factionId,
                definitionId = unit.id().toString(),
                serviceBranch = NpcServiceBranch.MILITARY,
            )
        }
        if (civilian != null) {
            return Evaluation(
                knownPlanetDimension = true,
                allowed = FactionId.of(civilian.factionId()).toString() == factionId,
                planetId = planet.id(),
                factionId = factionId,
                definitionId = civilian.id(),
                serviceBranch = NpcServiceBranch.CIVILIAN,
            )
        }
        return Evaluation(true, false, planet.id(), factionId, "", null)
    }

    @JvmStatic
    fun allowedEntityTypeIds(snapshot: GameplayDataSnapshot, dimensionId: String): Set<String> {
        val normalizedDimension = normalizeIdentifier(dimensionId)
        if (planetForDimension(snapshot.launchContent(), normalizedDimension) == null) {
            return emptySet()
        }
        val allowed = LinkedHashSet<String>()
        val candidateEntityTypes = LinkedHashSet<String>()
        candidateEntityTypes.addAll(snapshot.unitIdsByEntityType().keys)
        candidateEntityTypes.addAll(snapshot.civilianArchetypesByEntityType().keys)
        candidateEntityTypes.forEach { rawEntityTypeId ->
            val entityTypeId = normalizeIdentifier(rawEntityTypeId)
            if (evaluate(snapshot, normalizedDimension, entityTypeId).allowed) {
                allowed.add(entityTypeId)
            }
        }
        return Collections.unmodifiableSet(allowed)
    }

    private fun planetForDimension(
        definitions: LaunchContentDefinitions,
        normalizedDimensionId: String,
    ): LaunchContentDefinitions.PlanetDefinition? {
        var match: LaunchContentDefinitions.PlanetDefinition? = null
        definitions.planets().values.forEach { planet ->
            if (normalizeIdentifier(planet.dimensionId()) == normalizedDimensionId) {
                check(match == null) {
                    "Multiple launch planets target dimension $normalizedDimensionId"
                }
                match = planet
            }
        }
        return match
    }

    private fun normalizeIdentifier(value: String): String =
        value.trim().lowercase(Locale.ROOT)

    @JvmRecord
    data class Evaluation(
        val knownPlanetDimension: Boolean,
        val allowed: Boolean,
        val planetId: String,
        val factionId: String,
        val definitionId: String,
        val serviceBranch: NpcServiceBranch?,
    ) {
        init {
            require(!allowed || knownPlanetDimension) {
                "An allowed spawn must belong to a known planet dimension"
            }
            require(!allowed || serviceBranch != null) {
                "An allowed spawn must resolve to a service branch"
            }
        }
    }
}
