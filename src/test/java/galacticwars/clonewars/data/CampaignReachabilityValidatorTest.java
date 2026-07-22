package galacticwars.clonewars.data;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CampaignReachabilityValidatorTest {
    private CampaignReachabilityValidatorTest() {
    }

    public static void main(String[] args) {
        acceptsDisclosedPhysicalPrerequisites();
        rejectsVehicleBeforeSupplyDepot();
        rejectsAdvancedTradeBeforeThreeDeliveries();
        System.out.println("CampaignReachabilityValidatorTest passed");
    }

    private static void acceptsDisclosedPhysicalPrerequisites() {
        CampaignReachabilityValidator.validate(validCampaign(true), Map.of());
    }

    private static void rejectsVehicleBeforeSupplyDepot() {
        assertThrows(() -> CampaignReachabilityValidator.validate(validCampaign(false), Map.of()),
                "vehicle acquisition without Supply Depot");
    }

    private static void rejectsAdvancedTradeBeforeThreeDeliveries() {
        Map<String, LaunchContentDefinitions.QuestDefinition> quests = validCampaign(true);
        var chapterTwo = new LaunchContentDefinitions.QuestDefinition(
                "test_chapter_2",
                List.of(objective("delivery", "delivery_completed", Set.of(), 1),
                        objective("forward", "building_completed", Set.of("forward_base"), 1),
                        objective("supply", "building_completed", Set.of("supply_depot"), 1),
                        objective("advanced_trade", "trade_completed", Set.of("armorer"), 1),
                        objective("planet", "planet_visited", Set.of("tatooine"), 1)),
                0, Set.of());
        quests = Map.of("test_chapter_1", quests.get("test_chapter_1"),
                "test_chapter_2", chapterTwo, "test_chapter_3", quests.get("test_chapter_3"));
        var trade = new LaunchContentDefinitions.TradeDefinition(
                "armorer", "test", 1, "minecraft:iron_ingot", 1, "advanced_trading");
        Map<String, LaunchContentDefinitions.QuestDefinition> finalQuests = quests;
        assertThrows(() -> CampaignReachabilityValidator.validate(
                finalQuests, Map.of("armorer", trade)), "advanced trade before three deliveries");
    }

    private static Map<String, LaunchContentDefinitions.QuestDefinition> validCampaign(
            boolean includeSupplyDepot
    ) {
        var chapterOne = quest("test_chapter_1", List.of(
                objective("command", "building_completed", Set.of("command_center"), 1),
                objective("camp", "building_completed", Set.of("starter_camp"), 1)));
        java.util.ArrayList<LaunchContentDefinitions.QuestObjectiveDefinition> chapterTwoObjectives =
                new java.util.ArrayList<>(List.of(
                        objective("forward", "building_completed", Set.of("forward_base"), 1)));
        if (includeSupplyDepot) {
            chapterTwoObjectives.add(objective(
                    "supply", "building_completed", Set.of("supply_depot"), 1));
        }
        chapterTwoObjectives.add(objective(
                "planet", "planet_visited", Set.of("tatooine"), 1));
        var chapterTwo = quest("test_chapter_2", chapterTwoObjectives);
        var chapterThree = quest("test_chapter_3", List.of(
                objective("vehicle", "vehicle_acquired", Set.of(), 1)));
        return Map.of(chapterOne.id(), chapterOne, chapterTwo.id(), chapterTwo,
                chapterThree.id(), chapterThree);
    }

    private static LaunchContentDefinitions.QuestDefinition quest(
            String id, List<LaunchContentDefinitions.QuestObjectiveDefinition> objectives
    ) {
        return new LaunchContentDefinitions.QuestDefinition(id, objectives, 0, Set.of());
    }

    private static LaunchContentDefinitions.QuestObjectiveDefinition objective(
            String id, String event, Set<String> subjects, int count
    ) {
        return new LaunchContentDefinitions.QuestObjectiveDefinition(id, event, subjects, count);
    }

    private static void assertThrows(Runnable action, String label) {
        try {
            action.run();
            throw new AssertionError("Expected reachability rejection for " + label);
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
