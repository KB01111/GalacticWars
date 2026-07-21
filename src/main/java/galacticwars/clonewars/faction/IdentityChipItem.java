package galacticwars.clonewars.faction;

import galacticwars.clonewars.settlement.CommandCenterBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class IdentityChipItem extends Item {
    private final FactionId factionId;

    public IdentityChipItem(FactionId factionId, Properties properties) {
        super(properties);
        this.factionId = factionId;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)
                || !(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(context.getClickedPos()) instanceof CommandCenterBlockEntity hall)
                || !hall.isOwner(player)) {
            player.sendSystemMessage(Component.translatable(
                    "message.galacticwars.identity_chip.hall_required"));
            return InteractionResult.FAIL;
        }
        FactionPledgeService.Result result = FactionPledgeService.pledge(
                player, hall, factionId.toString());
        if (!result.accepted()) {
            player.sendSystemMessage(Component.translatable(
                    "message.galacticwars.faction_selection.rejected." + result.reason()));
            return InteractionResult.FAIL;
        }
        if (!result.changed()) {
            return InteractionResult.SUCCESS;
        }
        player.sendSystemMessage(Component.translatable(
                "message.galacticwars.identity_chip.applied",
                Component.translatable("faction.galacticwars." + factionId.path()),
                result.alignment()));
        return InteractionResult.SUCCESS;
    }
}
