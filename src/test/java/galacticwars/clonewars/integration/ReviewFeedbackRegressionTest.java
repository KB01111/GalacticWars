package galacticwars.clonewars.integration;

import java.nio.file.Files;
import java.nio.file.Path;

/** Source contracts for the client and network regressions found during PR review. */
public final class ReviewFeedbackRegressionTest {
    private ReviewFeedbackRegressionTest() {
    }

    public static void main(String[] args) throws Exception {
        commandCenterStateHandlerAllowsMissingClientPlayer();
        vehicleHudUsesStableSynchronizedMaxima();
        recruitFeedbackReturnsToGuidance();

        System.out.println("ReviewFeedbackRegressionTest passed");
    }

    private static void commandCenterStateHandlerAllowsMissingClientPlayer() throws Exception {
        String network = read("src/main/java/galacticwars/clonewars/network/GalacticNetwork.java");
        int handlerStart = network.indexOf("private static void handleCommandCenterState");
        int nextHandler = network.indexOf("private static void handleGameplayCatalog", handlerStart);
        String handler = network.substring(handlerStart, nextHandler);

        assertContains(handler, "var player = context.getPlayer();", "queued client player lookup");
        assertContains(handler, "if (player == null)", "missing-player guard");
        assertContains(handler, "player.containerMenu instanceof CommandCenterOperationsMenu",
                "guarded command-center menu access");
        assertNotContains(handler, "context.getPlayer().containerMenu", "unchecked player dereference");
        assertNotContains(handler, "instanceof ServerPlayer", "invalid S2C server-player cast");
    }

    private static void vehicleHudUsesStableSynchronizedMaxima() throws Exception {
        String hud = read("src/main/java/galacticwars/clonewars/client/gui/VehicleHud.java");
        String vehicle = read("src/main/java/galacticwars/clonewars/vehicle/GalacticVehicleEntity.java");

        assertContains(hud, "vehicle.syncedMaximumHealth()", "synchronized health fallback");
        assertContains(hud, "vehicle.syncedFuelCapacity()", "synchronized fuel fallback");
        assertNotContains(hud, "? Math.max(1, vehicle.health())", "current-health maximum fallback");
        assertNotContains(hud, "? Math.max(1, vehicle.fuel())", "current-fuel maximum fallback");
        assertContains(vehicle, "EntityDataAccessor<Integer> MAXIMUM_HEALTH",
                "synchronized maximum-health field");
        assertContains(vehicle, "EntityDataAccessor<Integer> FUEL_CAPACITY",
                "synchronized fuel-capacity field");
        assertContains(vehicle, "builder.define(MAXIMUM_HEALTH, 40)",
                "maximum-health client default");
        assertContains(vehicle, "builder.define(FUEL_CAPACITY, 1000)",
                "fuel-capacity client default");
        assertContains(vehicle, "output.putInt(\"MaximumHealth\", syncedMaximumHealth())",
                "maximum-health persistence");
        assertContains(vehicle, "output.putInt(\"FuelCapacity\", syncedFuelCapacity())",
                "fuel-capacity persistence");
    }

    private static void recruitFeedbackReturnsToGuidance() throws Exception {
        String screen = read("src/main/java/galacticwars/clonewars/client/gui/RecruitCommandScreen.java");
        int tickStart = screen.indexOf("public void tick()");
        int reset = screen.indexOf("\"screen.galacticwars.recruit.guidance\"", tickStart);
        int stateRefresh = screen.indexOf("if (tame != this.lastTame", tickStart);

        if (tickStart < 0 || reset < tickStart || stateRefresh < 0 || reset > stateRefresh) {
            throw new AssertionError("Recruit feedback must reset when the refresh countdown expires");
        }
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
