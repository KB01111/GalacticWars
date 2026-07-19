package galacticwars.clonewars.neoforge

import galacticwars.clonewars.GalacticWars
import galacticwars.clonewars.gametest.ModGameTests
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.RegisterGameTestsEvent
import net.neoforged.neoforge.registries.RegisterEvent
import net.neoforged.fml.common.Mod

@Mod(GalacticWars.MODID)
class GalacticWarsNeoForge(modEventBus: IEventBus) {
    init {
        NeoForgePlayerCampaignAttachments.register(modEventBus)
        GalacticWars.init()
        modEventBus.addListener { event: RegisterEvent ->
            ModGameTests.registerTestFunctions(event)
        }
        modEventBus.addListener { event: RegisterGameTestsEvent ->
            ModGameTests.registerGameTests(event)
        }
    }
}
