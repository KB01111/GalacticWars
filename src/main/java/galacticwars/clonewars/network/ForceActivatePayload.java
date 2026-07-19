package galacticwars.clonewars.network;

import galacticwars.clonewars.GalacticWars;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ForceActivatePayload(UUID activationId, int slot) implements CustomPacketPayload {
    public static final Type<ForceActivatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "force_activate"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ForceActivatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.activationId());
                buffer.writeVarInt(payload.slot());
            },
            buffer -> new ForceActivatePayload(buffer.readUUID(), buffer.readVarInt()));

    @Override
    public Type<ForceActivatePayload> type() {
        return TYPE;
    }
}
