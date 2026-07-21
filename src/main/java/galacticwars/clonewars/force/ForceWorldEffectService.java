package galacticwars.clonewars.force;

import galacticwars.clonewars.Config;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.data.CoreContentBindings;
import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.faction.FactionRelation;
import galacticwars.clonewars.progression.ForceAbilityRuntimeService;
import galacticwars.clonewars.progression.ForceSavedData;
import galacticwars.clonewars.progression.ForceRuntimeState;
import galacticwars.clonewars.progression.GalacticSystemsService;
import galacticwars.clonewars.progression.LaunchContentCatalog;
import galacticwars.clonewars.progression.ProgressionDecision;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.progression.ProgressionEvent;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionState;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import galacticwars.clonewars.network.ForceHudPayload;
import galacticwars.clonewars.network.GalacticNetwork;

public final class ForceWorldEffectService {
    private ForceWorldEffectService() {
    }

    public static boolean activate(ServerPlayer player, UUID activationId, int slot) {
        if (slot < 0 || slot > 2 || !(player.level() instanceof ServerLevel level)) return false;
        ProgressionSavedData progressionData = ProgressionSavedData.get(level);
        ProgressionState progression = progressionData.state(player.getUUID());
        String faction = path(progression.factionId());
        String abilityId = abilityFor(faction, slot);
        LaunchContentDefinitions content = LaunchContentCatalog.data();
        LaunchContentDefinitions.ForceAbilityDefinition ability = content.forceAbilities().get(abilityId);
        if (ability == null) return fail(player, "unknown_force_ability");
        LivingEntity target = requiresTarget(ability.effect()) ? target(player, ability.range()) : null;
        if (requiresTarget(ability.effect()) && target == null) return fail(player, "force_target_required");
        boolean targetsPlayer = target instanceof Player;
        ForceSavedData forceData = ForceSavedData.get(level);
        ForceRuntimeState forceBefore = forceData.state(player.getUUID());
        boolean forceWasStored = forceData.hasStoredState(player.getUUID());
        ForceAbilityRuntimeService.ActivationDecision decision = ForceAbilityRuntimeService.activate(
                progression, forceBefore, activationId, abilityId, level.getGameTime(), targetsPlayer,
                Config.ALLOW_FORCE_PVP.getAsBoolean(), content);
        if (!decision.accepted()) {
            sendSnapshot(player, decision.state(), faction, level.getGameTime());
            return fail(player, decision.reason());
        }
        if (decision.reason().equals("duplicate_activation")) {
            sendSnapshot(player, decision.state(), faction, level.getGameTime());
            return true;
        }
        UUID progressionId = UUID.nameUUIDFromBytes(("force:first-use:" + player.getUUID()
                + ":" + abilityId).getBytes(StandardCharsets.UTF_8));
        GalacticSystemsService.SystemDecision progressionEvaluation =
                GalacticSystemsService.useForceAbility(
                        progression, progressionId, abilityId, content);
        if (!progressionEvaluation.accepted()) {
            return fail(player, progressionEvaluation.reason());
        }
        ProgressionEvent progressionEvent = new ProgressionEvent(
                progressionId, player.getUUID(), ProgressionEventType.FORCE_ABILITY_USED,
                abilityId, 1);
        boolean progressionWasStored = progressionData.hasStoredState(player.getUUID());
        ProgressionState progressionAfter = progression;
        boolean progressionCommitted = false;
        boolean forceCommitted = false;
        try {
            if (progressionEvaluation.changed()) {
                ProgressionDecision committed = progressionData.commitEvaluated(
                        progressionEvent,
                        progression,
                        new ProgressionDecision(
                                true, true, progressionEvaluation.reason(),
                                progressionEvaluation.state()));
                if (!committed.accepted()
                        || (!committed.changed() && !committed.state().processed(progressionId))) {
                    throw new IllegalStateException("Force progression state changed during activation");
                }
                progressionAfter = committed.state();
                progressionCommitted = true;
            }
            if (!forceData.commitEvaluated(player.getUUID(), forceBefore, decision)) {
                throw new IllegalStateException("Force runtime state changed during activation");
            }
            forceCommitted = true;
            apply(player, target, ability);
        } catch (RuntimeException failure) {
            if (forceCommitted) {
                forceData.restoreAfterFailedTransaction(
                        player.getUUID(), decision.state(), forceBefore, forceWasStored);
            }
            if (progressionCommitted) {
                progressionData.restoreAfterFailedTransaction(
                        player.getUUID(), progressionAfter, progression, progressionWasStored);
            }
            sendSnapshot(player, forceData.state(player.getUUID()), faction, level.getGameTime());
            GalacticWars.LOGGER.error("Force activation transaction failed for {} using {}",
                    player.getGameProfile().name(), abilityId, failure);
            return fail(player, "transaction_failed");
        }
        sendSnapshot(player, decision.state(), faction, level.getGameTime());
        level.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.PLAYERS, 0.8F, ability.path().equals("dark") ? 0.7F : 1.2F);
        player.sendOverlayMessage(Component.translatable(
                "message.galacticwars.force.activated", abilityId, decision.state().energy()));
        return true;
    }

    private static void sendSnapshot(
            ServerPlayer player, galacticwars.clonewars.progression.ForceRuntimeState state,
            String faction, long gameTime
    ) {
        if (!GalacticNetwork.canPlayerReceive(player, ForceHudPayload.TYPE)) {
            return;
        }
        var abilities = CoreContentBindings.forceSlots(forcePath(faction));
        int[] cooldowns = new int[3];
        for (int index = 0; index < Math.min(abilities.size(), cooldowns.length); index++) {
            cooldowns[index] = (int) Math.max(0L,
                    Math.min(Integer.MAX_VALUE,
                            state.cooldownEnds().getOrDefault(abilities.get(index), 0L) - gameTime));
        }
        GalacticNetwork.CHANNEL.sendToPlayer(() -> player,
                new ForceHudPayload(state.energy(), cooldowns[0], cooldowns[1], cooldowns[2]));
    }

    public static void syncSnapshot(ServerPlayer player, ForceSavedData forceData) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        var progression = ProgressionSavedData.get(level).state(player.getUUID());
        String faction = path(progression.factionId());
        if (!faction.equals("republic") && !faction.equals("nightsister")) {
            return;
        }
        sendSnapshot(player, forceData.state(player.getUUID()), faction, level.getGameTime());
    }

    private static void apply(
            ServerPlayer player, LivingEntity target,
            LaunchContentDefinitions.ForceAbilityDefinition ability
    ) {
        Vec3 look = player.getLookAngle().normalize();
        switch (ability.effect()) {
            case "push" -> target.push(look.x * 1.45D, 0.35D, look.z * 1.45D);
            case "pull" -> {
                Vec3 direction = player.position().subtract(target.position()).normalize();
                target.push(direction.x * 1.25D, 0.22D, direction.z * 1.25D);
            }
            case "leap" -> player.push(look.x * 0.9D, 0.85D, look.z * 0.9D);
            case "dash" -> player.push(look.x * 1.6D, Math.max(0.05D, look.y * 0.3D), look.z * 1.6D);
            case "choke" -> {
                target.addEffect(new MobEffectInstance(
                        MobEffects.LEVITATION, ability.durationTicks(), 0), player);
                target.addEffect(new MobEffectInstance(
                        MobEffects.WEAKNESS, ability.durationTicks(), 1), player);
            }
            default -> throw new IllegalStateException("Missing Force effect " + ability.effect());
        }
    }

    private static LivingEntity target(ServerPlayer player, double range) {
        double boundedRange = Math.max(0.0D, Math.min(range, 64.0D));
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        return player.level().getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(boundedRange),
                        entity -> entity != player && entity.isAlive()
                                && entity.distanceToSqr(player) <= boundedRange * boundedRange
                                && player.hasLineOfSight(entity)
                                && canAffect(player, entity))
                .stream()
                .filter(entity -> {
                    Vec3 direction = entity.getEyePosition().subtract(eye);
                    return direction.lengthSqr() > 0.01D
                            && direction.normalize().dot(look) >= 0.94D;
                })
                .min(Comparator.comparingDouble(player::distanceToSqr))
                .orElse(null);
    }

    private static boolean canAffect(ServerPlayer player, LivingEntity target) {
        if (!(target instanceof GalacticRecruitEntity recruit)) {
            return true;
        }
        FactionRelation relation = recruit.factionRelationTo(player);
        return !recruit.isOwnedBy(player)
                && relation != FactionRelation.SAME
                && relation != FactionRelation.ALLY;
    }

    private static boolean fail(ServerPlayer player, String reason) {
        player.sendOverlayMessage(Component.translatable(
                "message.galacticwars.force.failed", reason));
        return false;
    }

    private static boolean requiresTarget(String effect) {
        return effect.equals("push") || effect.equals("pull") || effect.equals("choke");
    }

    private static String abilityFor(String faction, int slot) {
        var abilities = CoreContentBindings.forceSlots(forcePath(faction));
        return slot >= 0 && slot < abilities.size() ? abilities.get(slot) : "";
    }

    private static String forcePath(String faction) {
        return faction.equals("republic") ? "light" : faction.equals("nightsister") ? "dark" : "";
    }

    private static String path(String id) {
        int separator = id.indexOf(':');
        return separator < 0 ? id : id.substring(separator + 1);
    }
}
