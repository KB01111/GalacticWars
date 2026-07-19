package galacticwars.clonewars.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.network.ClassActivatePayload;
import galacticwars.clonewars.network.GalacticNetwork;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** Rebindable active-class controls kept separate from Force and vehicle controls. */
public final class ClassKeyMappings {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(GalacticWars.MODID, "class_abilities"));
    private static final KeyMapping[] KEYS = {
            new KeyMapping("key.galacticwars.class_1", InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_V, CATEGORY),
            new KeyMapping("key.galacticwars.class_2", InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_B, CATEGORY)
    };
    private static final List<KeyMapping> MAPPINGS = List.of(KEYS);
    private static final AtomicBoolean MAPPINGS_REGISTERED = new AtomicBoolean();
    private static final AtomicBoolean TICK_HANDLER_REGISTERED = new AtomicBoolean();

    private ClassKeyMappings() {
    }

    public static void register() {
        if (MAPPINGS_REGISTERED.compareAndSet(false, true)) {
            MAPPINGS.forEach(KeyMappingRegistry::register);
        }
        registerTickHandler();
    }

    public static List<KeyMapping> mappings() {
        return MAPPINGS;
    }

    public static void registerTickHandler() {
        if (TICK_HANDLER_REGISTERED.compareAndSet(false, true)) {
            ClientTickEvent.CLIENT_POST.register(ClassKeyMappings::tick);
        }
    }

    private static void tick(Minecraft minecraft) {
        boolean acceptsGameplayInput = minecraft.player != null
                && minecraft.gui.screen() == null
                && minecraft.gui.overlay() == null;
        for (int slot = 0; slot < KEYS.length; slot++) {
            while (KEYS[slot].consumeClick()) {
                if (acceptsGameplayInput) {
                    GalacticNetwork.CHANNEL.sendToServer(
                            new ClassActivatePayload(UUID.randomUUID(), slot));
                }
            }
        }
    }
}
