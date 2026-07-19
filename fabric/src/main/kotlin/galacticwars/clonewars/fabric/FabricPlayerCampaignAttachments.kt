package galacticwars.clonewars.fabric

import galacticwars.clonewars.GalacticWars
import galacticwars.clonewars.progression.PlayerCampaignAttachmentAccess
import galacticwars.clonewars.progression.PlayerCampaignAttachmentRuntime
import galacticwars.clonewars.progression.PlayerCampaignAttachmentState
import galacticwars.clonewars.survival.MountFiberAttachmentAccess
import galacticwars.clonewars.survival.MountFiberAttachmentRuntime
import java.util.concurrent.atomic.AtomicBoolean
import com.mojang.serialization.Codec
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget
import net.fabricmc.fabric.api.attachment.v1.AttachmentType
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.animal.equine.AbstractHorse
import net.minecraft.world.entity.player.Player

/** Fabric Attachment API registration and immutable player attachment access. */
object FabricPlayerCampaignAttachments {
    private val registered = AtomicBoolean()
    private lateinit var campaignState: AttachmentType<PlayerCampaignAttachmentState>
    private lateinit var lastBrushedDay: AttachmentType<Long>

    @JvmStatic
    fun register() {
        require(registered.compareAndSet(false, true)) {
            "Fabric player campaign attachment is already registered"
        }
        campaignState = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "player_campaign_state"),
        ) { builder ->
            builder
                .persistent(PlayerCampaignAttachmentState.CODEC)
                .copyOnDeath()
                .syncWith(
                    PlayerCampaignAttachmentState.STREAM_CODEC,
                    AttachmentSyncPredicate.targetOnly(),
                )
        }
        lastBrushedDay = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "last_brushed_day"),
        ) { builder ->
            builder.persistent(Codec.LONG)
        }
        PlayerCampaignAttachmentRuntime.install(FabricAccess)
        MountFiberAttachmentRuntime.install(FabricAccess)
    }

    private object FabricAccess : PlayerCampaignAttachmentAccess, MountFiberAttachmentAccess {
        override fun get(player: Player): PlayerCampaignAttachmentState? =
            (player as AttachmentTarget).getAttached(campaignState)

        override fun set(player: ServerPlayer, state: PlayerCampaignAttachmentState) {
            require(state.playerId == player.uuid) {
                "Cannot attach another player's campaign state"
            }
            (player as AttachmentTarget).setAttached(campaignState, state)
        }

        override fun getLastBrushedDay(horse: AbstractHorse): Long? =
            (horse as AttachmentTarget).getAttached(lastBrushedDay)

        override fun setLastBrushedDay(horse: AbstractHorse, day: Long) {
            require(day >= 0L) { "Mount brushed day cannot be negative" }
            (horse as AttachmentTarget).setAttached(lastBrushedDay, day)
        }
    }
}
