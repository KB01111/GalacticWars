package galacticwars.clonewars.integration;

import java.nio.file.Files;
import java.nio.file.Path;

/** Source-level contract test for the two loader attachment adapters. */
public final class PlayerCampaignAttachmentIntegrationTest {
    private static final Path COMMON = Path.of(
            "src/main/kotlin/galacticwars/clonewars/progression/PlayerCampaignAttachmentRuntime.kt");
    private static final Path FABRIC = Path.of(
            "fabric/src/main/kotlin/galacticwars/clonewars/fabric/FabricPlayerCampaignAttachments.kt");
    private static final Path NEOFORGE = Path.of(
            "neoforge/src/main/kotlin/galacticwars/clonewars/neoforge/NeoForgePlayerCampaignAttachments.kt");

    public static void main(String[] args) throws Exception {
        String common = Files.readString(COMMON);
        String fabric = Files.readString(FABRIC);
        String neoForge = Files.readString(NEOFORGE);

        assertContains(common, "ProgressionSavedData.get(level).state(playerId)",
                "SavedData remains campaign authority");
        assertContains(common, "ForceSavedData.get(level).state(playerId)",
                "SavedData remains Force authority");
        assertContains(common, "PlayerEvent.PLAYER_JOIN.register",
                "join synchronization");
        assertContains(common, "PlayerEvent.PLAYER_RESPAWN.register",
                "respawn synchronization");
        assertContains(common, "TickEvent.PLAYER_POST.register",
                "bounded cadence synchronization");
        assertContains(common, "if (access.get(player) == projected)",
                "unchanged projections are not written");
        assertNotContains(common, "toAuthoritative", "attachments cannot reconstruct SavedData");

        assertContains(fabric, ".persistent(PlayerCampaignAttachmentState.CODEC)",
                "Fabric persistence codec");
        assertContains(fabric, ".copyOnDeath()", "Fabric respawn copy");
        assertContains(fabric, "AttachmentSyncPredicate.targetOnly()",
                "Fabric owner-only synchronization");
        assertContains(fabric, "builder.persistent(Codec.LONG)",
                "Fabric mount cooldown persistence");

        assertContains(neoForge, ".serialize(PlayerCampaignAttachmentState.CODEC.fieldOf(\"state\"))",
                "NeoForge persistence codec");
        assertContains(neoForge, ".copyOnDeath()", "NeoForge respawn copy");
        assertContains(neoForge, "holder === recipient",
                "NeoForge owner-only synchronization");
        assertContains(neoForge, ".serialize(Codec.LONG.fieldOf(\"day\"))",
                "NeoForge mount cooldown persistence");

        String mountRuntime = Files.readString(Path.of(
                "src/main/kotlin/galacticwars/clonewars/survival/MountFiberAttachmentRuntime.kt"));
        assertContains(mountRuntime, "fun tryMarkBrushed(horse: AbstractHorse, day: Long): Boolean",
                "loader-neutral mount cooldown check");
        String mountEvent = Files.readString(Path.of(
                "src/main/java/galacticwars/clonewars/survival/MountFiberRecoveryEvents.java"));
        assertContains(mountEvent, "MountFiberAttachmentRuntime.tryMarkBrushed(horse, day)",
                "mount recovery uses cross-loader attachment");
        assertNotContains(mountEvent, "getPersistentData", "NeoForge-only persistent data removed");

        assertEntrypointRegistersBeforeCommon(
                Path.of("fabric/src/main/kotlin/galacticwars/clonewars/fabric/GalacticWarsFabric.kt"),
                "FabricPlayerCampaignAttachments.register()");
        assertEntrypointRegistersBeforeCommon(
                Path.of("neoforge/src/main/kotlin/galacticwars/clonewars/neoforge/GalacticWarsNeoForge.kt"),
                "NeoForgePlayerCampaignAttachments.register(modEventBus)");
        System.out.println("PlayerCampaignAttachmentIntegrationTest passed");
    }

    private static void assertEntrypointRegistersBeforeCommon(Path path, String registration)
            throws Exception {
        String source = Files.readString(path);
        int registrationIndex = source.indexOf(registration);
        int commonIndex = source.indexOf("GalacticWars.init()");
        if (registrationIndex < 0 || commonIndex < 0 || registrationIndex >= commonIndex) {
            throw new AssertionError(path + " must register attachments before common initialization");
        }
    }

    private static void assertContains(String source, String expected, String label) {
        if (!source.contains(expected)) {
            throw new AssertionError(label + " missing: " + expected);
        }
    }

    private static void assertNotContains(String source, String forbidden, String label) {
        if (source.contains(forbidden)) {
            throw new AssertionError(label + " contains forbidden text: " + forbidden);
        }
    }
}
