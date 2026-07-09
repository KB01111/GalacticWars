package middleearth.lotr.warmod.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WorkerWorksiteIntegrationTest {
    private WorkerWorksiteIntegrationTest() {
    }

    public static void main(String[] args) throws IOException {
        menuExposesWorksiteButtons();
        recruitPersistsWorksiteAndUsesWorkCommand();
        screenRendersWorksiteButtons();
        languageContainsWorksiteMessages();

        System.out.println("WorkerWorksiteIntegrationTest passed");
    }

    private static void menuExposesWorksiteButtons() throws IOException {
        String menu = read("src/main/java/middleearth/lotr/warmod/menu/RecruitCommandMenu.java");

        assertContains(menu, "BUTTON_SET_WORKSITE", "set worksite button");
        assertContains(menu, "BUTTON_RETURN_WORKSITE", "return worksite button");
        assertContains(menu, "BUTTON_CLEAR_WORKSITE", "clear worksite button");
    }

    private static void recruitPersistsWorksiteAndUsesWorkCommand() throws IOException {
        String entity = read("src/main/java/middleearth/lotr/warmod/entity/MiddleEarthRecruitEntity.java");
        String actions = read("src/main/java/middleearth/lotr/warmod/recruitment/RecruitmentAction.java");

        assertContains(actions, "WORK_AT_SITE", "work command action");
        assertContains(entity, "workTarget", "work target field");
        assertContains(entity, "\"WorkTargetX\"", "work target save x");
        assertContains(entity, "BUTTON_SET_WORKSITE", "set worksite handling");
        assertContains(entity, "BUTTON_RETURN_WORKSITE", "return worksite handling");
        assertContains(entity, "BUTTON_CLEAR_WORKSITE", "clear worksite handling");
        assertContains(entity, "WorkerTaskPlanner.plan", "task planner hook");
    }

    private static void screenRendersWorksiteButtons() throws IOException {
        String screen = read("src/main/java/middleearth/lotr/warmod/client/gui/RecruitCommandScreen.java");

        assertContains(screen, "screen.kingdomwarsmiddleearth.recruit.worksite.set", "set worksite screen label");
        assertContains(screen, "screen.kingdomwarsmiddleearth.recruit.worksite.return", "return worksite screen label");
        assertContains(screen, "screen.kingdomwarsmiddleearth.recruit.worksite.clear", "clear worksite screen label");
    }

    private static void languageContainsWorksiteMessages() throws IOException {
        String language = read("src/main/resources/assets/kingdomwarsmiddleearth/lang/en_us.json");

        assertContains(language, "\"screen.kingdomwarsmiddleearth.recruit.worksite.set\"", "set label translation");
        assertContains(language, "\"screen.kingdomwarsmiddleearth.recruit.worksite.return\"", "return label translation");
        assertContains(language, "\"screen.kingdomwarsmiddleearth.recruit.worksite.clear\"", "clear label translation");
        assertContains(language, "\"message.kingdomwarsmiddleearth.recruit.worksite.set\"", "set message translation");
        assertContains(language, "\"message.kingdomwarsmiddleearth.recruit.worksite.return\"", "return message translation");
        assertContains(language, "\"message.kingdomwarsmiddleearth.recruit.worksite.missing\"", "missing message translation");
        assertContains(language, "\"message.kingdomwarsmiddleearth.recruit.worksite.clear\"", "clear message translation");
    }

    private static String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    private static void assertContains(String haystack, String needle, String label) {
        if (!haystack.contains(needle)) {
            throw new AssertionError(label + " missing <" + needle + ">");
        }
    }
}
