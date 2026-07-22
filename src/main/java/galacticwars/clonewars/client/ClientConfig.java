package galacticwars.clonewars.client;

import dev.architectury.platform.Platform;
import galacticwars.clonewars.GalacticWars;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Local-only accessibility and presentation preferences. */
public final class ClientConfig {
    private static final Path CONFIG_PATH = Platform.getConfigFolder().resolve("galacticwars-client.properties");
    private static final Map<String, BooleanValue> BOOLEANS = new LinkedHashMap<>();
    private static final Map<String, IntegerValue> INTEGERS = new LinkedHashMap<>();
    private static boolean loaded;

    public static final IntegerValue HUD_HORIZONTAL_OFFSET = integer("hudHorizontalOffset", 0, -200, 200);
    public static final IntegerValue HUD_VERTICAL_OFFSET = integer("hudVerticalOffset", 0, -100, 100);
    public static final IntegerValue HUD_SCALE_PERCENT = integer("hudScalePercent", 100, 50, 150);
    public static final IntegerValue EFFECT_INTENSITY_PERCENT = integer("effectIntensityPercent", 100, 0, 100);
    public static final IntegerValue PARTICLE_DENSITY_PERCENT = integer("particleDensityPercent", 100, 0, 100);
    public static final IntegerValue CAMERA_SHAKE_PERCENT = integer("cameraShakePercent", 100, 0, 100);
    public static final BooleanValue HIGH_CONTRAST = bool("highContrast", false);
    public static final BooleanValue AVOID_COLOR_ONLY = bool("avoidColorOnly", true);
    public static final BooleanValue NARRATION_HINTS = bool("narrationHints", false);

    private ClientConfig() {
    }

    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.isRegularFile(CONFIG_PATH)) {
            save();
            return;
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
            properties.load(input);
        } catch (IOException exception) {
            GalacticWars.LOGGER.error("Unable to read {}", CONFIG_PATH, exception);
            return;
        }
        BOOLEANS.forEach((key, value) -> {
            String encoded = properties.getProperty(key);
            if (encoded != null && (encoded.equalsIgnoreCase("true") || encoded.equalsIgnoreCase("false"))) {
                value.set(Boolean.parseBoolean(encoded));
            }
        });
        INTEGERS.forEach((key, value) -> {
            try {
                if (properties.containsKey(key)) value.set(Integer.parseInt(properties.getProperty(key)));
            } catch (NumberFormatException invalid) {
                GalacticWars.LOGGER.warn("Ignoring invalid integer {} in {}", key, CONFIG_PATH);
            }
        });
    }

    public static synchronized void save() {
        Properties properties = new Properties();
        BOOLEANS.forEach((key, value) -> properties.setProperty(key, Boolean.toString(value.get())));
        INTEGERS.forEach((key, value) -> properties.setProperty(key, Integer.toString(value.get())));
        Path temporary = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "Galactic Wars local accessibility configuration");
            }
            try {
                Files.move(temporary, CONFIG_PATH, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            GalacticWars.LOGGER.error("Unable to save {}", CONFIG_PATH, exception);
        }
    }

    private static BooleanValue bool(String key, boolean defaultValue) {
        BooleanValue result = new BooleanValue(defaultValue);
        if (BOOLEANS.putIfAbsent(key, result) != null) throw new IllegalStateException("Duplicate client key " + key);
        return result;
    }

    private static IntegerValue integer(String key, int defaultValue, int minimum, int maximum) {
        IntegerValue result = new IntegerValue(defaultValue, minimum, maximum);
        if (INTEGERS.putIfAbsent(key, result) != null) throw new IllegalStateException("Duplicate client key " + key);
        return result;
    }

    public static final class BooleanValue {
        private final boolean defaultValue;
        private final AtomicBoolean value;
        private BooleanValue(boolean defaultValue) {
            this.defaultValue = defaultValue;
            this.value = new AtomicBoolean(defaultValue);
        }
        public boolean getDefault() { return defaultValue; }
        public boolean get() { return value.get(); }
        public void set(boolean next) { value.set(next); }
    }

    public static final class IntegerValue {
        private final int defaultValue;
        private final int minimum;
        private final int maximum;
        private final AtomicInteger value;
        private IntegerValue(int defaultValue, int minimum, int maximum) {
            this.defaultValue = defaultValue;
            this.minimum = minimum;
            this.maximum = maximum;
            this.value = new AtomicInteger(defaultValue);
        }
        public int getDefault() { return defaultValue; }
        public int get() { return value.get(); }
        public int minimum() { return minimum; }
        public int maximum() { return maximum; }
        public void set(int next) { value.set(Math.max(minimum, Math.min(maximum, next))); }
    }
}
