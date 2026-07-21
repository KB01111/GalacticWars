package galacticwars.clonewars.client.render;

import galacticwars.clonewars.recruitment.RecruitDuty;

import java.util.Map;
import java.util.Objects;

/** Immutable, save-neutral mappings from synchronized recruit duty to visual texture variants. */
public final class RecruitVisualProfileCatalog {
    private static final Map<VisualKey, String> TEXTURE_OVERRIDES = Map.of(
            new VisualKey("clone_trooper", RecruitDuty.COMMANDER), "clone_trooper_commander",
            new VisualKey("arc_trooper", RecruitDuty.COMMANDER), "arc_trooper_commander",
            new VisualKey("b1_battle_droid", RecruitDuty.COMMANDER), "b1_battle_droid_commander"
    );

    private RecruitVisualProfileCatalog() {
    }

    public static String textureId(String entityId, RecruitDuty duty) {
        String base = Objects.requireNonNull(entityId, "entityId");
        RecruitDuty resolvedDuty = duty == null ? RecruitDuty.SOLDIER : duty;
        return TEXTURE_OVERRIDES.getOrDefault(new VisualKey(base, resolvedDuty), base);
    }

    private record VisualKey(String entityId, RecruitDuty duty) {
    }
}
