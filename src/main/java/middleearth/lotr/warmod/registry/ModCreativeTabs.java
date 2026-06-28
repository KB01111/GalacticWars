package middleearth.lotr.warmod.registry;

import middleearth.lotr.warmod.KingdomWarsMiddleEarth;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, KingdomWarsMiddleEarth.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MIDDLE_EARTH =
            CREATIVE_MODE_TABS.register("middle_earth", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.kingdomwarsmiddleearth.middle_earth"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.MITHRIL_INGOT.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.MIDDLE_EARTH_STONE.get());
                        output.accept(ModItems.MITHRIL_ORE.get());
                        output.accept(ModItems.MALLORN_LOG.get());
                        output.accept(ModItems.MITHRIL_INGOT.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
