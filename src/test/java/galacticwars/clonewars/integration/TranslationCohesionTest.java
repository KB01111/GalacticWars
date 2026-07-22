package galacticwars.clonewars.integration;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class TranslationCohesionTest {
    private TranslationCohesionTest() {
    }

    public static void main(String[] args) throws IOException {
        JsonObject language = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/assets/galacticwars/lang/en_us.json"))).getAsJsonObject();
        JsonObject quests = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/data/galacticwars/galacticwars/quests/launch.json")))
                .getAsJsonObject();
        for (JsonElement element : quests.getAsJsonArray("quests")) {
            JsonObject quest = element.getAsJsonObject();
            String questId = quest.get("id").getAsString();
            require(language, "quest.galacticwars." + questId + ".title");
            for (JsonElement objectiveElement : quest.getAsJsonArray("objectives")) {
                String objectiveId = objectiveElement.getAsJsonObject().get("id").getAsString();
                require(language, "screen.galacticwars.operations.objective." + objectiveId);
            }
        }
        System.out.println("TranslationCohesionTest passed");
    }

    private static void require(JsonObject language, String key) {
        if (!language.has(key) || language.get(key).getAsString().isBlank()) {
            throw new AssertionError("Missing localized player-facing key " + key);
        }
    }
}
