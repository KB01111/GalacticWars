package galacticwars.clonewars.progression

import java.util.Collections
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.Objects
import java.util.UUID

/** Immutable Force energy, cooldown, path, and activation replay state. */
class ForceRuntimeState(
    path: String?,
    energy: Int,
    cooldownEnds: Map<String, Long>,
    processedActivationIds: Set<UUID>,
) {
    val path: String = path.orEmpty()
    val energy: Int = energy
    val cooldownEnds: Map<String, Long> = immutableCooldowns(cooldownEnds)
    val processedActivationIds: Set<UUID> = boundedActivationIds(processedActivationIds)

    init {
        require(path.isNullOrEmpty() || path == "light" || path == "dark") {
            "Unknown Force path $path"
        }
        require(energy in 0..MAX_ENERGY) {
            "Force energy must be between 0 and $MAX_ENERGY"
        }
    }

    constructor(energy: Int, cooldownEnds: Map<String, Long>) :
        this("", energy, cooldownEnds, emptySet())

    fun path(): String = path

    fun energy(): Int = energy

    fun cooldownEnds(): Map<String, Long> = cooldownEnds

    fun processedActivationIds(): Set<UUID> = processedActivationIds

    fun withPath(selectedPath: String?): ForceRuntimeState =
        ForceRuntimeState(selectedPath, energy, cooldownEnds, processedActivationIds)

    fun regenerate(amount: Int): ForceRuntimeState {
        require(amount >= 0) { "regeneration amount cannot be negative" }
        val regeneratedEnergy = minOf(MAX_ENERGY.toLong(), energy.toLong() + amount).toInt()
        return ForceRuntimeState(path, regeneratedEnergy, cooldownEnds, processedActivationIds)
    }

    override fun equals(other: Any?): Boolean = this === other ||
        other is ForceRuntimeState &&
        path == other.path &&
        energy == other.energy &&
        cooldownEnds == other.cooldownEnds &&
        processedActivationIds == other.processedActivationIds

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + energy
        result = 31 * result + cooldownEnds.hashCode()
        return 31 * result + processedActivationIds.hashCode()
    }

    override fun toString(): String = "ForceRuntimeState[" +
        "path=$path, energy=$energy, cooldownEnds=$cooldownEnds, " +
        "processedActivationIds=$processedActivationIds]"

    companion object {
        const val MAX_ENERGY: Int = 100
        const val MAX_PROCESSED_ACTIVATIONS: Int = 128

        @JvmStatic
        fun full(): ForceRuntimeState = ForceRuntimeState("", MAX_ENERGY, emptyMap(), emptySet())

        private fun immutableCooldowns(source: Map<String, Long>): Map<String, Long> {
            val copy = LinkedHashMap<String, Long>(source.size)
            source.forEach { (abilityId, cooldownEnd) ->
                copy[Objects.requireNonNull(abilityId, "cooldown abilityId")] =
                    Objects.requireNonNull(cooldownEnd, "cooldown end")
            }
            return Collections.unmodifiableMap(copy)
        }

        private fun boundedActivationIds(source: Set<UUID>): Set<UUID> {
            val copy = LinkedHashSet<UUID>(source.size)
            source.forEach { copy.add(Objects.requireNonNull(it, "processed activation id")) }
            while (copy.size > MAX_PROCESSED_ACTIVATIONS) {
                val iterator = copy.iterator()
                iterator.next()
                iterator.remove()
            }
            return Collections.unmodifiableSet(copy)
        }
    }
}
