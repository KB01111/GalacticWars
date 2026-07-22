package galacticwars.clonewars.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class YaclConfigIntegrationTest {
    private YaclConfigIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        String gradleProperties = read("gradle.properties");
        String rootBuild = read("build.gradle.kts");
        String fabricBuild = read("fabric/build.gradle.kts");
        String neoForgeBuild = read("neoforge/build.gradle.kts");
        String fabricMetadata = read("fabric/src/main/resources/fabric.mod.json");
        String neoForgeMetadata = read("neoforge/src/main/resources/META-INF/neoforge.mods.toml");
        String fabricClient = read("fabric/src/main/kotlin/galacticwars/clonewars/fabric/GalacticWarsFabricClient.kt");
        String neoForgeClient = read("neoforge/src/main/kotlin/galacticwars/clonewars/neoforge/GalacticWarsNeoForgeClient.kt");
        String screen = read("src/main/java/galacticwars/clonewars/client/gui/GalacticWarsConfigScreen.java");
        String serverConfig = read("src/main/java/galacticwars/clonewars/Config.java");
        String clientConfig = read("src/main/java/galacticwars/clonewars/client/ClientConfig.java");
        String policySync = read("src/main/java/galacticwars/clonewars/network/ServerPolicySync.java");
        String language = read("src/main/resources/assets/galacticwars/lang/en_us.json");
        String notice = read("NOTICE.md");

        assertContains(gradleProperties, "yaclVersion=3.9.5+26.2", "YACL version");
        assertContains(rootBuild, "https://maven.isxander.dev/releases", "YACL Maven repository");
        assertContains(fabricBuild,
                "implementation(\"dev.isxander:yet-another-config-lib:$yaclVersion-fabric\")",
                "Fabric YACL runtime");
        assertContains(neoForgeBuild,
                "implementation(\"dev.isxander:yet-another-config-lib:$yaclVersion-neoforge\")",
                "NeoForge YACL runtime");
        assertContains(fabricMetadata,
                "\"yet_another_config_lib_v3\": \">=${yacl_version}\"",
                "Fabric required YACL dependency");
        assertNotContains(fabricMetadata, "\"recommends\"", "optional Fabric YACL metadata");
        assertContains(neoForgeMetadata, "modId=\"yet_another_config_lib_v3\"",
                "NeoForge YACL dependency");
        assertContains(neoForgeMetadata, "versionRange=\"[${yacl_version},)\"", "YACL version range");
        assertContains(neoForgeMetadata, "side=\"CLIENT\"", "client-only dependency side");
        assertNotContains(neoForgeMetadata, "type=\"optional\"", "optional NeoForge YACL metadata");
        assertContains(fabricClient, "GalacticWarsConfigScreen.create(parent)", "Fabric YACL screen factory");
        assertContains(neoForgeClient, "GalacticWarsConfigScreen.create(parent)", "NeoForge YACL screen factory");
        assertContains(screen, "YetAnotherConfigLib.createBuilder()", "YACL builder");
        assertContains(screen, ".save(ClientConfig::save)", "local client config persistence");
        assertContains(clientConfig, "galacticwars-client.properties", "local config path");
        assertContains(serverConfig, "galacticwars-server.properties", "server policy path");
        assertContains(serverConfig, "galacticwars.properties", "legacy policy migration path");
        assertContains(policySync, "Commands.LEVEL_GAMEMASTERS", "operator-only reload gate");
        assertContains(policySync, "ServerPolicyPayload.current()", "read-only policy synchronization");
        assertNotContains(screen, "Config.ALLOW_", "client mutation of server policy");

        for (String option : new String[]{
                "hud_horizontal_offset",
                "hud_vertical_offset",
                "hud_scale_percent",
                "effect_intensity_percent",
                "particle_density_percent",
                "camera_shake_percent",
                "high_contrast",
                "avoid_color_only",
                "narration_hints"
        }) {
            assertContains(screen, "\"" + option + "\"", option + " binding");
            assertContains(language, "config.galacticwars.option." + option, option + " translation");
        }

        assertContains(notice, "YetAnotherConfigLib", "YACL license notice");
        assertNotContains(fabricBuild + neoForgeBuild,
                "jarJar(\"dev.isxander:yet-another-config-lib", "bundled YACL");

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
