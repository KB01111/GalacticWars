package middleearth.lotr.warmod.faction;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import middleearth.lotr.warmod.army.HiringDecision;
import middleearth.lotr.warmod.army.HiringPolicy;

public final class FactionHiringPolicyTest {
    private FactionHiringPolicyTest() {
    }

    public static void main(String[] args) {
        storesFactionDefinitionValues();
        resolvesFactionRelations();
        acceptsEligibleHiringRequest();
        rejectsHiringRequestWithStableReasonCodes();

        System.out.println("FactionHiringPolicyTest passed");
    }

    private static void storesFactionDefinitionValues() {
        FactionDefinition gondor = gondor();

        assertEquals(FactionId.of("gondor"), gondor.id(), "gondor id");
        assertEquals("Gondor", gondor.displayName(), "gondor display name");
        assertEquals(25, gondor.hireCost(), "gondor hire cost");
        assertEquals(10, gondor.minimumHiringAlignment(), "gondor minimum hiring alignment");
        assertEquals(12, gondor.maxOwnedRecruits(), "gondor max owned recruits");
        assertTrue(gondor.allies().contains(FactionId.of("rohan")), "gondor ally rohan");
        assertTrue(gondor.enemies().contains(FactionId.of("mordor")), "gondor enemy mordor");
    }

    private static void resolvesFactionRelations() {
        FactionCatalog catalog = testCatalog();

        assertEquals(FactionRelation.SELF, catalog.relation(FactionId.of("gondor"), FactionId.of("gondor")),
                "self relation");
        assertEquals(FactionRelation.ALLY, catalog.relation(FactionId.of("gondor"), FactionId.of("rohan")),
                "ally relation");
        assertEquals(FactionRelation.ENEMY, catalog.relation(FactionId.of("rohan"), FactionId.of("mordor")),
                "enemy relation");
        assertEquals(FactionRelation.NEUTRAL, catalog.relation(FactionId.of("mordor"), FactionId.of("unknown")),
                "neutral relation");
    }

    private static void acceptsEligibleHiringRequest() {
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        FactionDefinition gondor = gondor();
        FactionAlignment alignment = FactionAlignment.empty(playerId).withAddedScore(gondor.id(), 11);

        HiringDecision decision = HiringPolicy.canHire(alignment, gondor, 25, 11);

        assertTrue(decision.accepted(), "eligible hiring accepted");
        assertEquals("accepted", decision.reasonCode(), "accepted reason");
        assertEquals(25, decision.cost(), "accepted cost");
    }

    private static void rejectsHiringRequestWithStableReasonCodes() {
        UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000102");
        FactionDefinition gondor = gondor();
        FactionAlignment alignment = FactionAlignment.empty(playerId).withAddedScore(gondor.id(), 9);

        assertEquals("unknown_player", HiringPolicy.canHire(null, gondor, 25, 0).reasonCode(),
                "unknown player reason");
        assertEquals("alignment_too_low", HiringPolicy.canHire(alignment, gondor, 25, 0).reasonCode(),
                "low alignment reason");
        assertEquals("coins_too_low", HiringPolicy.canHire(alignment.withAddedScore(gondor.id(), 1), gondor, 24, 0).reasonCode(),
                "low coins reason");
        assertEquals("recruit_limit_reached", HiringPolicy.canHire(alignment.withAddedScore(gondor.id(), 1), gondor, 25, 12).reasonCode(),
                "recruit limit reason");
    }

    private static FactionCatalog testCatalog() {
        FactionDefinition gondor = gondor();
        FactionDefinition rohan = new FactionDefinition(
                FactionId.of("rohan"),
                "Rohan",
                20,
                8,
                10,
                Set.of(FactionId.of("gondor")),
                Set.of(FactionId.of("mordor")));
        FactionDefinition mordor = new FactionDefinition(
                FactionId.of("mordor"),
                "Mordor",
                30,
                15,
                16,
                Set.of(),
                Set.of(FactionId.of("gondor"), FactionId.of("rohan")));

        return new FactionCatalog(Map.of(
                gondor.id(), gondor,
                rohan.id(), rohan,
                mordor.id(), mordor));
    }

    private static FactionDefinition gondor() {
        return new FactionDefinition(
                FactionId.of("gondor"),
                "Gondor",
                25,
                10,
                12,
                Set.of(FactionId.of("rohan")),
                Set.of(FactionId.of("mordor")));
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
}
