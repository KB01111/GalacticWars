package galacticwars.clonewars.network;

import galacticwars.clonewars.GalacticWars;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-validated request to select a player-assignable class from a Command Center. */
public record ClassSelectPayload(UUID requestId, String classId) implements CustomPacketPayload {
    public static final int MAX_CLASS_ID_LENGTH = 128;
    public static final Type<ClassSelectPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "class_select"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClassSelectPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUUID(payload.requestId());
                buffer.writeUtf(payload.classId(), MAX_CLASS_ID_LENGTH);
            },
            buffer -> new ClassSelectPayload(
                    buffer.readUUID(), buffer.readUtf(MAX_CLASS_ID_LENGTH)));

    public ClassSelectPayload {
        Objects.requireNonNull(requestId, "requestId");
        classId = Objects.requireNonNull(classId, "classId").trim().toLowerCase(Locale.ROOT);
        if (classId.isBlank() || classId.length() > MAX_CLASS_ID_LENGTH) {
            throw new IllegalArgumentException("classId must be a bounded non-blank identifier");
        }
    }

    @Override
    public Type<ClassSelectPayload> type() {
        return TYPE;
    }
}
