package galacticwars.clonewars.progression;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LaunchContentCatalogTest {
    public static void main(String[] args) {
        var quest = new LaunchContentDefinitions.QuestDefinition(
                "republic_chapter_2", List.of("delivery_completed"), 70,
                Set.of("barc_speeder", "force_path"));
        var definitions = new LaunchContentDefinitions(
                Map.of(), Map.of(), Map.of(), Map.of(quest.id(), quest), Map.of(), Map.of());
        assertEquals(Set.of("barc_speeder", "force_path"),
                definitions.questUnlocks("republic_chapter_2"), "datapack quest unlocks");
        assertEquals(70, definitions.questRewardCredits("republic_chapter_2"), "datapack quest reward");
        var force = new LaunchContentDefinitions.ForceAbilityDefinition(
                "light_push", "light", 20, 60, "republic_chapter_2", true);
        assertEquals(true, force.enabled(), "Force runtime enabled by launch content");
        assertThrows(() -> new LaunchContentDefinitions.ForceAbilityDefinition(
                "broken", "neutral", 20, 60, "republic_chapter_2", true),
                "unknown Force path rejected");
        assertNullQuestCollectionRejected(null, Set.of(), "objectives for quest broken");
        assertNullQuestCollectionRejected(List.of("command_center"), null, "unlocks for quest broken");
        assertThrows(() -> new LaunchContentDefinitions.PlanetDefinition(
                "a".repeat(LaunchContentDefinitions.MAX_SERIALIZED_PLANET_ID_BYTES + 1),
                "galacticwars:test", "arrival", "theme", "republic"),
                "oversized planet id rejected");
        assertThrows(() -> new LaunchContentDefinitions.TradeDefinition(
                        "oversized_amount", "republic", 1, "minecraft:stone",
                        LaunchContentDefinitions.MAX_TRADE_ITEM_COUNT + 1, "faction_intro"),
                "oversized trade amount rejected");
        assertThrows(() -> new LaunchContentDefinitions.TradeDefinition(
                        "oversized_price", "republic",
                        LaunchContentDefinitions.MAX_TRADE_CREDIT_PRICE + 1,
                        "minecraft:stone", 1, "faction_intro"),
                "oversized trade price rejected");
        assertThrows(() -> new LaunchContentDefinitions.TradeDefinition(
                        "x".repeat(LaunchContentDefinitions.MAX_SERIALIZED_TRADE_TEXT_BYTES + 1),
                        "republic", 1, "minecraft:stone", 1, "faction_intro"),
                "oversized trade id rejected");
        System.out.println("LaunchContentCatalogTest passed");
    }

    private static void assertThrows(Runnable action, String label) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(label);
    }

    private static void assertNullQuestCollectionRejected(
            List<String> objectives,
            Set<String> unlocks,
            String expectedMessage
    ) {
        try {
            new LaunchContentDefinitions.QuestDefinition("broken", objectives, 0, unlocks);
            throw new AssertionError("Null quest collection was accepted");
        } catch (NullPointerException expected) {
            assertEquals(expectedMessage, expected.getMessage(), "null quest collection diagnostic");
        }
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) throw new AssertionError(label + " expected " + expected + " but was " + actual);
    }
}
