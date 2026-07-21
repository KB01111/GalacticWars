package galacticwars.clonewars.vehicle;

import galacticwars.clonewars.data.CoreContentBindings;
import galacticwars.clonewars.economy.CreditTransactionService;
import galacticwars.clonewars.kingdom.KingdomPermission;
import galacticwars.clonewars.progression.LaunchContentCatalog;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.settlement.CommandCenterBlockEntity;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class VehicleFabricationService {
    private VehicleFabricationService() {
    }

    public static String fabricate(
            ServerPlayer player, CommandCenterBlockEntity hall, String vehicleId
    ) {
        String normalizedVehicleId = Objects.requireNonNullElse(vehicleId, "").trim();
        FabricationAssessment assessment = assess(player, hall, normalizedVehicleId);
        if (!assessment.available()) return assessment.reason();
        ServerLevel level = player.level();
        var definition = LaunchContentCatalog.data().vehicles().get(normalizedVehicleId);
        Map<Item, Integer> inputs = resolveInputs(definition.fabricationInputs());
        if (!CreditTransactionService.withdrawContainer(hall, definition.fabricationCredits())) {
            return "insufficient_credits";
        }
        inputs.forEach((item, count) -> consume(hall, item, count));
        ItemStack kit = new ItemStack(Objects.requireNonNull(kit(normalizedVehicleId)));
        player.getInventory().add(kit);
        if (!kit.isEmpty()) player.spawnAtLocation(level, kit);
        return "accepted";
    }

    /** Server-authoritative, side-effect-free fabrication preflight used by the operations UI. */
    public static FabricationAssessment assess(
            ServerPlayer player, CommandCenterBlockEntity hall, String vehicleId
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(hall, "hall");
        vehicleId = Objects.requireNonNullElse(vehicleId, "").trim();
        if (!(player.level() instanceof ServerLevel level)) {
            return FabricationAssessment.rejected(vehicleId, "server_only", 0, List.of());
        }
        var definition = LaunchContentCatalog.data().vehicles().get(vehicleId);
        if (definition == null || kit(vehicleId) == null) {
            return FabricationAssessment.rejected(vehicleId, "unknown_vehicle", 0, List.of());
        }
        Map<Item, Integer> inputs = resolveInputs(definition.fabricationInputs());
        List<MaterialRequirement> materials = inputs.entrySet().stream()
                .map(entry -> new MaterialRequirement(
                        BuiltInRegistries.ITEM.getKey(entry.getKey()).toString(),
                        entry.getValue(), count(hall, entry.getKey())))
                .sorted(java.util.Comparator.comparing(MaterialRequirement::itemId))
                .toList();
        var state = ProgressionSavedData.get(level).state(player.getUUID());
        String requirementFailure = definition.deploymentRequirements().stream()
                .filter(requirement -> !requirementSatisfied(state, requirement))
                .map(requirement -> requirement.equals("supply_depot")
                        ? "supply_depot_required" : "vehicle_quest_locked")
                .findFirst().orElse("accepted");
        String reason = availabilityReason(
                hall.canUse(player, KingdomPermission.USE_STORAGE),
                hall.upkeepPaid(),
                !inputs.isEmpty(),
                requirementFailure,
                CreditTransactionService.containerBalance(hall),
                definition.fabricationCredits(),
                materials.stream().allMatch(MaterialRequirement::satisfied));
        return new FabricationAssessment(
                vehicleId, reason.equals("accepted"), reason,
                definition.fabricationCredits(), materials);
    }

    static String availabilityReason(
            boolean permitted,
            boolean upkeepPaid,
            boolean recipeValid,
            String requirementFailure,
            int availableCredits,
            int requiredCredits,
            boolean materialsAvailable
    ) {
        if (!permitted) return "permission_denied";
        if (!upkeepPaid) return "upkeep_unpaid";
        if (!Objects.requireNonNull(requirementFailure, "requirementFailure").equals("accepted")) {
            return requirementFailure;
        }
        if (!recipeValid) return "invalid_recipe";
        if (availableCredits < requiredCredits) return "insufficient_credits";
        if (!materialsAvailable) return "missing_materials";
        return "accepted";
    }

    private static Map<Item, Integer> resolveInputs(Map<String, Integer> configured) {
        LinkedHashMap<Item, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : configured.entrySet()) {
            if (entry.getValue() == null || entry.getValue() < 1) return Map.of();
            Identifier id;
            try {
                id = Identifier.parse(entry.getKey());
            } catch (RuntimeException invalid) {
                return Map.of();
            }
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item == null || !BuiltInRegistries.ITEM.getKey(item).equals(id)) return Map.of();
            result.put(item, entry.getValue());
        }
        return Map.copyOf(result);
    }

    private static boolean requirementSatisfied(
            galacticwars.clonewars.progression.ProgressionState state, String requirement
    ) {
        return requirement.equals("supply_depot")
                ? state.hasSubjectPath(ProgressionEventType.BUILDING_COMPLETED, "supply_depot")
                : state.unlocks().contains(requirement)
                || state.hasSubject(ProgressionEventType.QUEST_ADVANCED, requirement);
    }

    private static Item kit(String id) {
        CoreContentBindings.VehicleBinding binding = CoreContentBindings.vehicles().get(id);
        if (binding == null) {
            return null;
        }
        Identifier itemId = Identifier.parse(binding.deploymentKitItemId());
        Item item = BuiltInRegistries.ITEM.getValue(itemId);
        return item != null && itemId.equals(BuiltInRegistries.ITEM.getKey(item)) ? item : null;
    }

    private static int count(CommandCenterBlockEntity hall, Item item) {
        int total = 0;
        for (int slot = 0; slot < hall.getContainerSize(); slot++) {
            if (hall.getItem(slot).is(item)) total += hall.getItem(slot).getCount();
        }
        return total;
    }

    private static void consume(CommandCenterBlockEntity hall, Item item, int amount) {
        int remaining = amount;
        for (int slot = 0; slot < hall.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = hall.getItem(slot);
            if (!stack.is(item)) continue;
            int removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            remaining -= removed;
        }
        hall.setChanged();
    }

    public record FabricationAssessment(
            String vehicleId,
            boolean available,
            String reason,
            int requiredCredits,
            List<MaterialRequirement> materials
    ) {
        private static final int MAX_MATERIALS = 16;

        public FabricationAssessment {
            vehicleId = Objects.requireNonNullElse(vehicleId, "").trim();
            reason = Objects.requireNonNull(reason, "reason").trim();
            materials = List.copyOf(Objects.requireNonNull(materials, "materials"));
            if (reason.isEmpty() || requiredCredits < 0 || materials.size() > MAX_MATERIALS) {
                throw new IllegalArgumentException("invalid fabrication assessment");
            }
            if (available != reason.equals("accepted")) {
                throw new IllegalArgumentException("fabrication availability and reason must agree");
            }
        }

        private static FabricationAssessment rejected(
                String vehicleId, String reason, int requiredCredits,
                List<MaterialRequirement> materials
        ) {
            return new FabricationAssessment(
                    vehicleId, false, reason, requiredCredits, materials);
        }
    }

    public record MaterialRequirement(
            String itemId,
            int required,
            int available
    ) {
        public MaterialRequirement {
            itemId = Objects.requireNonNull(itemId, "itemId").trim();
            if (itemId.isEmpty() || required < 1 || available < 0) {
                throw new IllegalArgumentException("invalid fabrication material requirement");
            }
        }

        public boolean satisfied() {
            return available >= required;
        }
    }
}
