package middleearth.lotr.warmod.world;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import middleearth.lotr.warmod.faction.FactionId;

public final class MiddleEarthRegionCatalogTest {
    private MiddleEarthRegionCatalogTest() {
    }

    public static void main(String[] args) {
        normalizesRegionIds();
        storesRegionDefinitionValues();
        looksUpRegionsByIdFactionAndClimate();
        rejectsDuplicateRegionIds();
        rejectsInvalidRegionValues();

        System.out.println("MiddleEarthRegionCatalogTest passed");
    }

    private static void normalizesRegionIds() {
        assertEquals("kingdomwarsmiddleearth:gondor", MiddleEarthRegionId.of("Gondor").toString(),
                "default namespace region id");
        assertEquals("kingdomwarsmiddleearth:mordor", MiddleEarthRegionId.of("kingdomwarsmiddleearth:Mordor").toString(),
                "explicit namespace region id");
    }

    private static void storesRegionDefinitionValues() {
        MiddleEarthRegionDefinition gondor = gondor();

        assertEquals(MiddleEarthRegionId.of("gondor"), gondor.id(), "region id");
        assertEquals("Gondor", gondor.displayName(), "region display name");
        assertEquals(FactionId.of("gondor"), gondor.controllingFaction(), "region controlling faction");
        assertEquals(MiddleEarthRegionClimate.TEMPERATE, gondor.climate(), "region climate");
        assertEquals(0.8F, gondor.baseTemperature(), "region base temperature");
        assertEquals(0.4F, gondor.downfall(), "region downfall");
        assertEquals(10, gondor.spawnWeight(), "region spawn weight");
        assertTrue(gondor.features().contains("white_city_outskirts"), "region feature");
    }

    private static void looksUpRegionsByIdFactionAndClimate() {
        MiddleEarthRegionCatalog catalog = testCatalog();

        Optional<MiddleEarthRegionDefinition> gondor = catalog.definition(MiddleEarthRegionId.of("gondor"));
        assertTrue(gondor.isPresent(), "gondor lookup");
        assertEquals("Gondor", gondor.orElseThrow().displayName(), "gondor lookup name");

        List<MiddleEarthRegionDefinition> rohanRegions = catalog.regionsForFaction(FactionId.of("rohan"));
        assertEquals(1, rohanRegions.size(), "rohan region count");
        assertEquals(MiddleEarthRegionId.of("rohan"), rohanRegions.get(0).id(), "rohan region id");

        List<MiddleEarthRegionDefinition> shadowRegions =
                catalog.regionsForClimate(MiddleEarthRegionClimate.SHADOW);
        assertEquals(1, shadowRegions.size(), "shadow region count");
        assertEquals(MiddleEarthRegionId.of("mordor"), shadowRegions.get(0).id(), "shadow region id");
    }

    private static void rejectsDuplicateRegionIds() {
        assertThrows(IllegalArgumentException.class, () -> new MiddleEarthRegionCatalog(List.of(gondor(), gondor())),
                "duplicate region ids");
    }

    private static void rejectsInvalidRegionValues() {
        assertThrows(IllegalArgumentException.class, () -> new MiddleEarthRegionDefinition(
                MiddleEarthRegionId.of("ithilien"),
                "Ithilien",
                FactionId.of("gondor"),
                MiddleEarthRegionClimate.WOODLAND,
                -0.1F,
                0.6F,
                4,
                Set.of("crossroads")), "negative temperature");
        assertThrows(IllegalArgumentException.class, () -> new MiddleEarthRegionDefinition(
                MiddleEarthRegionId.of("dead_marshes"),
                "Dead Marshes",
                FactionId.of("mordor"),
                MiddleEarthRegionClimate.SHADOW,
                0.5F,
                0.9F,
                -1,
                Set.of("marsh_lights")), "negative spawn weight");
        assertThrows(IllegalArgumentException.class, () -> new MiddleEarthRegionDefinition(
                MiddleEarthRegionId.of("fangorn"),
                "Fangorn",
                FactionId.of("rohan"),
                MiddleEarthRegionClimate.WOODLAND,
                0.7F,
                0.8F,
                3,
                Set.of(" ")), "blank feature name");
    }

    private static MiddleEarthRegionCatalog testCatalog() {
        return new MiddleEarthRegionCatalog(List.of(
                gondor(),
                new MiddleEarthRegionDefinition(
                        MiddleEarthRegionId.of("rohan"),
                        "Rohan",
                        FactionId.of("rohan"),
                        MiddleEarthRegionClimate.PLAINS,
                        0.7F,
                        0.3F,
                        8,
                        Set.of("horse_lords_plains", "thatched_villages")),
                new MiddleEarthRegionDefinition(
                        MiddleEarthRegionId.of("mordor"),
                        "Mordor",
                        FactionId.of("mordor"),
                        MiddleEarthRegionClimate.SHADOW,
                        1.0F,
                        0.0F,
                        6,
                        Set.of("ash_wastes", "black_gate"))));
    }

    private static MiddleEarthRegionDefinition gondor() {
        return new MiddleEarthRegionDefinition(
                MiddleEarthRegionId.of("gondor"),
                "Gondor",
                FactionId.of("gondor"),
                MiddleEarthRegionClimate.TEMPERATE,
                0.8F,
                0.4F,
                10,
                Set.of("white_city_outskirts", "beacon_hills"));
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertEquals(float expected, float actual, String label) {
        if (Float.compare(expected, actual) != 0) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label + " expected to be true");
        }
    }

    private static <T extends Throwable> void assertThrows(Class<T> expectedType, ThrowingRunnable runnable, String label) {
        try {
            runnable.run();
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                return;
            }
            throw new AssertionError(label + " threw " + throwable.getClass().getName() + " instead of "
                    + expectedType.getName(), throwable);
        }

        throw new AssertionError(label + " did not throw " + expectedType.getName());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run();
    }
}
