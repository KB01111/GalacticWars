package middleearth.lotr.warmod;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import middleearth.lotr.warmod.registry.ModBlocks;
import middleearth.lotr.warmod.registry.ModCreativeTabs;
import middleearth.lotr.warmod.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(KingdomWarsMiddleEarth.MODID)
public class KingdomWarsMiddleEarth {
    public static final String MODID = "kingdomwarsmiddleearth";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KingdomWarsMiddleEarth(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        if (Config.LOG_STARTUP.getAsBoolean()) {
            LOGGER.info("KingdomWars-Middle-Earth foundation loaded.");
        }
    }
}
