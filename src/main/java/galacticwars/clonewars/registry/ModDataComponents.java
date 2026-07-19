package galacticwars.clonewars.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.combat.BlasterHeatPolicy;
import galacticwars.clonewars.item.CommandTargetSelection;
import galacticwars.clonewars.settlement.ConstructionPlan;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;

public final class ModDataComponents {
    private static final Codec<BlasterHeatPolicy.BlasterHeatState> BLASTER_HEAT_CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("shots_remaining")
                            .forGetter(BlasterHeatPolicy.BlasterHeatState::shotsRemaining),
                    Codec.INT.fieldOf("shot_cooldown_ticks")
                            .forGetter(BlasterHeatPolicy.BlasterHeatState::shotCooldownTicks),
                    Codec.INT.fieldOf("overheat_ticks")
                            .forGetter(BlasterHeatPolicy.BlasterHeatState::overheatTicks)
            ).apply(instance, BlasterHeatPolicy.BlasterHeatState::new));

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(GalacticWars.MODID, Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<BlasterHeatPolicy.BlasterHeatState>> BLASTER_HEAT =
            persistent("blaster_heat", BLASTER_HEAT_CODEC);
    public static final RegistrySupplier<DataComponentType<ConstructionPlan>> CONSTRUCTION_PLAN =
            persistent("construction_plan", ConstructionPlan.CODEC);
    public static final RegistrySupplier<DataComponentType<CommandTargetSelection>> COMMAND_TARGET =
            persistent("command_target", CommandTargetSelection.CODEC);

    private ModDataComponents() {
    }

    public static void register() {
        COMPONENTS.register();
    }

    private static <T> RegistrySupplier<DataComponentType<T>> persistent(String name, Codec<T> codec) {
        return COMPONENTS.register(name, () -> DataComponentType.<T>builder()
                .persistent(codec)
                .build());
    }
}
