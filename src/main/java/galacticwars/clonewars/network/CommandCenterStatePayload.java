package galacticwars.clonewars.network;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState;
import galacticwars.clonewars.menu.CommandCenterDashboardCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.Objects;

/** Server-owned refresh of the currently open Command Center dashboard. */
public record CommandCenterStatePayload(
        int containerId,
        CommandCenterDashboardState state
) implements CustomPacketPayload {
    public static final Type<CommandCenterStatePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "command_center_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CommandCenterStatePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeVarInt(payload.containerId());
                        CommandCenterDashboardCodec.write(buffer, payload.state());
                    },
                    buffer -> new CommandCenterStatePayload(
                            buffer.readVarInt(), CommandCenterDashboardCodec.read(buffer)));

    public CommandCenterStatePayload {
        if (containerId < 0) {
            throw new IllegalArgumentException("containerId cannot be negative");
        }
        Objects.requireNonNull(state, "state");
    }

    @Override
    public Type<CommandCenterStatePayload> type() {
        return TYPE;
    }
}
