package middleearth.lotr.warmod.army;

import java.util.List;
import java.util.Optional;

import middleearth.lotr.warmod.faction.FactionId;

public final class ArmyUnitCatalogTest {
    private ArmyUnitCatalogTest() {
    }

    public static void main(String[] args) {
        normalizesUnitIds();
        storesUnitDefinitionValues();
        looksUpUnitsByIdFactionAndRole();
        rejectsDuplicateUnitIds();

        System.out.println("ArmyUnitCatalogTest passed");
    }

    private static void normalizesUnitIds() {
        assertEquals("kingdomwarsmiddleearth:gondor_soldier", ArmyUnitId.of("Gondor_Soldier").toString(),
                "default namespace unit id");
        assertEquals("kingdomwarsmiddleearth:rohan_rider", ArmyUnitId.of("kingdomwarsmiddleearth:Rohan_Rider").toString(),
                "explicit namespace unit id");
    }

    private static void storesUnitDefinitionValues() {
        ArmyUnitDefinition gondorSoldier = gondorSoldier();

        assertEquals(ArmyUnitId.of("gondor_soldier"), gondorSoldier.id(), "unit id");
        assertEquals("Gondor Soldier", gondorSoldier.displayName(), "unit display name");
        assertEquals(FactionId.of("gondor"), gondorSoldier.factionId(), "unit faction");
        assertEquals(ArmyUnitRole.INFANTRY, gondorSoldier.role(), "unit role");
        assertEquals(25, gondorSoldier.hireCost(), "unit hire cost");
        assertEquals(24, gondorSoldier.maxHealth(), "unit max health");
        assertEquals(5, gondorSoldier.attackDamage(), "unit attack damage");
        assertEquals(ArmyFormation.LINE, gondorSoldier.defaultFormation(), "unit default formation");
    }

    private static void looksUpUnitsByIdFactionAndRole() {
        ArmyUnitCatalog catalog = testCatalog();

        Optional<ArmyUnitDefinition> gondorSoldier = catalog.definition(ArmyUnitId.of("gondor_soldier"));
        assertTrue(gondorSoldier.isPresent(), "gondor soldier lookup");
        assertEquals("Gondor Soldier", gondorSoldier.orElseThrow().displayName(), "gondor soldier lookup name");

        List<ArmyUnitDefinition> gondorUnits = catalog.unitsForFaction(FactionId.of("gondor"));
        assertEquals(1, gondorUnits.size(), "gondor unit count");
        assertEquals(ArmyUnitId.of("gondor_soldier"), gondorUnits.get(0).id(), "gondor unit id");

        List<ArmyUnitDefinition> cavalryUnits = catalog.unitsForRole(ArmyUnitRole.CAVALRY);
        assertEquals(1, cavalryUnits.size(), "cavalry unit count");
        assertEquals(ArmyUnitId.of("rohan_rider"), cavalryUnits.get(0).id(), "cavalry unit id");
    }

    private static void rejectsDuplicateUnitIds() {
        assertThrows(IllegalArgumentException.class, () -> new ArmyUnitCatalog(List.of(gondorSoldier(), gondorSoldier())),
                "duplicate unit ids");
    }

    private static ArmyUnitCatalog testCatalog() {
        return new ArmyUnitCatalog(List.of(
                gondorSoldier(),
                new ArmyUnitDefinition(
                        ArmyUnitId.of("rohan_rider"),
                        "Rohan Rider",
                        FactionId.of("rohan"),
                        ArmyUnitRole.CAVALRY,
                        35,
                        26,
                        6,
                        ArmyFormation.WEDGE),
                new ArmyUnitDefinition(
                        ArmyUnitId.of("mordor_orc"),
                        "Mordor Orc",
                        FactionId.of("mordor"),
                        ArmyUnitRole.BRUTE,
                        20,
                        22,
                        5,
                        ArmyFormation.COLUMN)));
    }

    private static ArmyUnitDefinition gondorSoldier() {
        return new ArmyUnitDefinition(
                ArmyUnitId.of("gondor_soldier"),
                "Gondor Soldier",
                FactionId.of("gondor"),
                ArmyUnitRole.INFANTRY,
                25,
                24,
                5,
                ArmyFormation.LINE);
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
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
