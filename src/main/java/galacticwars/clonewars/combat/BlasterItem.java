package galacticwars.clonewars.combat;

import galacticwars.clonewars.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public final class BlasterItem extends Item {
    private final double damage;
    private final float velocity;
    private final float inaccuracy;

    public BlasterItem(Properties properties, double damage, float velocity, float inaccuracy) {
        super(properties.stacksTo(1));
        if (damage <= 0.0D || velocity <= 0.0F || inaccuracy < 0.0F) {
            throw new IllegalArgumentException("Invalid blaster tuning");
        }
        this.damage = damage;
        this.velocity = velocity;
        this.inaccuracy = inaccuracy;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack weapon = player.getItemInHand(hand);
        if (!player.hasInfiniteMaterials() && !consumeEnergyCell(player)) {
            return InteractionResult.FAIL;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 0.65F,
                1.45F + level.getRandom().nextFloat() * 0.15F);
        if (level instanceof ServerLevel serverLevel) {
            Arrow bolt = new Arrow(level, player, new ItemStack(ModItems.ENERGY_CELL.get()), weapon);
            bolt.pickup = AbstractArrow.Pickup.DISALLOWED;
            bolt.setBaseDamage(damage);
            bolt.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, velocity, inaccuracy);
            serverLevel.addFreshEntity(bolt);
            weapon.hurtAndBreak(1, player, hand);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(weapon, 6);
        return InteractionResult.SUCCESS;
    }

    public void fireAt(ServerLevel level, LivingEntity shooter, LivingEntity target, ItemStack weapon) {
        level.playSound(null, shooter.getX(), shooter.getY(), shooter.getZ(),
                SoundEvents.CROSSBOW_SHOOT, SoundSource.NEUTRAL, 0.65F,
                1.45F + level.getRandom().nextFloat() * 0.15F);
        Arrow bolt = new Arrow(level, shooter, new ItemStack(ModItems.ENERGY_CELL.get()), weapon);
        bolt.pickup = AbstractArrow.Pickup.DISALLOWED;
        bolt.setBaseDamage(damage);
        double targetY = target.getY() + target.getBbHeight() * 0.6D;
        bolt.shoot(target.getX() - shooter.getX(), targetY - bolt.getY(), target.getZ() - shooter.getZ(),
                velocity, inaccuracy);
        level.addFreshEntity(bolt);
    }

    private static boolean consumeEnergyCell(Player player) {
        List<ItemStack> inventory = player.getInventory().getNonEquipmentItems();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stack = inventory.get(slot);
            if (!stack.is(ModItems.ENERGY_CELL.get())) {
                continue;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                inventory.set(slot, ItemStack.EMPTY);
            }
            player.getInventory().setChanged();
            return true;
        }
        return false;
    }
}
