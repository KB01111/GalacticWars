package galacticwars.clonewars.faction;

import java.util.Map;
import java.util.Set;

public final class FactionBalanceServiceTest {
    private FactionBalanceServiceTest() {
    }

    public static void main(String[] args) {
        resolvesTypedStrategyFallbacksAndRuntimeOverrides();
        rejectsUnknownAndMismatchedRuntimePolicies();
        calculatesBoundedRecruitCapacityWithoutOverflow();
        calculatesFactionTradePricesWithCeilingRounding();
        appliesBoundedPercentagesWithoutOverflow();
        System.out.println("FactionBalanceServiceTest passed");
    }

    private static void resolvesTypedStrategyFallbacksAndRuntimeOverrides() {
        FactionDefinition faction = faction(10, 4);
        FactionRuntimePolicy runtime = new FactionRuntimePolicy(
                faction.id(),
                Set.of("mass_production"),
                Map.of(
                        "production_percent", 85,
                        "coordination_percent", 115,
                        "trade_value_percent", 120));

        FactionBalanceService.ResolvedBalance balance =
                FactionBalanceService.resolve(faction, runtime);

        assertEquals(85, balance.productionPercent(),
                "runtime production replaces rather than multiplies strategy production");
        assertEquals(120, balance.upkeepPercent(), "strategy upkeep fallback");
        assertEquals(15, balance.moraleBonus(), "strategy morale bonus");
        assertEquals(115, balance.coordinationPercent(), "typed coordination modifier");
        assertEquals(120, balance.tradeValuePercent(), "typed trade modifier");
        assertEquals(100, balance.mobilityPercent(), "neutral mobility fallback");
        assertTrue(FactionBalanceService.knownModifierKeys().containsAll(Set.of(
                "coordination_percent",
                "loyalty_percent",
                "mobility_percent",
                "morale_percent",
                "production_percent",
                "recruitment_capacity_percent",
                "supply_efficiency_percent",
                "trade_value_percent",
                "upkeep_percent",
                "without_command_node_percent")), "known modifier whitelist");
    }

    private static void rejectsUnknownAndMismatchedRuntimePolicies() {
        FactionDefinition faction = faction(10, 4);
        assertThrows(IllegalArgumentException.class, () -> FactionBalanceService.resolve(
                faction,
                new FactionRuntimePolicy(faction.id(), Set.of(), Map.of("unbounded_damage", 500))),
                "unknown modifier");
        assertThrows(IllegalArgumentException.class, () -> FactionBalanceService.resolve(
                faction,
                new FactionRuntimePolicy(FactionId.of("other"), Set.of(), Map.of())),
                "mismatched policy faction");
    }

    private static void calculatesBoundedRecruitCapacityWithoutOverflow() {
        FactionDefinition faction = faction(10, 4);
        FactionRuntimePolicy reducedCapacity = new FactionRuntimePolicy(
                faction.id(), Set.of(), Map.of("recruitment_capacity_percent", 90));
        assertEquals(12, FactionBalanceService.effectiveRecruitLimit(faction, reducedCapacity),
                "floor recruit capacity");

        FactionDefinition maximum = faction(Integer.MAX_VALUE, Integer.MAX_VALUE);
        FactionRuntimePolicy maximumCapacity = new FactionRuntimePolicy(
                maximum.id(), Set.of(), Map.of("recruitment_capacity_percent", 500));
        assertEquals(Integer.MAX_VALUE,
                FactionBalanceService.effectiveRecruitLimit(maximum, maximumCapacity),
                "overflow-safe recruit capacity");

        FactionRuntimePolicy disabledCapacity = new FactionRuntimePolicy(
                faction.id(), Set.of(), Map.of("recruitment_capacity_percent", 0));
        assertEquals(0, FactionBalanceService.effectiveRecruitLimit(faction, disabledCapacity),
                "zero recruit capacity");
    }

    private static void calculatesFactionTradePricesWithCeilingRounding() {
        FactionDefinition faction = faction(10, 4);
        FactionRuntimePolicy tradePolicy = new FactionRuntimePolicy(
                faction.id(), Set.of(), Map.of("trade_value_percent", 120));
        FactionBalanceService.ResolvedBalance balance =
                FactionBalanceService.resolve(faction, tradePolicy);

        assertEquals(21, FactionBalanceService.tradeCreditPrice(25, balance),
                "ceil discounted credit price");
        assertEquals(1, FactionBalanceService.tradeCreditPrice(1, balance),
                "minimum credit price");

        FactionBalanceService.ResolvedBalance zeroValue = FactionBalanceService.resolve(
                faction,
                new FactionRuntimePolicy(
                        faction.id(), Set.of(), Map.of("trade_value_percent", 0)));
        assertEquals(1_000_000, FactionBalanceService.tradeCreditPrice(1_000_000, zeroValue),
                "packet-bounded defensive trade price");
    }

    private static void appliesBoundedPercentagesWithoutOverflow() {
        assertEquals(2, FactionBalanceService.applyPercentFloor(3, 75), "floor percentage");
        assertEquals(3, FactionBalanceService.applyPercentCeil(3, 75), "ceiling percentage");
        assertEquals(0, FactionBalanceService.applyPercentFloor(100, -50),
                "negative percentage clamp");
        assertEquals(500, FactionBalanceService.applyPercentCeil(100, 900),
                "maximum percentage clamp");
        assertEquals(Integer.MAX_VALUE,
                FactionBalanceService.applyPercentCeil(Integer.MAX_VALUE, 500),
                "overflow-safe percentage");
    }

    private static FactionDefinition faction(int maximumRecruits, int capacityBonus) {
        FactionStrategyDefinition strategy = new FactionStrategyDefinition(
                "test", capacityBonus, 120, 125, 15, "discipline", "attrition");
        return new FactionDefinition(
                FactionId.of("test"), "Test", 25, 10, maximumRecruits,
                Set.of(), Set.of(), 0, "", 10, 0, 0, strategy);
    }

    private static void assertThrows(
            Class<? extends Throwable> expected, Runnable action, String label
    ) {
        try {
            action.run();
        } catch (Throwable throwable) {
            if (expected.isInstance(throwable)) {
                return;
            }
            throw new AssertionError(label + " expected " + expected.getSimpleName()
                    + " but caught " + throwable, throwable);
        }
        throw new AssertionError(label + " expected " + expected.getSimpleName());
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label + " expected true");
        }
    }
}
