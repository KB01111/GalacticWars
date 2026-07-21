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
        null,
        GameplayContentState.uninitialized(),
    )

    @JvmStatic
    fun current(): RuntimeSnapshot = activeSnapshot

    @JvmStatic
    fun install(
        definitions: LaunchContentDefinitions,
        factions: List<String>,
        units: Map<String, List<String>>,
    ) {
        val current = activeSnapshot
        activeSnapshot = RuntimeSnapshot(
            definitions,
            factions,
            units,
            current.gameplaySnapshot(),
            current.contentState(),
        )
    }

    /** Publishes every production view of an accepted datapack reload in one volatile write. */
    @JvmStatic
    fun installAccepted(
        gameplaySnapshot: GameplayDataSnapshot,
        state: GameplayContentState,
        factions: List<String>,
        units: Map<String, List<String>>,
    ) {
        activeSnapshot = RuntimeSnapshot(
            gameplaySnapshot.launchContent(),
            factions,
            units,
            gameplaySnapshot,
            state,
        )
    }

    /** Changes reload diagnostics without replacing the last accepted gameplay definitions. */
    @JvmStatic
    fun updateContentState(state: GameplayContentState) {
        val current = activeSnapshot
        activeSnapshot = RuntimeSnapshot(
            current.definitions(),
            current.factions(),
            current.units(),
            current.gameplaySnapshot(),
            state,
        )
    }

    class RuntimeSnapshot(
        definitions: LaunchContentDefinitions,
        factions: List<String>,
        units: Map<String, List<String>>,
        gameplaySnapshot: GameplayDataSnapshot?,
        contentState: GameplayContentState,
    ) {
        private val immutableDefinitions = definitions
        private val immutableFactions = Collections.unmodifiableList(
            ArrayList(factions.map { requireNotNull(it) { "faction" } }),
        )
        private val immutableUnits: Map<String, List<String>>
        private val immutableGameplaySnapshot = gameplaySnapshot
        private val immutableContentState = contentState

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

        fun gameplaySnapshot(): GameplayDataSnapshot? = immutableGameplaySnapshot

        fun contentState(): GameplayContentState = immutableContentState
    }
}
