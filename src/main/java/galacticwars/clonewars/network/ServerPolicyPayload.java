package galacticwars.clonewars.network;

import galacticwars.clonewars.Config;
import galacticwars.clonewars.GalacticWars;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Read-only client snapshot of authoritative dedicated-server gameplay policy. */
public record ServerPolicyPayload(
        boolean blasterFriendlyFire,
        boolean blasterPvp,
        boolean classPvp,
        boolean forcePvp,
        boolean forceBlockPhysics,
        boolean forceVehiclePhysics
) implements CustomPacketPayload {
    public static final Type<ServerPolicyPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "server_policy"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerPolicyPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeBoolean(payload.blasterFriendlyFire());
                buffer.writeBoolean(payload.blasterPvp());
                buffer.writeBoolean(payload.classPvp());
                buffer.writeBoolean(payload.forcePvp());
                buffer.writeBoolean(payload.forceBlockPhysics());
                buffer.writeBoolean(payload.forceVehiclePhysics());
            },
            buffer -> new ServerPolicyPayload(
                    buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
                    buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean()));

    public static ServerPolicyPayload current() {
        return new ServerPolicyPayload(
                Config.ALLOW_BLASTER_FRIENDLY_FIRE.get(), Config.ALLOW_BLASTER_PVP.get(),
                Config.ALLOW_CLASS_PVP.get(), Config.ALLOW_FORCE_PVP.get(),
                Config.ALLOW_FORCE_BLOCK_PHYSICS.get(), Config.ALLOW_FORCE_VEHICLE_PHYSICS.get());
    }

    @Override
    public Type<ServerPolicyPayload> type() {
        return TYPE;
    }
}
