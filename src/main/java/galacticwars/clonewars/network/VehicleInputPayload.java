package galacticwars.clonewars.network;

import galacticwars.clonewars.GalacticWars;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record VehicleInputPayload(
        UUID replayId, int entityId, float forward, float strafe,
        boolean ascend, boolean descend, boolean fire
) implements CustomPacketPayload {
    public static final Type<VehicleInputPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "vehicle_input"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VehicleInputPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.replayId());
                buffer.writeVarInt(payload.entityId());
                buffer.writeFloat(payload.forward());
                buffer.writeFloat(payload.strafe());
                buffer.writeBoolean(payload.ascend());
                buffer.writeBoolean(payload.descend());
                buffer.writeBoolean(payload.fire());
            }, buffer -> new VehicleInputPayload(buffer.readUUID(), buffer.readVarInt(),
                buffer.readFloat(), buffer.readFloat(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean()));

    @Override
    public Type<VehicleInputPayload> type() {
        return TYPE;
    }
}
