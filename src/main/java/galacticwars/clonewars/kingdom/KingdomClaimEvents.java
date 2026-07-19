package galacticwars.clonewars.kingdom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/** Loader-neutral claim permission checks used by the shared event bridge. */
public final class KingdomClaimEvents {
    private KingdomClaimEvents() {
    }

    public static boolean allowBreak(Level eventLevel, ServerPlayer player, BlockPos pos) {
        return !(eventLevel instanceof ServerLevel level)
                || allows(level, player, pos.getX(), pos.getZ(), KingdomPermission.BUILD);
    }

    public static boolean allowPlace(Level eventLevel, Entity entity, BlockPos pos) {
        return !(eventLevel instanceof ServerLevel level)
                || !(entity instanceof Player player)
                || allows(level, player, pos.getX(), pos.getZ(), KingdomPermission.BUILD);
    }

    public static boolean allowStorageInteraction(Player player, BlockPos pos) {
        return !(player.level() instanceof ServerLevel level)
                || !(level.getBlockEntity(pos) instanceof Container)
                || allows(level, player, pos.getX(), pos.getZ(), KingdomPermission.USE_STORAGE);
    }

    private static boolean allows(
            ServerLevel level,
            Player player,
            int blockX,
            int blockZ,
            KingdomPermission permission
    ) {
        if (player instanceof ServerPlayer serverPlayer
                && level.getServer().getPlayerList().isOp(serverPlayer.nameAndId())) {
            return true;
        }
        String dimensionId = level.dimension().identifier().toString();
        KingdomSavedData data = KingdomSavedData.get(level);
        var claim = data.claimAt(dimensionId, new ChunkPos(blockX >> 4, blockZ >> 4));
        if (claim.isEmpty()) {
            return true;
        }
        if (player == null) {
            return false;
        }
        return data.kingdom(claim.orElseThrow().kingdomId())
                .map(kingdom -> kingdom.allows(player.getUUID(), permission))
                .orElse(false);
    }
}
