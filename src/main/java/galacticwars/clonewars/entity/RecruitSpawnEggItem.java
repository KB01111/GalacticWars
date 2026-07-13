package galacticwars.clonewars.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.SpawnEggItem;

/** Vanilla spawn egg whose entity type is bound through the standard item component. */
public final class RecruitSpawnEggItem extends SpawnEggItem {
    public RecruitSpawnEggItem(
            EntityType<GalacticRecruitEntity> recruitType,
            Properties properties
    ) {
        super(properties.spawnEgg(recruitType));
    }
}
