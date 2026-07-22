package galacticwars.clonewars.fabric

import dev.architectury.platform.Platform
import dev.architectury.platform.client.ConfigurationScreenRegistry
import galacticwars.clonewars.GalacticWars
import galacticwars.clonewars.GalacticWarsClient
import galacticwars.clonewars.client.gui.BlasterHeatHud
import galacticwars.clonewars.client.gui.ClassHud
import galacticwars.clonewars.client.gui.ForceHud
import galacticwars.clonewars.client.gui.ObjectiveMarkerHud
import galacticwars.clonewars.client.gui.GalacticWarsConfigScreen
import galacticwars.clonewars.client.gui.VehicleHud
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.minecraft.resources.Identifier

class GalacticWarsFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        GalacticWarsClient.init()
        ConfigurationScreenRegistry.register(Platform.getMod(GalacticWars.MODID)) { parent ->
            GalacticWarsConfigScreen.create(parent)
        }

        attachHud("blaster_heat") { graphics -> BlasterHeatHud.render(graphics) }
        attachHud("force_hud") { graphics -> ForceHud.render(graphics) }
        attachHud("class_hud") { graphics -> ClassHud.render(graphics) }
        attachHud("vehicle_hud") { graphics -> VehicleHud.render(graphics) }
        attachHud("objective_marker") { graphics -> ObjectiveMarkerHud.render(graphics) }
    }

    private fun attachHud(
        path: String,
        renderer: (net.minecraft.client.gui.GuiGraphicsExtractor) -> Unit,
    ) {
        HudElementRegistry.attachElementAfter(
            VanillaHudElements.HOTBAR,
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, path),
        ) { graphics, _ -> renderer(graphics) }
    }
}
