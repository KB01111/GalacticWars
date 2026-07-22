package galacticwars.clonewars.world;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Idempotently authors the three launch POIs without replacing protected player construction. */
public final class PlanetInfrastructureService {
    private static final int PLATFORM_RADIUS = 3;

    private PlanetInfrastructureService() {
    }

    public static void ensure(
            ServerLevel level,
            LaunchContentDefinitions.PlanetDefinition planet
    ) {
        for (LaunchContentDefinitions.PlanetPoiDefinition poi : planet.pointsOfInterest()) {
            ensurePoi(level, planet, poi);
        }
    }

    private static void ensurePoi(
            ServerLevel level,
            LaunchContentDefinitions.PlanetDefinition planet,
            LaunchContentDefinitions.PlanetPoiDefinition poi
    ) {
        level.getChunk(poi.x() >> 4, poi.z() >> 4);
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, poi.x(), poi.z());
        BlockPos feet = new BlockPos(poi.x(), Math.min(level.getMaxY() - 4, surface + 2), poi.z());
        BlockState floor = surfaceBlock(planet.theme());
        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                BlockPos floorPos = feet.offset(x, -1, z);
                placeIfReplaceable(level, floorPos, floor);
            }
        }
        if (poi.role().equals("economy")) {
            placeIfReplaceable(level, feet, Blocks.BARREL.defaultBlockState());
            placeIfReplaceable(level, feet.offset(2, 0, 2), Blocks.LANTERN.defaultBlockState());
            placeIfReplaceable(level, feet.offset(-2, 0, -2), Blocks.LANTERN.defaultBlockState());
        } else if (poi.role().equals("arrival")) {
            placeIfReplaceable(level, feet.offset(2, 0, 2), Blocks.SEA_LANTERN.defaultBlockState());
            placeIfReplaceable(level, feet.offset(-2, 0, -2), Blocks.SEA_LANTERN.defaultBlockState());
        }
    }

    private static void placeIfReplaceable(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockState(pos).canBeReplaced() || !level.getFluidState(pos).isEmpty()) {
            level.setBlockAndUpdate(pos, state);
        }
    }

    private static BlockState surfaceBlock(String theme) {
        return switch (theme) {
            case "desert" -> ModBlocks.TATOOINE_SAND.get().defaultBlockState();
            case "canyon" -> ModBlocks.GEONOSIS_ROCK.get().defaultBlockState();
            case "storm_ocean" -> ModBlocks.KAMINO_PANEL.get().defaultBlockState();
            case "city" -> ModBlocks.CORUSCANT_PANEL.get().defaultBlockState();
            default -> ModBlocks.DURACRETE.get().defaultBlockState();
        };
    }
}
