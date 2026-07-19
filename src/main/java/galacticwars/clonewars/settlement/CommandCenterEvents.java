package galacticwars.clonewars.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Loader-neutral ownership guard for command-center destruction. */
public final class CommandCenterEvents {
    private CommandCenterEvents() {
    }

    public static boolean allowBlockBreak(Level eventLevel, BlockPos pos, ServerPlayer player) {
        if (!(eventLevel instanceof ServerLevel level)
                || !(level.getBlockEntity(pos) instanceof CommandCenterBlockEntity hall)) {
            return true;
        }
        if (hall.ownerId() != null && !hall.isOwner(player)) {
            player.sendSystemMessage(
                    Component.translatable("message.galacticwars.command_center.not_owner"));
            return false;
        }
        return true;
    }
}
