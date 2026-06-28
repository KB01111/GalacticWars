package middleearth.lotr.warmod.registry;

import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(KingdomWarsMiddleEarth.MODID);

    public static final DeferredItem<BlockItem> MIDDLE_EARTH_STONE =
            ITEMS.registerSimpleBlockItem("middle_earth_stone", ModBlocks.MIDDLE_EARTH_STONE);
    public static final DeferredItem<BlockItem> MITHRIL_ORE =
            ITEMS.registerSimpleBlockItem("mithril_ore", ModBlocks.MITHRIL_ORE);
    public static final DeferredItem<BlockItem> MALLORN_LOG =
            ITEMS.registerSimpleBlockItem("mallorn_log", ModBlocks.MALLORN_LOG);
    public static final DeferredItem<Item> MITHRIL_INGOT =
            ITEMS.registerSimpleItem("mithril_ingot", properties -> properties);

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
