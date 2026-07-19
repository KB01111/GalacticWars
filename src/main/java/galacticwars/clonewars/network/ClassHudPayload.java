package galacticwars.clonewars.network;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.classes.ClassProgressState;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Bounded owner-only projection of authoritative player class state. */
public record ClassHudPayload(
        String classId,
        int rank,
        int resource,
        String ability1Id,
        int cooldown1,
        String ability2Id,
        int cooldown2
) implements CustomPacketPayload {
    public static final int MAX_ID_LENGTH = 128;
    public static final Type<ClassHudPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "class_hud"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ClassHudPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUtf(payload.classId(), MAX_ID_LENGTH);
                buffer.writeVarInt(payload.rank());
                buffer.writeVarInt(payload.resource());
                buffer.writeUtf(payload.ability1Id(), MAX_ID_LENGTH);
                buffer.writeVarInt(payload.cooldown1());
                buffer.writeUtf(payload.ability2Id(), MAX_ID_LENGTH);
                buffer.writeVarInt(payload.cooldown2());
            },
            buffer -> new ClassHudPayload(
                    buffer.readUtf(MAX_ID_LENGTH),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readUtf(MAX_ID_LENGTH),
                    buffer.readVarInt(),
                    buffer.readUtf(MAX_ID_LENGTH),
                    buffer.readVarInt()));

    public ClassHudPayload {
        classId = normalize(classId, true, "classId");
        ability1Id = normalize(ability1Id, true, "ability1Id");
        ability2Id = normalize(ability2Id, true, "ability2Id");
        if (rank < 0 || rank > ClassProgressState.MAX_RANK) {
            throw new IllegalArgumentException("rank is outside the class progression range");
        }
        if (resource < 0 || resource > ClassProgressState.MAX_RESOURCE) {
            throw new IllegalArgumentException("resource is outside the class progression range");
        }
        if (cooldown1 < 0 || cooldown2 < 0) {
            throw new IllegalArgumentException("class cooldowns cannot be negative");
        }
        if (classId.isEmpty() != (rank == 0)) {
            throw new IllegalArgumentException("class assignment and rank disagree");
        }
    }

    public static ClassHudPayload unassigned() {
        return new ClassHudPayload("", 0, ClassProgressState.MAX_RESOURCE, "", 0, "", 0);
    }

    @Override
    public Type<ClassHudPayload> type() {
        return TYPE;
    }

    private static String normalize(String value, boolean allowEmpty, String label) {
        String normalized = Objects.requireNonNull(value, label).trim().toLowerCase(Locale.ROOT);
        if ((!allowEmpty && normalized.isEmpty()) || normalized.length() > MAX_ID_LENGTH) {
            throw new IllegalArgumentException(label + " is not a bounded identifier");
        }
        return normalized;
    }
}
