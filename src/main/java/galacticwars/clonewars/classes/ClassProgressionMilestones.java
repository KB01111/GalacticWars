package galacticwars.clonewars.classes;

/** Shared, deterministic launch milestones for non-Force player classes. */
public final class ClassProgressionMilestones {
    public static final int SECONDARY_ABILITY_RANK = 3;
    public static final int RESOURCE_EFFICIENCY_RANK = 5;
    public static final int COOLDOWN_EFFICIENCY_RANK = 7;
    public static final int MASTER_RANK = 10;

    private ClassProgressionMilestones() {
    }

    public static int requiredRankForAbilityIndex(int index) {
        return index <= 0 ? 1 : SECONDARY_ABILITY_RANK;
    }

    public static int resourceCost(int baseCost, int rank) {
        double multiplier = rank >= MASTER_RANK ? 0.80D
                : rank >= RESOURCE_EFFICIENCY_RANK ? 0.90D : 1.0D;
        return Math.max(baseCost == 0 ? 0 : 1, (int) Math.ceil(baseCost * multiplier));
    }

    public static int cooldownTicks(int baseTicks, int rank) {
        double multiplier = rank >= MASTER_RANK ? 0.75D
                : rank >= COOLDOWN_EFFICIENCY_RANK ? 0.85D : 1.0D;
        return Math.max(1, (int) Math.ceil(baseTicks * multiplier));
    }

    public static double potency(int rank) {
        return rank >= MASTER_RANK ? 1.25D : 1.0D;
    }
}
