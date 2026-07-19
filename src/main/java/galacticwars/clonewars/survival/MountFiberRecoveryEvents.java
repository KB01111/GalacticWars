package galacticwars.clonewars.survival;

import galacticwars.clonewars.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Optional;

/** Loader-neutral brush interaction backed by a platform attachment. */
public final class MountFiberRecoveryEvents {
    private MountFiberRecoveryEvents() {
    }

    public static Optional<InteractionResult> onHorseBrushed(
            net.minecraft.world.entity.player.Player interactingPlayer,
            Entity target,
            InteractionHand hand
    ) {
        if (!(interactingPlayer instanceof ServerPlayer player)
                || !(target instanceof AbstractHorse horse)
                || horse.isBaby()) {
            return Optional.empty();
        }
        ItemStack brush = player.getItemInHand(hand);
        if (!brush.is(Items.BRUSH)) {
            return Optional.empty();
        }
        long day = horse.level().getGameTime() / 24000L;
        if (!MountFiberAttachmentRuntime.tryMarkBrushed(horse, day)) {
            player.sendSystemMessage(Component.translatable(
                    "message.galacticwars.fiber.already_brushed"));
            return Optional.of(InteractionResult.FAIL);
        }
        ItemStack hair = new ItemStack(ModItems.MANDALORIAN_FIBER.get());
        if (!player.addItem(hair)) {
            player.drop(hair, false);
        }
        brush.hurtAndBreak(1, player, hand);
        return Optional.of(InteractionResult.SUCCESS);
    }
}
