package galacticwars.clonewars.network;

import java.util.Collections;
import java.util.List;

public final class GameplayCatalogPayloadValidationTest {
    private GameplayCatalogPayloadValidationTest() {
    }

    public static void main(String[] args) {
        var requirement = new GameplayCatalogPayload.RequirementEntry("quest", "chapter1", 1);
        var unitClass = new GameplayCatalogPayload.ClassEntry(
                "galacticwars:clone_trooper",
                "Clone Trooper",
                "galacticwars:republic",
                "",
                List.of(requirement),
                List.of("Suppressive Fire", "Rally"));
        var vehicle = new GameplayCatalogPayload.VehicleEntry("laat_gunship", 180, 2400);
        var payload = new GameplayCatalogPayload(4L, List.of(unitClass), List.of(vehicle));

        if (payload.generation() != 4L
                || !payload.classes().getFirst().classId().equals("galacticwars:clone_trooper")
                || payload.vehicles().getFirst().fuelCapacity() != 2400) {
            throw new AssertionError("valid gameplay catalog payload did not retain its projection");
        }
        expectFailure(() -> new GameplayCatalogPayload(-1L, List.of(), List.of()),
                "negative generation");
        expectFailure(() -> new GameplayCatalogPayload(
                        1L, List.of(unitClass, unitClass), List.of()),
                "duplicate class id");
        expectFailure(() -> new GameplayCatalogPayload(
                        1L,
                        Collections.nCopies(GameplayCatalogPayload.MAX_CLASSES + 1, unitClass),
                        List.of()),
                "class count bound");
        expectFailure(() -> new GameplayCatalogPayload.ClassEntry(
                        "galacticwars:invalid",
                        "x".repeat(GameplayCatalogPayload.MAX_TEXT_BYTES + 1),
                        "galacticwars:republic",
                        "",
                        List.of(),
                        List.of()),
                "text byte bound");
        expectFailure(() -> new GameplayCatalogPayload.RequirementEntry(
                        "quest", "chapter1", GameplayCatalogPayload.MAX_REQUIREMENT_AMOUNT + 1),
                "requirement amount bound");
        expectFailure(() -> new GameplayCatalogPayload.VehicleEntry(
                        "laat_gunship", GameplayCatalogPayload.MAX_VEHICLE_STAT + 1, 1),
                "vehicle maximum bound");

        try {
            payload.classes().add(unitClass);
            throw new AssertionError("payload class list is mutable");
        } catch (UnsupportedOperationException expected) {
            // Expected immutable projection.
        }

        System.out.println("GameplayCatalogPayloadValidationTest passed");
    }

    private static void expectFailure(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError(label + " was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected validation failure.
        }
    }
}
