package galacticwars.clonewars.ability

class AbilityDefinition(
    id: AbilityId?,
    displayName: String?,
    kind: AbilityKind?,
    activation: AbilityActivation?,
    val cooldownTicks: Int,
    val resourceCost: Int,
    val range: Double,
    val aiEvaluationIntervalTicks: Int,
    val enabled: Boolean,
) {
    val id: AbilityId = requireNotNull(id) { "id" }
    val displayName: String = requireNonBlank(displayName, "displayName")
    val kind: AbilityKind = requireNotNull(kind) { "kind" }
    val activation: AbilityActivation = requireNotNull(activation) { "activation" }

    init {
        require(cooldownTicks >= 0) { "cooldownTicks cannot be negative" }
        require(resourceCost >= 0) { "resourceCost cannot be negative" }
        require(range.isFinite() && range >= 0.0) { "range cannot be negative or non-finite" }
        require(aiEvaluationIntervalTicks >= 1) { "aiEvaluationIntervalTicks must be positive" }
        require(activation != AbilityActivation.PASSIVE || cooldownTicks == 0 && resourceCost == 0) {
            "Passive abilities cannot consume resources or use cooldowns"
        }
    }

    fun id(): AbilityId = id

    fun displayName(): String = displayName

    fun kind(): AbilityKind = kind

    fun activation(): AbilityActivation = activation

    fun cooldownTicks(): Int = cooldownTicks

    fun resourceCost(): Int = resourceCost

    fun range(): Double = range

    fun aiEvaluationIntervalTicks(): Int = aiEvaluationIntervalTicks

    fun enabled(): Boolean = enabled

    fun active(): Boolean = activation != AbilityActivation.PASSIVE

    override fun equals(other: Any?): Boolean =
        this === other || other is AbilityDefinition &&
            id == other.id &&
            displayName == other.displayName &&
            kind == other.kind &&
            activation == other.activation &&
            cooldownTicks == other.cooldownTicks &&
            resourceCost == other.resourceCost &&
            range.compareTo(other.range) == 0 &&
            aiEvaluationIntervalTicks == other.aiEvaluationIntervalTicks &&
            enabled == other.enabled

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + kind.hashCode()
        result = 31 * result + activation.hashCode()
        result = 31 * result + cooldownTicks
        result = 31 * result + resourceCost
        result = 31 * result + range.hashCode()
        result = 31 * result + aiEvaluationIntervalTicks
        result = 31 * result + enabled.hashCode()
        return result
    }

    override fun toString(): String =
        "AbilityDefinition[id=$id, displayName=$displayName, kind=$kind, activation=$activation, " +
            "cooldownTicks=$cooldownTicks, resourceCost=$resourceCost, range=$range, " +
            "aiEvaluationIntervalTicks=$aiEvaluationIntervalTicks, enabled=$enabled]"

    private companion object {
        fun requireNonBlank(value: String?, label: String): String {
            val normalized = requireNotNull(value) { label }.trim()
            require(normalized.isNotEmpty()) { "$label cannot be blank" }
            return normalized
        }
    }
}
