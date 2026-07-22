package galacticwars.clonewars.network;

import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.PlayerEvent;
import galacticwars.clonewars.Config;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Join synchronization and operator-only hot reload for authoritative gameplay policy. */
public final class ServerPolicySync {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();

    private ServerPolicySync() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        PlayerEvent.PLAYER_JOIN.register(ServerPolicySync::send);
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, alive) -> send(player));
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) ->
                dispatcher.register(Commands.literal("galacticwars")
                        .then(Commands.literal("reloadpolicy")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(context -> reload(context.getSource().getServer(),
                                        context.getSource())))));
    }

    public static void send(ServerPlayer player) {
        if (GalacticNetwork.canPlayerReceive(player, ServerPolicyPayload.TYPE)) {
            GalacticNetwork.CHANNEL.sendToPlayer(() -> player, ServerPolicyPayload.current());
        }
    }

    private static int reload(MinecraftServer server, net.minecraft.commands.CommandSourceStack source) {
        if (!Config.reload()) {
            source.sendFailure(Component.translatable("command.galacticwars.policy.reload_failed"));
            return 0;
        }
        server.getPlayerList().getPlayers().forEach(ServerPolicySync::send);
        source.sendSuccess(() -> Component.translatable("command.galacticwars.policy.reloaded"), true);
        return 1;
    }
}
