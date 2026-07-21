package galacticwars.clonewars.network;

import galacticwars.clonewars.GalacticWars;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Replay-addressable request for server-owned menu actions with explicit optional targets. */
public record MenuActionPayload(
        UUID replayId,
        int containerId,
        int actionId,
        Optional<UUID> primaryTargetId,
        Optional<UUID> secondaryTargetId,
        long expectedContentGeneration,
        int expectedSettlementRevision
) implements CustomPacketPayload {
    public static final Type<MenuActionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "menu_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MenuActionPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.replayId());
                buffer.writeVarInt(payload.containerId());
                buffer.writeVarInt(payload.actionId());
                writeOptionalUuid(buffer, payload.primaryTargetId());
                writeOptionalUuid(buffer, payload.secondaryTargetId());
                buffer.writeVarLong(payload.expectedContentGeneration());
                buffer.writeVarInt(payload.expectedSettlementRevision());
            }, buffer -> new MenuActionPayload(
                    buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(),
                    readOptionalUuid(buffer), readOptionalUuid(buffer),
                    buffer.readVarLong(), buffer.readVarInt()));

    public MenuActionPayload(UUID replayId, int containerId, int actionId) {
        this(replayId, containerId, actionId, Optional.empty(), Optional.empty(), -1L, -1);
    }

    public MenuActionPayload(
            UUID replayId,
            int containerId,
            int actionId,
            Optional<UUID> primaryTargetId,
            Optional<UUID> secondaryTargetId
    ) {
        this(replayId, containerId, actionId, primaryTargetId, secondaryTargetId, -1L, -1);
    }

    public MenuActionPayload {
        if (replayId == null || containerId < 0 || actionId < 0 || actionId > 255
                || expectedContentGeneration < -1L || expectedSettlementRevision < -1) {
            throw new IllegalArgumentException("invalid menu action payload");
        }
        primaryTargetId = primaryTargetId == null ? Optional.empty() : primaryTargetId;
        secondaryTargetId = secondaryTargetId == null ? Optional.empty() : secondaryTargetId;
    }

    private static void writeOptionalUuid(RegistryFriendlyByteBuf buffer, Optional<UUID> value) {
        buffer.writeBoolean(value.isPresent());
        value.ifPresent(buffer::writeUUID);
    }

    private static Optional<UUID> readOptionalUuid(RegistryFriendlyByteBuf buffer) {
        return buffer.readBoolean() ? Optional.of(buffer.readUUID()) : Optional.empty();
    }

    @Override
    public Type<MenuActionPayload> type() {
        return TYPE;
    }
}
