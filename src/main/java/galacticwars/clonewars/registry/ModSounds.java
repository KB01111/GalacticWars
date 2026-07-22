package galacticwars.clonewars.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import galacticwars.clonewars.GalacticWars;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Subtitle-backed mod sound identities; sounds.json maps them to licensed runtime events. */
public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(GalacticWars.MODID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> BLASTER_FIRE = register("blaster_fire");
    public static final RegistrySupplier<SoundEvent> LIGHTSABER_SWING = register("lightsaber_swing");
    public static final RegistrySupplier<SoundEvent> FORCE_USE = register("force_use");
    public static final RegistrySupplier<SoundEvent> VEHICLE_ENGINE = register("vehicle_engine");
    public static final RegistrySupplier<SoundEvent> PLANET_TRAVEL = register("planet_travel");
    public static final RegistrySupplier<SoundEvent> CONSTRUCTION_COMPLETE = register("construction_complete");
    public static final RegistrySupplier<SoundEvent> MISSION_UPDATE = register("mission_update");
    public static final RegistrySupplier<SoundEvent> PLANET_AMBIENCE = register("planet_ambience");

    private ModSounds() {
    }

    public static void register() {
        SOUNDS.register();
    }

    private static RegistrySupplier<SoundEvent> register(String path) {
        Identifier id = Identifier.fromNamespaceAndPath(GalacticWars.MODID, path);
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(id));
    }
}
