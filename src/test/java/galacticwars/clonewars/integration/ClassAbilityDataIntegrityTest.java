package galacticwars.clonewars.integration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ClassAbilityDataIntegrityTest {
    private static final Path ROOT = Path.of(
            "src/main/resources/data/galacticwars/galacticwars");

    private ClassAbilityDataIntegrityTest() {
    }

    public static void main(String[] args) throws Exception {
        launchClassesReferenceKnownUnitsAndAbilities();
        everyFactionHasRuntimePolicy();
        forceRuntimeRemainsReserved();
        System.out.println("ClassAbilityDataIntegrityTest passed");
    }

    private static void launchClassesReferenceKnownUnitsAndAbilities() throws Exception {
        String abilityData = Files.readString(ROOT.resolve("abilities/initial.json"));
        String classData = Files.readString(ROOT.resolve("classes/initial.json"));
        Set<String> abilityIds = values(abilityData, "id");
        Set<String> classIds = values(classData, "id");
        Set<String> classUnits = values(classData, "unit");
        assertEquals(30, abilityIds.size(), "ability count");
        assertEquals(15, classIds.size(), "class count");
        assertEquals(15, classUnits.size(), "one class per unit");
        assertEquals(15, count(classData, "\"player_assignable\":true"), "assignable class count");
        for (String abilityReference : arrayValues(classData, "abilities")) {
            assertTrue(abilityIds.contains(abilityReference), "known class ability " + abilityReference);
        }
        assertEquals(30, arrayValues(classData, "abilities").size(), "two abilities per class");
    }

    private static void everyFactionHasRuntimePolicy() throws Exception {
        String policyData = Files.readString(ROOT.resolve("faction_policies/initial.json"));
        Set<String> factions = values(policyData, "faction");
        assertEquals(Set.of(
                "galacticwars:republic",
                "galacticwars:separatist",
                "galacticwars:mandalorian",
                "galacticwars:hutt_cartel",
                "galacticwars:nightsister"), factions, "launch faction policies");
        assertEquals(5, count(policyData, "\"traits\""), "policy traits");
        assertEquals(5, count(policyData, "\"modifiers\""), "policy modifiers");
    }

    private static void forceRuntimeRemainsReserved() throws Exception {
        String abilityData = Files.readString(ROOT.resolve("abilities/initial.json"));
        String classData = Files.readString(ROOT.resolve("classes/initial.json"));
        assertTrue(!abilityData.contains("\"kind\":\"force\""), "no launch Force ability");
        assertContains(classData, "\"id\":\"galacticwars:jedi_guardian\"", "Jedi class exists");
        assertContains(classData, "\"force_path_slot\":\"light\"", "reserved Jedi Force slot");
    }

    private static Set<String> values(String json, String key) {
        HashSet<String> values = new HashSet<>();
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(key)
                + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return Set.copyOf(values);
    }

    private static Set<String> arrayValues(String json, String key) {
        HashSet<String> values = new HashSet<>();
        Matcher arrays = Pattern.compile("\\\"" + Pattern.quote(key)
                + "\\\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(json);
        while (arrays.find()) {
            Matcher entries = Pattern.compile("\\\"([^\\\"]+)\\\"").matcher(arrays.group(1));
            while (entries.find()) {
                values.add(entries.group(1));
            }
        }
        return Set.copyOf(values);
    }

    private static int count(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }

    private static void assertContains(String value, String expected, String label) {
        assertTrue(value.contains(expected), label);
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected " + expected + " but was " + actual);
        }
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label);
        }
    }
}
