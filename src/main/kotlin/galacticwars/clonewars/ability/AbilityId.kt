package galacticwars.clonewars.ability

import java.util.Locale

class AbilityId(namespace: String?, path: String?) {
    val namespace: String = normalize(namespace, "namespace")
    val path: String = normalize(path, "path")

    init {
        require(this.namespace == DEFAULT_NAMESPACE && PATH.matches(this.path)) {
            "Invalid ability id ${this.namespace}:${this.path}"
        }
    }

    fun namespace(): String = namespace

    fun path(): String = path

    override fun equals(other: Any?): Boolean =
        this === other || other is AbilityId && namespace == other.namespace && path == other.path

    override fun hashCode(): Int = 31 * namespace.hashCode() + path.hashCode()

    override fun toString(): String = "$namespace:$path"

    companion object {
        const val DEFAULT_NAMESPACE: String = "galacticwars"
        private val PATH = Regex("[a-z0-9]+(?:_[a-z0-9]+)*")

        @JvmStatic
        fun of(value: String?): AbilityId {
            val normalized = requireNotNull(value) { "value" }.trim().lowercase(Locale.ROOT)
            val separator = normalized.indexOf(':')
            return if (separator < 0) {
                AbilityId(DEFAULT_NAMESPACE, normalized)
            } else {
                AbilityId(normalized.substring(0, separator), normalized.substring(separator + 1))
            }
        }

        private fun normalize(value: String?, label: String): String {
            val normalized = requireNotNull(value) { label }.trim().lowercase(Locale.ROOT)
            require(normalized.isNotEmpty()) { "$label cannot be blank" }
            return normalized
        }
    }
}
