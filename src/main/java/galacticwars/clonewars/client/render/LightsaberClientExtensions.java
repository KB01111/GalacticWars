package galacticwars.clonewars.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class LightsaberClientExtensions implements IClientItemExtensions {
    public static final LightsaberClientExtensions INSTANCE = new LightsaberClientExtensions();

    private LightsaberClientExtensions() {
    }

    @Override
    public HumanoidModel.ArmPose getArmPose(
            LivingEntity entity,
            InteractionHand hand,
            ItemStack stack
    ) {
        return HumanoidModel.ArmPose.ITEM;
    }

    @Override
    public boolean applyForgeHandTransform(
            PoseStack poseStack,
            LocalPlayer player,
            HumanoidArm arm,
            ItemStack stack,
            float partialTick,
            float equipProgress,
            float swingProgress
    ) {
        float direction = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float ready = 1.0F - Mth.clamp(equipProgress, 0.0F, 1.0F);
        float slash = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        float idle = Mth.sin((player.tickCount + partialTick) * 0.14F) * 0.008F * ready;

        // The GeckoLib geometry now pivots through the middle of the hilt. Keep
        // the animation centered there so the hand stays wrapped around the
        // grip instead of orbiting the weapon during a slash.
        poseStack.translate(direction * 0.012F * ready, idle, -0.018F * ready);
        poseStack.mulPose(Axis.ZP.rotationDegrees(direction * (-6.0F * ready - 20.0F * slash)));
        poseStack.mulPose(Axis.XP.rotationDegrees(-3.0F * ready - 10.0F * slash));
        // Returning false preserves Minecraft's normal first-person hand and item
        // transforms after applying the subtle ready/slash offsets above.
        return false;
    }
}
