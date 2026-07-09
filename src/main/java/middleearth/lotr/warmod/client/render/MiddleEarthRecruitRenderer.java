package middleearth.lotr.warmod.client.render;

import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import middleearth.lotr.warmod.entity.MiddleEarthRecruitEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

public class MiddleEarthRecruitRenderer
        extends HumanoidMobRenderer<MiddleEarthRecruitEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    private final Identifier texture;

    public MiddleEarthRecruitRenderer(EntityRendererProvider.Context context) {
        this(context, "gondor_recruit");
    }

    public MiddleEarthRecruitRenderer(EntityRendererProvider.Context context, String textureName) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.texture = Identifier.fromNamespaceAndPath(
                KingdomWarsMiddleEarth.MODID,
                "textures/entity/" + textureName + ".png");
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return texture;
    }
}
