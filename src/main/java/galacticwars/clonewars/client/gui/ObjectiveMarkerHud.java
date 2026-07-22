package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.client.ObjectiveMarkerClientState;
import galacticwars.clonewars.client.ClientConfig;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Dimension-aware direction, distance, and next-action campaign guidance. */
public final class ObjectiveMarkerHud {
    private ObjectiveMarkerHud() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        var state = ObjectiveMarkerClientState.snapshot();
        if (player == null || !state.active()) {
            return;
        }
        Component action = Component.translatable(
                "screen.galacticwars.operations.objective." + state.objectiveId());
        int offsetX = ClientConfig.HUD_HORIZONTAL_OFFSET.get();
        int offsetY = ClientConfig.HUD_VERTICAL_OFFSET.get();
        int primaryColor = ClientConfig.HIGH_CONTRAST.get() ? 0xFFFFFFFF : 0xFFE8EEF7;
        drawCentered(graphics, action, 12 + offsetY, offsetX, primaryColor);
        if (!state.targetKnown()) {
            return;
        }
        String currentDimension = player.level().dimension().identifier().toString();
        if (!currentDimension.equals(state.dimensionId())) {
            drawCentered(graphics,
                    Component.translatable("hud.galacticwars.objective.travel",
                            dimensionName(state.dimensionId())), 24 + offsetY, offsetX, 0xFFFFC96B);
            return;
        }
        double dx = state.x() + 0.5D - player.getX();
        double dz = state.z() + 0.5D - player.getZ();
        int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double relative = wrapDegrees(targetYaw - player.getYRot());
        String arrow = Math.abs(relative) < 22.5D ? "↑"
                : relative >= 157.5D || relative < -157.5D ? "↓"
                : relative >= 112.5D ? "↙" : relative >= 67.5D ? "←"
                : relative >= 22.5D ? "↖" : relative >= -22.5D ? "↑"
                : relative >= -67.5D ? "↗" : relative >= -112.5D ? "→" : "↘";
        drawCentered(graphics,
                Component.translatable("hud.galacticwars.objective.marker", arrow, distance),
                24 + offsetY, offsetX, 0xFFFFC96B);
    }

    private static Component dimensionName(String dimensionId) {
        String path = dimensionId.substring(dimensionId.indexOf(':') + 1)
                .toLowerCase(Locale.ROOT);
        return dimensionId.startsWith("galacticwars:")
                ? Component.translatable("planet.galacticwars." + path)
                : Component.translatable("dimension." + dimensionId.replace(':', '.'));
    }

    private static void drawCentered(
            GuiGraphicsExtractor graphics, Component text, int y, int offsetX, int color
    ) {
        int x = (graphics.guiWidth() - Minecraft.getInstance().font.width(text)) / 2 + offsetX;
        graphics.text(Minecraft.getInstance().font, text, x, y, color);
    }

    private static double wrapDegrees(double value) {
        double wrapped = value % 360.0D;
        if (wrapped >= 180.0D) wrapped -= 360.0D;
        if (wrapped < -180.0D) wrapped += 360.0D;
        return wrapped;
    }
}
