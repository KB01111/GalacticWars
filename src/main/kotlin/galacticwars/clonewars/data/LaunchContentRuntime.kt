package galacticwars.clonewars.data

import java.util.Collections
import java.util.ArrayList
import java.util.LinkedHashMap

/**
 * Dependency-light view of the last atomically accepted gameplay snapshot.
 * Runtime consumers never retain reload-owned mutable collections.
 */
object LaunchContentRuntime {
    @Volatile
    private var activeSnapshot: RuntimeSnapshot = RuntimeSnapshot(
        LaunchContentDefinitions.empty(),
        emptyList(),
        emptyMap(),
    )

    @JvmStatic
    fun current(): RuntimeSnapshot = activeSnapshot

    @JvmStatic
    fun install(
        definitions: LaunchContentDefinitions,
        factions: List<String>,
        units: Map<String, List<String>>,
    ) {
        activeSnapshot = RuntimeSnapshot(definitions, factions, units)
    }

    class RuntimeSnapshot(
        definitions: LaunchContentDefinitions,
        factions: List<String>,
        units: Map<String, List<String>>,
    ) {
        private val immutableDefinitions = definitions
        private val immutableFactions = Collections.unmodifiableList(
            ArrayList(factions.map { requireNotNull(it) { "faction" } }),
        )
        private val immutableUnits: Map<String, List<String>>

        init {
            val copiedUnits = LinkedHashMap<String, List<String>>(units.size)
            units.forEach { (faction, ids) ->
                copiedUnits[requireNotNull(faction) { "unit faction" }] = Collections.unmodifiableList(
                    ArrayList(ids.map { requireNotNull(it) { "unit id for $faction" } }),
                )
            }
            immutableUnits = Collections.unmodifiableMap(copiedUnits)
        }

        fun definitions(): LaunchContentDefinitions = immutableDefinitions

        fun factions(): List<String> = immutableFactions

        fun units(): Map<String, List<String>> = immutableUnits
    }
}
