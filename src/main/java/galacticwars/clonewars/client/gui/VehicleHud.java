package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.client.ClientGameplayCatalog;
import galacticwars.clonewars.vehicle.GalacticVehicleEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class VehicleHud {
    private VehicleHud() {}
    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(minecraft.player.getVehicle() instanceof GalacticVehicleEntity vehicle)) return;
        int left = graphics.guiWidth() - 154;
        int top = graphics.guiHeight() - 38;
        var configured = ClientGameplayCatalog.snapshot().vehicle(vehicle.vehicleId()).orElse(null);
        int maximumHealth = configured == null
                ? vehicle.syncedMaximumHealth()
                : Math.max(vehicle.health(), configured.maxHealth());
        int maximumFuel = configured == null
                ? vehicle.syncedFuelCapacity()
                : Math.max(vehicle.fuel(), configured.fuelCapacity());
        graphics.fill(left, top, left + 142, top + 26, 0xBB080C12);
        graphics.text(minecraft.font, Component.translatable("hud.galacticwars.vehicle",
                vehicle.health(), maximumHealth, vehicle.fuel(), maximumFuel),
                left + 6, top + 8, 0xFFBDEBFF);
    }
}
