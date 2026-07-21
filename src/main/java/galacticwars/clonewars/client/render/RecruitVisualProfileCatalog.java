package galacticwars.clonewars.client.render;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.recruitment.RecruitDuty;
import galacticwars.clonewars.registry.ModEntityTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.Map;
import java.util.Objects;

/** Immutable, save-neutral mappings from synchronized recruit duty to visual texture variants. */
public final class RecruitVisualProfileCatalog {
    private static final Map<EntityType<?>, String> COMMANDER_TEXTURE_OVERRIDES = Map.of(
            ModEntityTypes.CLONE_TROOPER.get(), "clone_trooper_commander",
            ModEntityTypes.ARC_TROOPER.get(), "arc_trooper_commander",
            ModEntityTypes.B1_BATTLE_DROID.get(), "b1_battle_droid_commander"
    );

    private RecruitVisualProfileCatalog() {
    }

    public static Identifier textureResource(
            EntityType<GalacticRecruitEntity> entityType,
            RecruitDuty duty
    ) {
        Objects.requireNonNull(entityType, "entityType");
        Identifier registeredId = Objects.requireNonNull(
                BuiltInRegistries.ENTITY_TYPE.getKey(entityType),
                "registered entity type"
        );
        RecruitDuty resolvedDuty = duty == null ? RecruitDuty.SOLDIER : duty;
        String textureId = registeredId.getPath();
        if (resolvedDuty == RecruitDuty.COMMANDER) {
            textureId = COMMANDER_TEXTURE_OVERRIDES.getOrDefault(entityType, textureId);
        }
        return Identifier.fromNamespaceAndPath(
                GalacticWars.MODID,
                "textures/entity/" + textureId + ".png"
        );
    }
}
