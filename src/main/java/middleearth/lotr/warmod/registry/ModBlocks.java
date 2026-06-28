package middleearth.lotr.warmod.registry;

import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(KingdomWarsMiddleEarth.MODID);

    public static final DeferredBlock<Block> MIDDLE_EARTH_STONE =
            BLOCKS.registerSimpleBlock("middle_earth_stone", properties -> properties.mapColor(MapColor.STONE));
    public static final DeferredBlock<Block> MITHRIL_ORE =
            BLOCKS.registerSimpleBlock("mithril_ore", properties -> properties.mapColor(MapColor.STONE));
    public static final DeferredBlock<Block> MALLORN_LOG =
            BLOCKS.registerSimpleBlock("mallorn_log", properties -> properties.mapColor(MapColor.STONE));

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
