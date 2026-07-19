package galacticwars.clonewars.integration;

import java.nio.file.Files;
import java.nio.file.Path;

/** Source contract for server-authored UI content in separate client/server JVMs. */
public final class DedicatedMultiplayerCatalogSyncTest {
    private DedicatedMultiplayerCatalogSyncTest() {
    }

    public static void main(String[] args) throws Exception {
        String payload = read("src/main/java/galacticwars/clonewars/network/GameplayCatalogPayload.java");
        String sync = read("src/main/java/galacticwars/clonewars/network/GameplayCatalogSync.java");
        String network = read("src/main/java/galacticwars/clonewars/network/GalacticNetwork.java");
        String bridge = read("src/main/kotlin/galacticwars/clonewars/network/ClientPacketBridge.kt");
        String clientCatalog = read(
                "src/main/java/galacticwars/clonewars/client/ClientGameplayCatalog.java");
        String clientBootstrap = read("src/main/java/galacticwars/clonewars/GalacticWarsClient.java");
        String classScreen = read(
                "src/main/java/galacticwars/clonewars/client/gui/PlayerClassScreen.java");
        String vehicleHud = read(
                "src/main/java/galacticwars/clonewars/client/gui/VehicleHud.java");

        assertContains(payload, "MAX_CLASSES = 128", "class count bound");
        assertContains(payload, "MAX_VEHICLES = 64", "vehicle count bound");
        assertContains(payload, "MAX_TEXT_BYTES = 192", "UTF-8 text bound");
        assertContains(payload, "size < 0 || size > maximum", "decode list bound");
        assertContains(payload, "requireUnique", "duplicate identifier rejection");
        assertContains(payload, "fromSnapshot", "server projection factory");

        assertContains(sync, "PlayerEvent.PLAYER_JOIN", "login synchronization");
        assertContains(sync, "PlayerEvent.PLAYER_RESPAWN", "respawn synchronization");
        assertContains(sync, "TickEvent.SERVER_POST", "reload generation observation");
        assertContains(sync, "GameplayDataManager.generation()", "reload generation source");
        assertContains(sync, "GalacticNetwork.canPlayerReceive", "payload negotiation guard");
        assertNotContains(sync, "galacticwars.clonewars.client", "server/client class leak");

        assertContains(network, "GameplayCatalogPayload.STREAM_CODEC", "S2C codec registration");
        assertContains(network, "ClientPacketBridge.handleGameplayCatalog(payload)",
                "main-thread client handoff");
        assertNotContains(network, "galacticwars.clonewars.client", "common network client leak");
        assertContains(bridge, "AtomicReference(noGameplayCatalogHandler)",
                "loader-neutral catalog bridge");
        assertContains(clientBootstrap,
                "installGameplayCatalogHandler(ClientGameplayCatalog::replace)",
                "client-only snapshot installation");
        assertContains(clientBootstrap, "CLIENT_PLAYER_QUIT",
                "cross-server snapshot cleanup");

        assertContains(classScreen, "ClientGameplayCatalog.snapshot()",
                "server-authored class selection catalog");
        assertNotContains(classScreen, "GameplayDataManager.snapshot()",
                "client-local class datapack lookup");
        assertContains(classScreen, "displayedCatalogRevision",
                "live reload widget rebuild");
        assertContains(vehicleHud, "ClientGameplayCatalog.snapshot().vehicle",
                "server-authored vehicle HUD maxima");
        assertNotContains(vehicleHud, "maxHealthForHud()",
                "client-local vehicle health maximum");
        assertNotContains(vehicleHud, "fuelCapacityForHud()",
                "client-local vehicle fuel maximum");
        assertContains(clientCatalog, "AtomicReference<Snapshot>",
                "atomic client snapshot replacement");

        System.out.println("DedicatedMultiplayerCatalogSyncTest passed");
    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    private static void assertContains(String value, String expected, String label) {
        if (!value.contains(expected)) {
            throw new AssertionError(label + " missing <" + expected + ">");
        }
    }

    private static void assertNotContains(String value, String forbidden, String label) {
        if (value.contains(forbidden)) {
            throw new AssertionError(label + " contains forbidden <" + forbidden + ">");
        }
    }
}
