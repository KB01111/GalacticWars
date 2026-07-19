package galacticwars.clonewars.network;

import galacticwars.clonewars.GalacticWars;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ForceHudPayload(int energy, int cooldown1, int cooldown2, int cooldown3)
        implements CustomPacketPayload {
    public static final Type<ForceHudPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "force_hud"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ForceHudPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.energy());
                buffer.writeVarInt(payload.cooldown1());
                buffer.writeVarInt(payload.cooldown2());
                buffer.writeVarInt(payload.cooldown3());
            }, buffer -> new ForceHudPayload(buffer.readVarInt(), buffer.readVarInt(),
                    buffer.readVarInt(), buffer.readVarInt()));

    @Override
    public Type<ForceHudPayload> type() {
        return TYPE;
    }
}
