package galacticwars.clonewars.network;

import galacticwars.clonewars.GalacticWars;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Replay-addressable request to activate one of the selected player's active class abilities. */
public record ClassActivatePayload(UUID activationId, int slot) implements CustomPacketPayload {
    public static final Type<ClassActivatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "class_activate"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClassActivatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.activationId());
                buffer.writeVarInt(payload.slot());
            },
            buffer -> new ClassActivatePayload(buffer.readUUID(), buffer.readVarInt()));

    public ClassActivatePayload {
        Objects.requireNonNull(activationId, "activationId");
    }

    @Override
    public Type<ClassActivatePayload> type() {
        return TYPE;
    }
}
