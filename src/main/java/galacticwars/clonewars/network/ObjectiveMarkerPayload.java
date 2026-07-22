package galacticwars.clonewars.network;

import galacticwars.clonewars.GalacticWars;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Bounded owner-only projection of the next embodied campaign action and its target. */
public record ObjectiveMarkerPayload(
        boolean active,
        String objectiveId,
        boolean targetKnown,
        String dimensionId,
        int x,
        int y,
        int z
) implements CustomPacketPayload {
    public static final int MAX_ID_LENGTH = 128;
    public static final Type<ObjectiveMarkerPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "objective_marker"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ObjectiveMarkerPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buffer, payload) -> {
                        buffer.writeBoolean(payload.active());
                        buffer.writeUtf(payload.objectiveId(), MAX_ID_LENGTH);
                        buffer.writeBoolean(payload.targetKnown());
                        buffer.writeUtf(payload.dimensionId(), MAX_ID_LENGTH);
                        buffer.writeInt(payload.x());
                        buffer.writeInt(payload.y());
                        buffer.writeInt(payload.z());
                    },
                    buffer -> new ObjectiveMarkerPayload(
                            buffer.readBoolean(),
                            buffer.readUtf(MAX_ID_LENGTH),
                            buffer.readBoolean(),
                            buffer.readUtf(MAX_ID_LENGTH),
                            buffer.readInt(),
                            buffer.readInt(),
                            buffer.readInt()));

    public ObjectiveMarkerPayload {
        objectiveId = normalize(objectiveId, "objectiveId");
        dimensionId = normalize(dimensionId, "dimensionId");
        if (!active && (!objectiveId.isEmpty() || targetKnown || !dimensionId.isEmpty())) {
            throw new IllegalArgumentException("inactive objective marker contains target state");
        }
        if (targetKnown && dimensionId.isEmpty()) {
            throw new IllegalArgumentException("known objective target has no dimension");
        }
    }

    public static ObjectiveMarkerPayload inactive() {
        return new ObjectiveMarkerPayload(false, "", false, "", 0, 0, 0);
    }

    @Override
    public Type<ObjectiveMarkerPayload> type() {
        return TYPE;
    }

    private static String normalize(String value, String label) {
        String normalized = Objects.requireNonNull(value, label).trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException(label + " exceeds the packet bound");
        }
        return normalized;
    }
}
