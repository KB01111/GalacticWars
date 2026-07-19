package galacticwars.clonewars.vehicle;

public final class VehicleFabricationServiceTest {
    private VehicleFabricationServiceTest() {
    }

    public static void main(String[] args) {
        requireReason("permission_denied", false, true, true, "accepted", 100, 32, true);
        requireReason("upkeep_unpaid", true, false, true, "accepted", 100, 32, true);
        requireReason("supply_depot_required", true, true, true,
                "supply_depot_required", 100, 32, true);
        requireReason("vehicle_quest_locked", true, true, true,
                "vehicle_quest_locked", 100, 32, true);
        requireReason("invalid_recipe", true, true, false, "accepted", 100, 32, true);
        requireReason("insufficient_credits", true, true, true, "accepted", 31, 32, true);
        requireReason("missing_materials", true, true, true, "accepted", 32, 32, false);
        requireReason("accepted", true, true, true, "accepted", 32, 32, true);
        System.out.println("VehicleFabricationServiceTest passed");
    }

    private static void requireReason(
            String expected,
            boolean permitted,
            boolean upkeepPaid,
            boolean recipeValid,
            String requirementFailure,
            int availableCredits,
            int requiredCredits,
            boolean materialsAvailable
    ) {
        String actual = VehicleFabricationService.availabilityReason(
                permitted, upkeepPaid, recipeValid, requirementFailure,
                availableCredits, requiredCredits, materialsAvailable);
        if (!actual.equals(expected)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }
}
