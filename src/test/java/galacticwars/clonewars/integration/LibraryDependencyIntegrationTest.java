package galacticwars.clonewars.integration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;

public final class LibraryDependencyIntegrationTest {
    private LibraryDependencyIntegrationTest() {
    }

    public static void main(String[] args) throws Exception {
        String rootBuild = Files.readString(Path.of("build.gradle.kts"));
        String commonBuild = Files.readString(Path.of("common/build.gradle.kts"));
        String fabricBuild = Files.readString(Path.of("fabric/build.gradle.kts"));
        String neoForgeBuild = Files.readString(Path.of("neoforge/build.gradle.kts"));
        String properties = Files.readString(Path.of("gradle.properties"));
        String fabricMetadata = Files.readString(Path.of("fabric/src/main/resources/fabric.mod.json"));
        String neoForgeMetadata = Files.readString(
                Path.of("neoforge/src/main/resources/META-INF/neoforge.mods.toml"));
        String notice = Files.readString(Path.of("NOTICE.md"));

        assertContains(rootBuild, "id(\"architectury-plugin\") version \"3.5.169\"",
                "Architectury build plugin");
        assertContains(commonBuild, "api(\"dev.architectury:architectury:$architecturyApiVersion\")",
                "common Architectury API coordinate");
        assertContains(fabricBuild,
                "implementation(\"net.tslat:smartbrainlib-fabric-26.2:$smartBrainLibVersion\")",
                "Fabric SmartBrainLib coordinate");
        assertContains(neoForgeBuild,
                "implementation(\"net.tslat:smartbrainlib-neoforge-26.2:$smartBrainLibVersion\")",
                "NeoForge SmartBrainLib coordinate");
        assertContains(properties, "architecturyApiVersion=21.0.5", "Architectury API pin");
        assertContains(properties, "smartBrainLibVersion=2.0.0", "SmartBrainLib pin");
        assertContains(fabricMetadata, "\"architectury\": \">=${architectury_api_version}\"",
                "Fabric Architectury required metadata");
        assertContains(fabricMetadata, "\"smartbrainlib\": \">=${smartbrainlib_version}\"",
                "Fabric SmartBrainLib required metadata");
        assertContains(neoForgeMetadata, "modId=\"architectury\"",
                "NeoForge Architectury required metadata");
        assertContains(neoForgeMetadata, "modId=\"smartbrainlib\"",
                "NeoForge SmartBrainLib required metadata");
        assertContains(neoForgeMetadata, "versionRange=\"[${smartbrainlib_version},3.0.0)\"",
                "SmartBrainLib compatibility range");
        assertContains(notice, "MPL-2.0", "SmartBrainLib license notice");
        assertNotContains(rootBuild + commonBuild + fabricBuild + neoForgeBuild
                        + properties + fabricMetadata + neoForgeMetadata + notice,
                "MrCrayfish", "removed networking dependency");

        String version = property(properties, "modVersion");
        Path artifact = Path.of("neoforge/build/libs/galacticwars-neoforge-" + version + ".jar");
        if (!Files.isRegularFile(artifact)) {
            throw new AssertionError("built NeoForge Galactic Wars JAR missing <" + artifact + ">");
        }
        try (JarFile jar = new JarFile(artifact.toFile())) {
            if (jar.getEntry("META-INF/jarjar/metadata.json") != null) {
                throw new AssertionError("External libraries must not be embedded with jarJar");
            }
        }
        System.out.println("LibraryDependencyIntegrationTest passed");
    }

    private static void assertContains(String value, String expected, String label) {
        if (!value.contains(expected)) {
            throw new AssertionError(label + " missing <" + expected + ">");
        }
    }

    private static void assertNotContains(String value, String unexpected, String label) {
        if (value.contains(unexpected)) {
            throw new AssertionError(label + " unexpectedly contained <" + unexpected + ">");
        }
    }

    private static String property(String properties, String key) {
        return properties.lines()
                .filter(line -> line.startsWith(key + "="))
                .map(line -> line.substring(key.length() + 1).trim())
                .findFirst()
                .orElseThrow(() -> new AssertionError("property missing <" + key + ">"));
    }
}
