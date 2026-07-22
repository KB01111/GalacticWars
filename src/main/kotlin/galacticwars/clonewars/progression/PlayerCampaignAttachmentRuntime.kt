package galacticwars.clonewars.progression

import dev.architectury.event.events.common.PlayerEvent
import dev.architectury.event.events.common.TickEvent
import java.util.concurrent.atomic.AtomicBoolean
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player

/**
 * Loader-neutral synchronization boundary for the player campaign attachment.
 *
 * [ProgressionSavedData] and [ForceSavedData] remain authoritative. Platform
 * attachments are immutable, owner-visible projections that are always replaced
 * from SavedData and are never used to reconstruct server state.
 */
object PlayerCampaignAttachmentRuntime {
    const val SYNCHRONIZATION_INTERVAL_TICKS: Int = 20

    private val installed = AtomicBoolean()

    @Volatile
    private var platformAccess: PlayerCampaignAttachmentAccess? = null

    /** Installs exactly one loader adapter and registers the shared lifecycle hooks. */
    @JvmStatic
    fun install(access: PlayerCampaignAttachmentAccess) {
        require(installed.compareAndSet(false, true)) {
            "Player campaign attachment runtime is already installed"
        }
        platformAccess = access

        PlayerEvent.PLAYER_JOIN.register { player -> synchronize(player) }
        PlayerEvent.PLAYER_RESPAWN.register { player, _, _ -> synchronize(player) }
        TickEvent.PLAYER_POST.register { player ->
            if (player is ServerPlayer &&
                player.tickCount % SYNCHRONIZATION_INTERVAL_TICKS == 0
            ) {
                synchronize(player)
            }
        }
    }

    /**
     * Returns the validated local projection for UI consumers on either side.
     * A mismatched persisted UUID is ignored until the server replaces it.
     */
    @JvmStatic
    fun get(player: Player): PlayerCampaignAttachmentState? =
        platformAccess?.get(player)?.takeIf { state -> state.playerId == player.uuid }

    /**
     * Reprojects authoritative state and writes only when the immutable snapshot
     * changed. Returns true when the platform attachment was replaced.
     */
    @JvmStatic
    fun synchronize(player: ServerPlayer): Boolean {
        val access = checkNotNull(platformAccess) {
            "Player campaign attachment runtime has not been installed"
        }
        val level = player.level()
        val playerId = player.uuid
        val authoritativeProgression = ProgressionSavedData.get(level).state(playerId)
        OnboardingAdvancementService.synchronize(player, authoritativeProgression)
        ObjectiveMarkerService.synchronize(player, authoritativeProgression)
        val authoritativeForce = ForceSavedData.get(level).state(playerId)
        val projected = PlayerCampaignAttachmentService.fromAuthoritative(
            authoritativeProgression,
            authoritativeForce,
        )
        check(projected.playerId == playerId) {
            "Authoritative player campaign projection belongs to another player"
        }

        if (access.get(player) == projected) {
            return false
        }
        access.set(player, projected)
        return true
    }
}

/** Platform implementation installed by the active loader entrypoint. */
interface PlayerCampaignAttachmentAccess {
    fun get(player: Player): PlayerCampaignAttachmentState?

    fun set(player: ServerPlayer, state: PlayerCampaignAttachmentState)
}
