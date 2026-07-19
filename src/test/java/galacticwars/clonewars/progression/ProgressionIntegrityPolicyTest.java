package galacticwars.clonewars.progression;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.data.LaunchContentRuntime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ProgressionIntegrityPolicyTest {
    public static void main(String[] args) {
        installContent();
        UUID player = UUID.randomUUID();
        String courier = "courier/" + UUID.randomUUID();
        List<EventCase> valid = new ArrayList<>(List.of(
                valid(ProgressionEventType.CAMPAIGN_RECHECK, "eligible_quests", 0),
                valid(ProgressionEventType.FACTION_PLEDGED, "galacticwars:republic", 1),
                valid(ProgressionEventType.RECRUIT_HIRED, "clone_trooper", 1),
                valid(ProgressionEventType.DELIVERY_COMPLETED, courier, 1),
                valid(ProgressionEventType.PLANET_VISITED, "tatooine", 1),
                valid(ProgressionEventType.VEHICLE_ACQUIRED, "barc_speeder", 1),
                valid(ProgressionEventType.FORCE_ABILITY_UNLOCKED, "light_push", 1),
                valid(ProgressionEventType.QUEST_ADVANCED, "republic_chapter_1", 1),
                valid(ProgressionEventType.CAMPAIGN_COMPLETED, "republic_campaign", 1),
                valid(ProgressionEventType.TRADE_COMPLETED, "republic_quartermaster", 1),
                valid(ProgressionEventType.REGION_CAPTURED, "tatooine_spaceport", 1)));
        for (String profession : List.of("farmer", "lumberjack", "fisherman", "animal_farmer",
                "miner", "builder", "cook", "merchant", "courier")) {
            valid.add(valid(ProgressionEventType.PROFESSION_ASSIGNED, profession, 1));
        }
        for (String building : List.of("command_center", "forward_base", "barracks", "supply_depot",
                "moisture_farm", "salvage_yard", "mine")) {
            valid.add(valid(ProgressionEventType.BUILDING_COMPLETED, "galacticwars:" + building, 1));
        }
        for (EventCase test : valid) {
            assertValidation(player, test.type(), test.subject(), test.amount(), true, "accepted");
            int inflated = test.amount() == 0 ? 1 : test.amount() + 1;
            assertValidation(player, test.type(), test.subject(), inflated,
                    false, "invalid_event_amount");
        }

        for (EventCase test : List.of(
                invalid(ProgressionEventType.CAMPAIGN_RECHECK, "all_quests", "invalid_campaign_recheck"),
                invalid(ProgressionEventType.FACTION_PLEDGED, "galacticwars:invented", "unknown_faction"),
                invalid(ProgressionEventType.RECRUIT_HIRED, "galacticwars:invented", "unknown_recruit"),
                invalid(ProgressionEventType.PROFESSION_ASSIGNED, "senator", "unknown_profession"),
                invalid(ProgressionEventType.DELIVERY_COMPLETED, "courier/not-a-uuid", "unknown_delivery"),
                invalid(ProgressionEventType.BUILDING_COMPLETED, "galacticwars:death_star", "unknown_building"),
                invalid(ProgressionEventType.PLANET_VISITED, "alderaan", "unknown_planet"),
                invalid(ProgressionEventType.VEHICLE_ACQUIRED, "x_wing", "unknown_vehicle"),
                invalid(ProgressionEventType.FORCE_ABILITY_UNLOCKED, "force_win", "unknown_force_ability"),
                invalid(ProgressionEventType.QUEST_ADVANCED, "republic_chapter_99", "unknown_quest"),
                invalid(ProgressionEventType.CAMPAIGN_COMPLETED, "invented_campaign", "unknown_campaign"),
                invalid(ProgressionEventType.TRADE_COMPLETED, "free_credits", "unknown_trade"),
                invalid(ProgressionEventType.REGION_CAPTURED, "whole_galaxy", "unknown_region"))) {
            assertValidation(player, test.type(), test.subject(), test.amount(), false, test.reason());
        }

        assertValidation(player, ProgressionEventType.RECRUIT_HIRED,
                "othermod:clone_trooper", 1, false, "unknown_recruit");
        System.out.println("ProgressionIntegrityPolicyTest passed");
    }

    private static void installContent() {
        var planet = new LaunchContentDefinitions.PlanetDefinition(
                "tatooine", "galacticwars:tatooine", "spaceport", "desert", "hutt_cartel");
        var vehicle = new LaunchContentDefinitions.VehicleDefinition(
                "barc_speeder", "hover", 1, 40, 1200, "vehicle_crafting");
        var force = new LaunchContentDefinitions.ForceAbilityDefinition(
                "light_push", "light", 20, 60, "republic_chapter_1", true);
        var quest = new LaunchContentDefinitions.QuestDefinition(
                "republic_chapter_1", List.of("faction_pledged"), 40, Set.of("workforce"));
        var trade = new LaunchContentDefinitions.TradeDefinition(
                "republic_quartermaster", "republic", 12, "galacticwars:energy_cell", 8,
                "faction_intro");
        var region = new LaunchContentDefinitions.ConquestRegionDefinition(
                "tatooine_spaceport", "tatooine", 48, 1200, 90);
        LaunchContentRuntime.install(new LaunchContentDefinitions(
                        Map.of(planet.id(), planet), Map.of(vehicle.id(), vehicle), Map.of(force.id(), force),
                        Map.of(quest.id(), quest), Map.of(trade.id(), trade), Map.of(region.id(), region)),
                List.of("galacticwars:republic"), Map.of("republic", List.of("clone_trooper")));
    }

    private static void assertValidation(
            UUID player,
            ProgressionEventType type,
            String subject,
            int amount,
            boolean accepted,
            String reason
    ) {
        ProgressionIntegrityPolicy.Validation actual = ProgressionIntegrityPolicy.validate(
                new ProgressionEvent(UUID.randomUUID(), player, type, subject, amount));
        if (actual.accepted() != accepted || !actual.reason().equals(reason)) {
            throw new AssertionError(type + "/" + subject + "/" + amount
                    + " expected " + accepted + "/" + reason
                    + " but was " + actual.accepted() + "/" + actual.reason());
        }
    }

    private static EventCase valid(ProgressionEventType type, String subject, int amount) {
        return new EventCase(type, subject, amount, "accepted");
    }

    private static EventCase invalid(ProgressionEventType type, String subject, String reason) {
        return new EventCase(type, subject, type == ProgressionEventType.CAMPAIGN_RECHECK ? 0 : 1, reason);
    }

    private record EventCase(ProgressionEventType type, String subject, int amount, String reason) {
    }
}
