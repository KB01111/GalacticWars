package galacticwars.clonewars.client.gui;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import galacticwars.clonewars.client.ClientConfig;
import galacticwars.clonewars.client.ServerPolicyClientState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Local accessibility controls plus a read-only authoritative server-policy snapshot. */
public final class GalacticWarsConfigScreen {
    private GalacticWarsConfigScreen() {
    }

    public static Screen create(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(text("config.galacticwars.title"))
                .category(ConfigCategory.createBuilder()
                        .name(text("config.galacticwars.category.accessibility"))
                        .group(OptionGroup.createBuilder()
                                .name(text("config.galacticwars.group.hud"))
                                .description(OptionDescription.of(
                                        text("config.galacticwars.group.hud.description")))
                                .option(integerOption("hud_horizontal_offset", ClientConfig.HUD_HORIZONTAL_OFFSET, 10))
                                .option(integerOption("hud_vertical_offset", ClientConfig.HUD_VERTICAL_OFFSET, 5))
                                .option(integerOption("hud_scale_percent", ClientConfig.HUD_SCALE_PERCENT, 5))
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(text("config.galacticwars.group.effects"))
                                .description(OptionDescription.of(
                                        text("config.galacticwars.group.effects.description")))
                                .option(integerOption("effect_intensity_percent", ClientConfig.EFFECT_INTENSITY_PERCENT, 5))
                                .option(integerOption("particle_density_percent", ClientConfig.PARTICLE_DENSITY_PERCENT, 5))
                                .option(integerOption("camera_shake_percent", ClientConfig.CAMERA_SHAKE_PERCENT, 5))
                                .option(booleanOption("high_contrast", ClientConfig.HIGH_CONTRAST))
                                .option(booleanOption("avoid_color_only", ClientConfig.AVOID_COLOR_ONLY))
                                .option(booleanOption("narration_hints", ClientConfig.NARRATION_HINTS))
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(text("config.galacticwars.category.server_policy"))
                        .group(OptionGroup.createBuilder()
                                .name(text("config.galacticwars.group.server_policy"))
                                .description(OptionDescription.of(policyDescription()))
                                .build())
                        .build())
                .save(ClientConfig::save)
                .build()
                .generateScreen(parent);
    }

    private static Option<Boolean> booleanOption(
            String key,
            ClientConfig.BooleanValue value
    ) {
        return Option.<Boolean>createBuilder()
                .name(text("config.galacticwars.option." + key))
                .description(OptionDescription.of(
                        text("config.galacticwars.option." + key + ".description")))
                .binding(value.getDefault(), value::get, value::set)
                .controller(option -> BooleanControllerBuilder.create(option)
                        .coloured(true)
                        .onOffFormatter())
                .build();
    }

    private static Option<Integer> integerOption(
            String key, ClientConfig.IntegerValue value, int step
    ) {
        return Option.<Integer>createBuilder()
                .name(text("config.galacticwars.option." + key))
                .description(OptionDescription.of(
                        text("config.galacticwars.option." + key + ".description")))
                .binding(value.getDefault(), value::get, value::set)
                .controller(option -> IntegerSliderControllerBuilder.create(option)
                        .range(value.minimum(), value.maximum()).step(step))
                .build();
    }

    private static Component policyDescription() {
        var policy = ServerPolicyClientState.snapshot();
        return Component.translatable("config.galacticwars.group.server_policy.description",
                onOff(policy.blasterFriendlyFire()), onOff(policy.blasterPvp()),
                onOff(policy.classPvp()), onOff(policy.forcePvp()),
                onOff(policy.forceBlockPhysics()), onOff(policy.forceVehiclePhysics()));
    }

    private static Component onOff(boolean value) {
        return Component.translatable(value ? "options.on" : "options.off");
    }

    private static Component text(String key) {
        return Component.translatable(key);
    }
}
