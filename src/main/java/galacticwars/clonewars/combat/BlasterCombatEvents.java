package galacticwars.clonewars.combat;

import galacticwars.clonewars.Config;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.faction.FactionRelation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

/** Loader-neutral projectile deflection and faction friendly-fire policy. */
public final class BlasterCombatEvents {
    private BlasterCombatEvents() {
    }

    /**
     * Handles a projectile collision before vanilla damage is applied.
     *
     * @return {@code true} when the collision was consumed by deflection or
     *         friendly-fire protection
     */
    public static boolean handleProjectileImpact(AbstractArrow projectile, Entity target) {
        if (projectile instanceof BlasterBoltEntity bolt
                && target instanceof LivingEntity defender
                && LightsaberDeflectionService.tryDeflect(
                        bolt, defender, defender.level().getGameTime())) {
            return true;
        }
        if (!FactionRangedWeaponService.isProtectedFactionProjectile(projectile.getWeaponItem())
                || !(projectile.getOwner() instanceof LivingEntity shooter)) {
            return false;
        }
        boolean blocked = target instanceof Player player
                ? blocksPlayerHit(shooter, player)
                : target instanceof GalacticRecruitEntity recruit && blocksRecruitHit(shooter, recruit);
        if (blocked) {
            projectile.discard();
        }
        return blocked;
    }

    /** Final cross-loader damage guard for vanilla arrow implementations. */
    public static boolean blocksIncomingDamage(LivingEntity target, DamageSource source) {
        return source.getDirectEntity() instanceof AbstractArrow arrow
                && handleProjectileImpact(arrow, target);
    }

    private static boolean blocksPlayerHit(LivingEntity shooter, Player player) {
        if (shooter instanceof ServerPlayer) {
            return BlasterFriendlyFirePolicy.blocksHit(
                    true, false, FactionRelation.NEUTRAL,
                    Config.ALLOW_BLASTER_FRIENDLY_FIRE.getAsBoolean(),
                    Config.ALLOW_BLASTER_PVP.getAsBoolean());
        }
        if (!(shooter instanceof GalacticRecruitEntity recruit)) {
            return true;
        }
        return BlasterFriendlyFirePolicy.blocksRecruitHitOnPlayer(
                recruit.isOwnedBy(player),
                recruit.factionRelationTo(player),
                Config.ALLOW_BLASTER_PVP.getAsBoolean());
    }

    private static boolean blocksRecruitHit(LivingEntity shooter, GalacticRecruitEntity recruit) {
        boolean sameOwner = shooter instanceof ServerPlayer player && recruit.isOwnedBy(player)
                || shooter instanceof GalacticRecruitEntity other && sameOwner(other, recruit);
        FactionRelation relation = relation(shooter, recruit);
        return BlasterFriendlyFirePolicy.blocksHit(false, sameOwner, relation,
                Config.ALLOW_BLASTER_FRIENDLY_FIRE.getAsBoolean(),
                Config.ALLOW_BLASTER_PVP.getAsBoolean());
    }

    private static FactionRelation relation(LivingEntity shooter, GalacticRecruitEntity recruit) {
        if (shooter instanceof GalacticRecruitEntity other) {
            return other.factionRelationTo(recruit);
        }
        return shooter instanceof ServerPlayer player
                ? recruit.factionRelationTo(player)
                : FactionRelation.NEUTRAL;
    }

    private static boolean sameOwner(GalacticRecruitEntity first, GalacticRecruitEntity second) {
        LivingEntity firstOwner = first.getOwner();
        LivingEntity secondOwner = second.getOwner();
        return firstOwner != null && secondOwner != null && firstOwner.getUUID().equals(secondOwner.getUUID());
    }
}
