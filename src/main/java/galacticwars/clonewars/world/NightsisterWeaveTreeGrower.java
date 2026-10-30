package galacticwars.clonewars.world;

import galacticwars.clonewars.GalacticWars;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.Feature;

public final class NightsisterWeaveTreeGrower {
    public static final ResourceKey<Feature> NIGHTSISTER_WEAVE_TREE = ResourceKey.create(
            Registries.FEATURE,
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "nightsister_weave_tree"));
    public static final TreeGrower INSTANCE = new TreeGrower(
            "galacticwars:nightsister_weave",
            WeightedList.of(NIGHTSISTER_WEAVE_TREE),
            WeightedList.of(),
            WeightedList.of(),
            NIGHTSISTER_WEAVE_TREE);

    private NightsisterWeaveTreeGrower() {
    }
}
