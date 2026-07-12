package galacticwars.clonewars.progression;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class LaunchDataIntegrityTest {
    private static final Path ROOT = Path.of("src/main/resources/data/galacticwars");
    private static final Path GAMEPLAY = ROOT.resolve("galacticwars");

    public static void main(String[] args) throws Exception {
        assertJsonCount(GAMEPLAY.resolve("factions"), 5, "factions");
        assertJsonCount(GAMEPLAY.resolve("units"), 15, "units");
        for (String category : Set.of("planets", "vehicles", "force_abilities", "quests", "trades", "conquest_regions")) {
            assertTrue(Files.isRegularFile(GAMEPLAY.resolve(category).resolve("launch.json")), category + " launch data");
        }
        String planets = Files.readString(GAMEPLAY.resolve("planets/launch.json"));
        Set<String> planetIds = ids(planets);
        assertTrue(planetIds.size() == 4, "planet count");
        for (String planet : planetIds) {
            assertTrue(Files.isRegularFile(ROOT.resolve("dimension").resolve(planet + ".json")), planet + " dimension");
        }
        assertTrue(Files.isRegularFile(ROOT.resolve("dimension_type/planet.json")), "planet dimension type");
        String quests = Files.readString(GAMEPLAY.resolve("quests/launch.json"));
        Set<String> questIds = ids(quests);
        assertTrue(questIds.size() == 15, "quest count");
        assertTrue(!quests.contains("\"objectives\":[\"force_ability_unlocked\""),
                "launch campaign cannot require disabled Force runtime");
        for (String quest : questIds) {
            assertTrue(quests.contains("\"id\":\"" + quest + "\""), quest);
        }
        Map<String, Set<String>> declaredUnlocks = new HashMap<>();
        Matcher questMatcher = Pattern.compile(
                "\\{\"id\":\"([^\"]+)\".*?\"unlocks\":\\[([^]]*)]}").matcher(quests);
        while (questMatcher.find()) {
            HashSet<String> unlocks = new HashSet<>();
            Matcher unlockMatcher = Pattern.compile("\"([^\"]+)\"").matcher(questMatcher.group(2));
            while (unlockMatcher.find()) {
                unlocks.add(unlockMatcher.group(1));
            }
            declaredUnlocks.put(questMatcher.group(1), Set.copyOf(unlocks));
        }
        assertTrue(declaredUnlocks.size() == 15, "quest unlock declarations");
        System.out.println("LaunchDataIntegrityTest passed");
    }

    private static void assertJsonCount(Path directory, long expected, String label) throws Exception {
        try (Stream<Path> files = Files.list(directory)) {
            assertTrue(files.filter(path -> path.toString().endsWith(".json")).count() == expected, label);
        }
    }

    private static Set<String> ids(String json) {
        HashSet<String> ids = new HashSet<>();
        Matcher matcher = Pattern.compile("\\\"id\\\":\\\"([^\\\"]+)\\\"").matcher(json);
        while (matcher.find()) ids.add(matcher.group(1));
        return Set.copyOf(ids);
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) throw new AssertionError(label);
    }
}
