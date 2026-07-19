package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.client.ClassClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Compact class resource, rank and active-slot HUD. */
public final class ClassHud {
    private ClassHud() {
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (!ClassClientState.visible() || Minecraft.getInstance().player == null) {
            return;
        }
        var state = ClassClientState.snapshot();
        int width = 100;
        int left = 12;
        int top = graphics.guiHeight() - 56;
        graphics.fill(left, top, left + width + 2, top + 8, 0xAA080C12);
        graphics.fill(left + 1, top + 1,
                left + 1 + state.resource(), top + 7, 0xFFFFB84D);
        graphics.text(Minecraft.getInstance().font,
                Component.translatable(
                        "hud.galacticwars.class",
                        humanize(state.classId()),
                        state.rank(),
                        state.resource(),
                        slot(state.ability1Id(), state.cooldown1()),
                        slot(state.ability2Id(), state.cooldown2())),
                left, top - 11, 0xFFFFE4B8);
    }

    private static String slot(String abilityId, int cooldown) {
        return abilityId.isBlank() ? "-" : humanize(abilityId) + " " + cooldown;
    }

    private static String humanize(String identifier) {
        String path = identifier.substring(identifier.indexOf(':') + 1);
        String[] words = path.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }
}
