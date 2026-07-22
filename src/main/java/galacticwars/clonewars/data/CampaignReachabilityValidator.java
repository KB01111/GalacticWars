package galacticwars.clonewars.data;

import galacticwars.clonewars.progression.ProgressionEventType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validates that the campaign instructions expose the prerequisites enforced by runtime services.
 * This intentionally models only player-visible launch gates; it is not a replacement for the
 * authoritative runtime checks.
 */
final class CampaignReachabilityValidator {
    private CampaignReachabilityValidator() {
    }

    static void validate(
            Map<String, LaunchContentDefinitions.QuestDefinition> quests,
            Map<String, LaunchContentDefinitions.TradeDefinition> trades
    ) {
        Set<String> factionPaths = new HashSet<>();
        quests.keySet().stream().map(CampaignReachabilityValidator::campaignPath)
                .filter(path -> !path.isBlank()).forEach(factionPaths::add);
        for (String factionPath : factionPaths) {
            List<LaunchContentDefinitions.QuestDefinition> chapters = quests.values().stream()
                    .filter(quest -> quest.id().startsWith(factionPath + "_chapter_"))
                    .sorted(Comparator.comparingInt(quest -> chapterNumber(quest.id())))
                    .toList();
            if (chapters.size() != 3) {
                throw new IllegalArgumentException("Campaign " + factionPath
                        + " must declare exactly three launch chapters");
            }
            validateCampaign(factionPath, chapters, trades);
        }
    }

    private static void validateCampaign(
            String factionPath,
            List<LaunchContentDefinitions.QuestDefinition> chapters,
            Map<String, LaunchContentDefinitions.TradeDefinition> trades
    ) {
        Set<String> availableUnlocks = new HashSet<>();
        int deliveries = 0;
        for (LaunchContentDefinitions.QuestDefinition chapter : chapters) {
            for (LaunchContentDefinitions.QuestObjectiveDefinition objective : chapter.objectives()) {
                ProgressionEventType event = ProgressionEventType.valueOf(
                        objective.eventType().toUpperCase(Locale.ROOT));
                if (event == ProgressionEventType.PLANET_VISITED
                        && !availableUnlocks.contains("planet_travel")) {
                    fail(factionPath, chapter.id(), objective.id(), "planet_travel");
                }
                if (event == ProgressionEventType.VEHICLE_ACQUIRED
                        && !availableUnlocks.contains("vehicle_crafting")) {
                    fail(factionPath, chapter.id(), objective.id(), "vehicle_crafting");
                }
                if (event == ProgressionEventType.TRADE_COMPLETED) {
                    for (String subject : objective.subjectIds()) {
                        LaunchContentDefinitions.TradeDefinition trade = trades.get(path(subject));
                        if (trade != null && !availableUnlocks.contains(trade.requiredUnlock())) {
                            fail(factionPath, chapter.id(), objective.id(), trade.requiredUnlock());
                        }
                    }
                }

                if (event == ProgressionEventType.BUILDING_COMPLETED) {
                    for (String building : objective.subjectIds()) {
                        switch (path(building)) {
                            case "command_center" -> availableUnlocks.addAll(Set.of(
                                    "treasury", "recruitment"));
                            case "starter_camp" -> availableUnlocks.addAll(Set.of(
                                    "workforce", "army_command", "formation_basic"));
                            case "barracks" -> availableUnlocks.addAll(Set.of(
                                    "formation_advanced", "larger_squads"));
                            case "forward_base" -> availableUnlocks.addAll(Set.of(
                                    "commander", "planet_travel", "simultaneous_orders"));
                            case "supply_depot" -> availableUnlocks.addAll(Set.of(
                                    "vehicle_crafting", "sustained_march", "field_resupply"));
                            default -> {
                            }
                        }
                    }
                } else if (event == ProgressionEventType.DELIVERY_COMPLETED) {
                    deliveries += objective.requiredCount();
                    if (deliveries >= 3) {
                        availableUnlocks.add("advanced_trading");
                    }
                }
            }
            availableUnlocks.add(chapter.id());
            availableUnlocks.addAll(chapter.unlocks());
        }
    }

    private static void fail(String campaign, String quest, String objective, String prerequisite) {
        throw new IllegalArgumentException("Campaign " + campaign + " quest " + quest
                + " objective " + objective + " hides required prerequisite " + prerequisite);
    }

    private static String campaignPath(String questId) {
        int marker = questId.lastIndexOf("_chapter_");
        return marker <= 0 ? "" : questId.substring(0, marker);
    }

    private static int chapterNumber(String questId) {
        return Integer.parseInt(questId.substring(questId.lastIndexOf('_') + 1));
    }

    private static String path(String id) {
        int separator = id.indexOf(':');
        return separator < 0 ? id : id.substring(separator + 1);
    }
}
