package galacticwars.clonewars.network;

import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import galacticwars.clonewars.data.GameplayDataManager;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Sends the current bounded gameplay UI catalog on login and after successful datapack reloads. */
public final class GameplayCatalogSync {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static MinecraftServer observedServer;
    private static long broadcastGeneration = Long.MIN_VALUE;
    private static long cachedGeneration = Long.MIN_VALUE;
    private static GameplayCatalogPayload cachedPayload;

    private GameplayCatalogSync() {
    }

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) {
            return;
        }
        PlayerEvent.PLAYER_JOIN.register(GameplayCatalogSync::sendCurrent);
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, alive) -> sendCurrent(player));
        TickEvent.SERVER_POST.register(GameplayCatalogSync::onServerTick);
    }

    public static void sendCurrent(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        GameplayCatalogPayload payload = currentPayload();
        if (GalacticNetwork.canPlayerReceive(player, GameplayCatalogPayload.TYPE)) {
            GalacticNetwork.CHANNEL.sendToPlayer(() -> player, payload);
        }
    }

    private static void onServerTick(MinecraftServer server) {
        long generation = GameplayDataManager.generation();
        if (observedServer == server && broadcastGeneration == generation) {
            return;
        }
        observedServer = server;
        broadcastGeneration = generation;
        GameplayCatalogPayload payload = currentPayload();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (GalacticNetwork.canPlayerReceive(player, GameplayCatalogPayload.TYPE)) {
                GalacticNetwork.CHANNEL.sendToPlayer(() -> player, payload);
            }
        }
    }

    private static synchronized GameplayCatalogPayload currentPayload() {
        long generation = GameplayDataManager.generation();
        if (cachedPayload == null || cachedGeneration != generation) {
            cachedPayload = GameplayCatalogPayload.fromSnapshot(
                    GameplayDataManager.snapshot(), generation);
            cachedGeneration = generation;
        }
        return cachedPayload;
    }
}
