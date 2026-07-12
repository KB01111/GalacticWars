package galacticwars.clonewars.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class YaclConfigIntegrationTest {
    private YaclConfigIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        String gradleProperties = read("gradle.properties");
        String buildGradle = read("build.gradle");
        String metadata = read("src/main/templates/META-INF/neoforge.mods.toml");
        String client = read("src/main/java/galacticwars/clonewars/GalacticWarsClient.java");
        String screen = read("src/main/java/galacticwars/clonewars/client/gui/GalacticWarsConfigScreen.java");
        String language = read("src/main/resources/assets/galacticwars/lang/en_us.json");
        String notice = read("NOTICE.md");

        assertContains(gradleProperties, "yacl_version=3.9.5+26.2-neoforge", "YACL version");
        assertContains(buildGradle, "https://maven.isxander.dev/releases", "YACL Maven repository");
        assertContains(buildGradle,
                "compileOnly(\"dev.isxander:yet-another-config-lib:${yacl_version}\")",
                "compile-only YACL dependency");
        assertContains(buildGradle,
                "localRuntime(\"dev.isxander:yet-another-config-lib:${yacl_version}\")",
                "development YACL runtime");
        assertContains(buildGradle, "transitive = false", "embedded YACL dependency handling");
        assertContains(metadata, "modId=\"yet_another_config_lib_v3\"", "YACL mod dependency");
        assertContains(metadata, "versionRange=\"[${yacl_version},)\"", "YACL version range");
        assertContains(metadata, "side=\"CLIENT\"", "client-only dependency side");
        assertContains(client, "GalacticWarsConfigScreen.create(parent)", "YACL screen factory");
        assertContains(screen, "YetAnotherConfigLib.createBuilder()", "YACL builder");
        assertContains(screen, ".save(Config::save)", "NeoForge config persistence");

        for (String option : new String[]{
                "log_startup",
                "enable_content_seed",
                "allow_blaster_friendly_fire",
                "allow_blaster_pvp",
                "allow_force_pvp"
        }) {
            assertContains(screen, "\"" + option + "\"", option + " binding");
            assertContains(language, "config.galacticwars.option." + option, option + " translation");
        }

        assertContains(notice, "YetAnotherConfigLib", "YACL license notice");
        assertNotContains(buildGradle, "jarJar(\"dev.isxander:yet-another-config-lib", "bundled YACL");

        System.out.println("YaclConfigIntegrationTest passed");
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private static void assertContains(String haystack, String needle, String label) {
        if (!haystack.contains(needle)) {
            throw new AssertionError(label + " missing <" + needle + ">");
        }
    }

    private static void assertNotContains(String haystack, String needle, String label) {
        if (haystack.contains(needle)) {
            throw new AssertionError(label + " unexpectedly contained <" + needle + ">");
        }
    }
}
