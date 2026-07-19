package galacticwars.clonewars.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GeckoLibDependencyMetadataTest {
    private GeckoLibDependencyMetadataTest() {
    }

    public static void main(String[] args) throws IOException {
        String gradleProperties = read("gradle.properties");
        String rootBuild = read("build.gradle.kts");
        String fabricBuild = read("fabric/build.gradle.kts");
        String neoForgeBuild = read("neoforge/build.gradle.kts");
        String fabricMetadata = read("fabric/src/main/resources/fabric.mod.json");
        String neoForgeMetadata = read("neoforge/src/main/resources/META-INF/neoforge.mods.toml");
        String notice = read("NOTICE.md");
        String readme = read("README.md");

        assertContains(gradleProperties, "geckoLibVersion=5.5.3", "GeckoLib version property");
        assertContains(rootBuild, "https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/", "GeckoLib Maven repository");
        assertContains(fabricBuild,
                "implementation(\"com.geckolib:geckolib-fabric-26.2:$geckoLibVersion\")",
                "GeckoLib Fabric dependency");
        assertContains(neoForgeBuild,
                "implementation(\"com.geckolib:geckolib-neoforge-26.2:$geckoLibVersion\")",
                "GeckoLib NeoForge dependency");
        assertContains(fabricMetadata, "\"geckolib\": \">=${geckolib_version}\"",
                "Fabric GeckoLib metadata dependency");
        assertContains(neoForgeMetadata, "modId=\"geckolib\"",
                "NeoForge GeckoLib metadata dependency");
        assertContains(neoForgeMetadata, "versionRange=\"[${geckolib_version},)\"",
                "GeckoLib metadata version range");
        assertContains(notice, "GeckoLib", "GeckoLib notice");
        assertContains(readme, "GeckoLib", "GeckoLib README");

        System.out.println("GeckoLibDependencyMetadataTest passed");
    }

    private static String read(String relativePath) throws IOException {
        return Files.readString(Path.of(relativePath));
    }

    private static void assertContains(String haystack, String needle, String label) {
        if (!haystack.contains(needle)) {
            throw new AssertionError(label + " missing <" + needle + ">");
        }
    }
}
