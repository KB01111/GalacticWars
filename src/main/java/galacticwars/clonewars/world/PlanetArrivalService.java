package galacticwars.clonewars.world;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;

public final class PlanetArrivalService {
    private static final int PLATFORM_RADIUS = 2;
    private static final int HOME_SEARCH_RADIUS = 24;
    private static final int[] HOME_VERTICAL_OFFSETS = {0, 1, -1, 2, -2, 3, -3};
    private static final List<BlockPos> CANDIDATE_OFFSETS = List.of(
            BlockPos.ZERO,
            new BlockPos(32, 0, 0),
            new BlockPos(-32, 0, 0),
            new BlockPos(0, 0, 32),
            new BlockPos(0, 0, -32),
            new BlockPos(32, 0, 32),
            new BlockPos(-32, 0, -32),
            new BlockPos(32, 0, -32),
            new BlockPos(-32, 0, 32));

    private PlanetArrivalService() {
    }

    public static Optional<BlockPos> findOrCreate(ServerLevel level) {
        return findOrCreateAt(level, 0, 0);
    }

    public static Optional<BlockPos> findOrCreate(
            ServerLevel level,
            LaunchContentDefinitions.PlanetDefinition planet,
            java.util.Collection<LaunchContentDefinitions.ConquestRegionDefinition> regions
    ) {
        ArrivalProfile profile = ArrivalProfile.byId(planet.arrival());
        LaunchContentDefinitions.ConquestRegionDefinition region = regions.stream()
                .filter(candidate -> candidate.planetId().equals(planet.id()))
                .sorted(java.util.Comparator.comparing(
                        LaunchContentDefinitions.ConquestRegionDefinition::id))
                .findFirst()
                .orElse(null);
        int centerX = 0;
        int centerZ = 0;
        if (region != null) {
            int clearance = Math.addExact(region.protectedRadius(), 32);
            centerX = Math.addExact(region.landmarkX(), profile.offsetX() * clearance);
            centerZ = Math.addExact(region.landmarkZ(), profile.offsetZ() * clearance);
        }
        return findOrCreateAt(level, centerX, centerZ);
    }

    private static Optional<BlockPos> findOrCreateAt(ServerLevel level, int centerX, int centerZ) {
        for (BlockPos offset : CANDIDATE_OFFSETS) {
            Optional<BlockPos> existing = findExistingPlatform(
                    level, centerX + offset.getX(), centerZ + offset.getZ());
            if (existing.isPresent()) {
                return existing;
            }
        }
        for (BlockPos offset : CANDIDATE_OFFSETS) {
            Optional<BlockPos> created = createPlatform(
                    level, centerX + offset.getX(), centerZ + offset.getZ());
            if (created.isPresent()) {
                return created;
            }
        }
        return Optional.empty();
    }

    public static Optional<BlockPos> findHomeArrival(
            ServerLevel level,
            BlockPos commandCenter,
            Entity traveler
    ) {
        for (int radius = 2; radius <= HOME_SEARCH_RADIUS; radius++) {
            for (int yOffset : HOME_VERTICAL_OFFSETS) {
                for (int x = -radius; x <= radius; x++) {
                    Optional<BlockPos> north = safeHomeCandidate(
                            level, commandCenter.offset(x, yOffset, -radius), traveler);
                    if (north.isPresent()) {
                        return north;
                    }
                    Optional<BlockPos> south = safeHomeCandidate(
                            level, commandCenter.offset(x, yOffset, radius), traveler);
                    if (south.isPresent()) {
                        return south;
                    }
                }
                for (int z = -radius + 1; z < radius; z++) {
                    Optional<BlockPos> west = safeHomeCandidate(
                            level, commandCenter.offset(-radius, yOffset, z), traveler);
                    if (west.isPresent()) {
                        return west;
                    }
                    Optional<BlockPos> east = safeHomeCandidate(
                            level, commandCenter.offset(radius, yOffset, z), traveler);
                    if (east.isPresent()) {
                        return east;
                    }
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> findExistingPlatform(ServerLevel level, int centerX, int centerZ) {
        if (!unclaimedPlatform(level, centerX, centerZ)) {
            return Optional.empty();
        }
        for (int feetY = level.getMinY() + 2; feetY < level.getMaxY() - 2; feetY++) {
            BlockPos feet = new BlockPos(centerX, feetY, centerZ);
            if (isPlatform(level, feet) && hasClearArrivalVolume(level, feet)) {
                return Optional.of(feet);
            }
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> createPlatform(ServerLevel level, int centerX, int centerZ) {
        if (!unclaimedPlatform(level, centerX, centerZ)) {
            return Optional.empty();
        }
        level.getChunk(centerX >> 4, centerZ >> 4);
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerX, centerZ);
        BlockPos feet = new BlockPos(centerX, Math.min(level.getMaxY() - 3, surfaceY + 3), centerZ);
        if (!canCreatePlatform(level, feet)) {
            return Optional.empty();
        }
        BlockState platform = ModBlocks.DURACRETE.get().defaultBlockState();
        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                level.setBlockAndUpdate(feet.offset(x, -1, z), platform);
            }
        }
        return Optional.of(feet);
    }

    private static boolean canCreatePlatform(ServerLevel level, BlockPos feet) {
        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                if (!replaceable(level.getBlockState(feet.offset(x, -1, z)))) {
                    return false;
                }
            }
        }
        return hasClearArrivalVolume(level, feet);
    }

    private static boolean isPlatform(ServerLevel level, BlockPos feet) {
        for (int x = -PLATFORM_RADIUS; x <= PLATFORM_RADIUS; x++) {
            for (int z = -PLATFORM_RADIUS; z <= PLATFORM_RADIUS; z++) {
                if (!level.getBlockState(feet.offset(x, -1, z)).is(ModBlocks.DURACRETE.get())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean hasClearArrivalVolume(ServerLevel level, BlockPos feet) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (!clearForPlayer(level.getBlockState(feet.offset(x, 0, z)))
                        || !clearForPlayer(level.getBlockState(feet.offset(x, 1, z)))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean replaceable(BlockState state) {
        return state.canBeReplaced() || !state.getFluidState().isEmpty();
    }

    private static boolean clearForPlayer(BlockState state) {
        return state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    private static Optional<BlockPos> safeHomeCandidate(
            ServerLevel level,
            BlockPos feet,
            Entity traveler
    ) {
        level.getChunkAt(feet);
        BlockPos floor = feet.below();
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                || !level.getFluidState(feet).isEmpty()
                || !level.getFluidState(feet.above()).isEmpty()) {
            return Optional.empty();
        }
        AABB current = traveler.getBoundingBox();
        AABB destination = current.move(
                feet.getX() + 0.5D - traveler.getX(),
                feet.getY() - traveler.getY(),
                feet.getZ() + 0.5D - traveler.getZ());
        return level.noCollision(traveler, destination) ? Optional.of(feet) : Optional.empty();
    }

    private static boolean unclaimedPlatform(ServerLevel level, int centerX, int centerZ) {
        KingdomSavedData kingdoms = KingdomSavedData.get(level);
        String dimensionId = level.dimension().identifier().toString();
        int minimumChunkX = (centerX - PLATFORM_RADIUS) >> 4;
        int maximumChunkX = (centerX + PLATFORM_RADIUS) >> 4;
        int minimumChunkZ = (centerZ - PLATFORM_RADIUS) >> 4;
        int maximumChunkZ = (centerZ + PLATFORM_RADIUS) >> 4;
        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                if (kingdoms.claimAt(
                        dimensionId, new net.minecraft.world.level.ChunkPos(chunkX, chunkZ)).isPresent()) {
                    return false;
                }
            }
        }
        return true;
    }

    public enum ArrivalProfile {
        SPACEPORT("spaceport", 1, 0),
        FOUNDRY_OUTPOST("foundry_outpost", -1, 0),
        PLATFORM_CITY("platform_city", 0, 1),
        SENATE_DISTRICT("senate_district", 0, -1);

        private final String id;
        private final int offsetX;
        private final int offsetZ;

        ArrivalProfile(String id, int offsetX, int offsetZ) {
            this.id = id;
            this.offsetX = offsetX;
            this.offsetZ = offsetZ;
        }

        public int offsetX() {
            return offsetX;
        }

        public int offsetZ() {
            return offsetZ;
        }

        public static ArrivalProfile byId(String id) {
            for (ArrivalProfile profile : values()) {
                if (profile.id.equals(id)) {
                    return profile;
                }
            }
            throw new IllegalArgumentException("Unknown planet arrival profile " + id);
        }
    }
}
