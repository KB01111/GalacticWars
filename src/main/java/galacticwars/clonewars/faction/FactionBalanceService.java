package galacticwars.clonewars.faction;

import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.data.LaunchContentDefinitions;
import java.util.Objects;
import java.util.Set;

/** Resolves data-driven faction modifiers into bounded, typed gameplay values. */
public final class FactionBalanceService {
    public static final int MIN_PERCENT = 0;
    public static final int MAX_PERCENT = 500;

    private static final Set<String> KNOWN_MODIFIER_KEYS = Set.of(
            "coordination_percent",
            "loyalty_percent",
            "mobility_percent",
            "morale_percent",
            "production_percent",
            "recruitment_capacity_percent",
            "supply_efficiency_percent",
            "trade_value_percent",
            "upkeep_percent",
            "without_command_node_percent");

    private FactionBalanceService() {
    }

    /** Resolves the currently loaded definition and runtime policy for a faction. */
    public static ResolvedBalance resolve(String factionId) {
        if (factionId == null || factionId.isBlank()) {
            return ResolvedBalance.neutral();
        }
        FactionId id;
        try {
            id = FactionId.of(factionId);
        } catch (IllegalArgumentException exception) {
            return ResolvedBalance.neutral();
        }
        var snapshot = GameplayDataManager.snapshot();
        FactionDefinition definition = snapshot.factions().definition(id).orElse(null);
        if (definition == null) {
            return ResolvedBalance.neutral();
        }
        return resolve(definition, snapshot.factionPolicy(id).orElse(null));
    }

    /**
     * Pure resolver used by runtime code and dependency-light tests. A runtime
     * modifier replaces its strategy fallback; the two sources are never multiplied.
     */
    public static ResolvedBalance resolve(
            FactionDefinition definition, FactionRuntimePolicy runtimePolicy
    ) {
        Objects.requireNonNull(definition, "definition");
        validatePolicy(definition, runtimePolicy);
        FactionStrategyDefinition strategy = definition.strategy();
        return new ResolvedBalance(
                percent(runtimePolicy, "recruitment_capacity_percent", 100),
                percent(runtimePolicy, "upkeep_percent", strategy.upkeepPercent()),
                percent(runtimePolicy, "production_percent", strategy.productionPercent()),
                strategy.moraleBonus(),
                percent(runtimePolicy, "coordination_percent", 100),
                percent(runtimePolicy, "supply_efficiency_percent", 100),
                percent(runtimePolicy, "without_command_node_percent", 100),
                percent(runtimePolicy, "mobility_percent", 100),
                percent(runtimePolicy, "trade_value_percent", 100),
                percent(runtimePolicy, "loyalty_percent", 100),
                percent(runtimePolicy, "morale_percent", 100));
    }

    /** Returns floor((base maximum + strategy bonus) * runtime capacity / 100). */
    public static int effectiveRecruitLimit(String factionId) {
        if (factionId == null || factionId.isBlank()) {
            return 0;
        }
        FactionId id;
        try {
            id = FactionId.of(factionId);
        } catch (IllegalArgumentException exception) {
            return 0;
        }
        var snapshot = GameplayDataManager.snapshot();
        FactionDefinition definition = snapshot.factions().definition(id).orElse(null);
        if (definition == null) {
            return 0;
        }
        return effectiveRecruitLimit(definition, snapshot.factionPolicy(id).orElse(null));
    }

    /** Pure, overflow-safe recruit-limit calculation. */
    public static int effectiveRecruitLimit(
            FactionDefinition definition, FactionRuntimePolicy runtimePolicy
    ) {
        Objects.requireNonNull(definition, "definition");
        ResolvedBalance balance = resolve(definition, runtimePolicy);
        long unscaledLimit = (long) definition.maxOwnedRecruits()
                + definition.strategy().recruitmentCapacityBonus();
        long scaledLimit = unscaledLimit * balance.recruitmentCapacityPercent() / 100L;
        return clampToInt(scaledLimit);
    }

    /** Applies the loaded faction's trade-value modifier to a server credit price. */
    public static int tradeCreditPrice(String factionId, int basePrice) {
        return tradeCreditPrice(basePrice, resolve(factionId));
    }

    /** Returns ceil(basePrice * 100 / tradeValuePercent), bounded to the packet price limit. */
    public static int tradeCreditPrice(int basePrice, ResolvedBalance balance) {
        if (basePrice <= 0) {
            throw new IllegalArgumentException("basePrice must be positive");
        }
        Objects.requireNonNull(balance, "balance");
        int tradeValue = Math.max(1, clampPercent(balance.tradeValuePercent()));
        long adjusted = ((long) basePrice * 100L + tradeValue - 1L) / tradeValue;
        return (int) Math.max(1L, Math.min(
                LaunchContentDefinitions.MAX_TRADE_CREDIT_PRICE, adjusted));
    }

    /** Applies a bounded percentage with floor rounding and overflow-safe arithmetic. */
    public static int applyPercentFloor(int value, int percent) {
        if (value < 0) {
            throw new IllegalArgumentException("value cannot be negative");
        }
        return clampToInt((long) value * clampPercent(percent) / 100L);
    }

    /** Applies a bounded percentage with ceiling rounding and overflow-safe arithmetic. */
    public static int applyPercentCeil(int value, int percent) {
        if (value < 0) {
            throw new IllegalArgumentException("value cannot be negative");
        }
        long product = (long) value * clampPercent(percent);
        return clampToInt((product + 99L) / 100L);
    }

    public static Set<String> knownModifierKeys() {
        return KNOWN_MODIFIER_KEYS;
    }

    private static void validatePolicy(
            FactionDefinition definition, FactionRuntimePolicy runtimePolicy
    ) {
        if (runtimePolicy == null) {
            return;
        }
        if (!definition.id().equals(runtimePolicy.factionId())) {
            throw new IllegalArgumentException("Runtime policy faction does not match definition");
        }
        for (String key : runtimePolicy.modifiers().keySet()) {
            if (!KNOWN_MODIFIER_KEYS.contains(key)) {
                throw new IllegalArgumentException("Unknown faction modifier key: " + key);
            }
        }
    }

    private static int percent(
            FactionRuntimePolicy runtimePolicy, String modifier, int strategyFallback
    ) {
        int value = runtimePolicy == null
                ? strategyFallback
                : runtimePolicy.modifier(modifier, strategyFallback);
        return clampPercent(value);
    }

    private static int clampPercent(int percent) {
        return Math.max(MIN_PERCENT, Math.min(MAX_PERCENT, percent));
    }

    private static int clampToInt(long value) {
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, value));
    }

    public record ResolvedBalance(
            int recruitmentCapacityPercent,
            int upkeepPercent,
            int productionPercent,
            int moraleBonus,
            int coordinationPercent,
            int supplyEfficiencyPercent,
            int withoutCommandNodePercent,
            int mobilityPercent,
            int tradeValuePercent,
            int loyaltyPercent,
            int moralePercent
    ) {
        public ResolvedBalance {
            recruitmentCapacityPercent = clampPercent(recruitmentCapacityPercent);
            upkeepPercent = clampPercent(upkeepPercent);
            productionPercent = clampPercent(productionPercent);
            moraleBonus = Math.max(-100, Math.min(100, moraleBonus));
            coordinationPercent = clampPercent(coordinationPercent);
            supplyEfficiencyPercent = clampPercent(supplyEfficiencyPercent);
            withoutCommandNodePercent = clampPercent(withoutCommandNodePercent);
            mobilityPercent = clampPercent(mobilityPercent);
            tradeValuePercent = clampPercent(tradeValuePercent);
            loyaltyPercent = clampPercent(loyaltyPercent);
            moralePercent = clampPercent(moralePercent);
        }

        public static ResolvedBalance neutral() {
            return new ResolvedBalance(100, 100, 100, 0, 100, 100, 100, 100, 100, 100, 100);
        }
    }
}
