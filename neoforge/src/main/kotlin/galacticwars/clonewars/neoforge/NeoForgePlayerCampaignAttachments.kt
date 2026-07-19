package galacticwars.clonewars.neoforge

import com.mojang.serialization.Codec
import galacticwars.clonewars.GalacticWars
import galacticwars.clonewars.progression.PlayerCampaignAttachmentAccess
import galacticwars.clonewars.progression.PlayerCampaignAttachmentRuntime
import galacticwars.clonewars.progression.PlayerCampaignAttachmentState
import galacticwars.clonewars.survival.MountFiberAttachmentAccess
import galacticwars.clonewars.survival.MountFiberAttachmentRuntime
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Function
import java.util.function.Supplier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.animal.equine.AbstractHorse
import net.minecraft.world.entity.player.Player
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.attachment.IAttachmentHolder
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import net.neoforged.neoforge.registries.NeoForgeRegistries

/** NeoForge AttachmentType registration and immutable player attachment access. */
object NeoForgePlayerCampaignAttachments {
    private val registered = AtomicBoolean()
    private val attachments: DeferredRegister<AttachmentType<*>> =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, GalacticWars.MODID)

    private val campaignState: DeferredHolder<AttachmentType<*>, AttachmentType<PlayerCampaignAttachmentState>> =
        attachments.register("player_campaign_state", Supplier {
            AttachmentType.builder(Function { holder: IAttachmentHolder ->
                val player = holder as? Player
                    ?: throw IllegalArgumentException(
                        "Player campaign attachments can only be created for players",
                    )
                PlayerCampaignAttachmentState.initial(player.uuid)
            })
                .serialize(PlayerCampaignAttachmentState.CODEC.fieldOf("state"))
                .copyOnDeath()
                .sync(
                    { holder, recipient -> holder === recipient },
                    PlayerCampaignAttachmentState.STREAM_CODEC,
                )
                .build()
        })

    private val lastBrushedDay: DeferredHolder<AttachmentType<*>, AttachmentType<Long>> =
        attachments.register("last_brushed_day", Supplier {
            AttachmentType.builder(Supplier { Long.MIN_VALUE })
                .serialize(Codec.LONG.fieldOf("day"))
                .build()
        })

    @JvmStatic
    fun register(modEventBus: IEventBus) {
        require(registered.compareAndSet(false, true)) {
            "NeoForge player campaign attachment is already registered"
        }
        attachments.register(modEventBus)
        PlayerCampaignAttachmentRuntime.install(NeoForgeAccess)
        MountFiberAttachmentRuntime.install(NeoForgeAccess)
    }

    private object NeoForgeAccess : PlayerCampaignAttachmentAccess, MountFiberAttachmentAccess {
        override fun get(player: Player): PlayerCampaignAttachmentState? =
            (player as IAttachmentHolder).getExistingDataOrNull(campaignState.get())

        override fun set(player: ServerPlayer, state: PlayerCampaignAttachmentState) {
            require(state.playerId == player.uuid) {
                "Cannot attach another player's campaign state"
            }
            (player as IAttachmentHolder).setData(campaignState.get(), state)
        }

        override fun getLastBrushedDay(horse: AbstractHorse): Long? =
            (horse as IAttachmentHolder).getExistingDataOrNull(lastBrushedDay.get())

        override fun setLastBrushedDay(horse: AbstractHorse, day: Long) {
            require(day >= 0L) { "Mount brushed day cannot be negative" }
            (horse as IAttachmentHolder).setData(lastBrushedDay.get(), day)
        }
    }
}
