package galacticwars.clonewars.client.render;

import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.recruitment.RecruitDuty;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

/** Default recruit model with a synchronized, duty-sensitive texture selection. */
public final class GalacticRecruitGeoModel extends DefaultedEntityGeoModel<GalacticRecruitEntity> {
    public static final DataTicket<RecruitDuty> RECRUIT_DUTY = DataTickets.create(
            "galacticwars:recruit_duty", RecruitDuty.class);

    private final String visualId;

    public GalacticRecruitGeoModel(EntityType<GalacticRecruitEntity> entityType) {
        super(entityType);
        this.visualId = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).getPath();
    }

    @Override
    public void addAdditionalStateData(
            GalacticRecruitEntity animatable,
            Object relatedObject,
            GeoRenderState renderState
    ) {
        super.addAdditionalStateData(animatable, relatedObject, renderState);
        renderState.addGeckolibData(RECRUIT_DUTY, animatable.getRecruitDuty());
    }

    @Override
    public Identifier getTextureResource(GeoRenderState renderState) {
        RecruitDuty duty = renderState.getOrDefaultGeckolibData(RECRUIT_DUTY, RecruitDuty.SOLDIER);
        String textureId = RecruitVisualProfileCatalog.textureId(this.visualId, duty);
        return Identifier.fromNamespaceAndPath(
                GalacticWars.MODID,
                "textures/entity/" + textureId + ".png");
    }
}
