package galacticwars.clonewars.kingdom;

import java.util.Set;
import java.util.UUID;

public final class CommanderRecruitmentTest {
    private CommanderRecruitmentTest() {
    }

    public static void main(String[] args) {
        acceptsBoundedAlliedCampaign();
        rejectsHostileAndUnderfundedCampaigns();
        directHiringUsesTheSameFactionCapacityAndUpkeepRules();
        rejectsDirectAndCampaignRecruitmentAtTheEffectiveFactionLimit();
        pausesCampaignWhileSettlementIsUnloaded();
        invalidCampaignStatesFailClosedAndRefundsRemainPending();
        System.out.println("CommanderRecruitmentTest passed");
    }

    private static void acceptsBoundedAlliedCampaign() {
        UUID ownerId = UUID.randomUUID();
        UUID commanderId = UUID.randomUUID();
        CommanderPolicy policy = new CommanderPolicy(true, 4, 40, 10, 1200);
        SettlementRecord settlement = SettlementRecord.create("minecraft:overworld", 0, 64, 0)
                .withRecruit(commanderId)
                .withCommander(commanderId)
                .withCommanderPolicy(policy);
        KingdomRecord kingdom = new KingdomRecord(
                UUID.randomUUID(), ownerId, "galacticwars:republic", settlement);

        RecruitmentCampaignDecision decision = RecruitmentService.evaluateCommanderCampaignWithLimit(
                kingdom,
                commanderId,
                "galacticwars:mandalorian_rider",
                "galacticwars:mandalorian",
                "",
                20,
                48,
                500,
                Set.of("galacticwars:mandalorian"),
                12);

        assertTrue(decision.accepted(), "allied campaign");
        RecruitmentCampaign campaign = decision.campaign().orElseThrow();
        assertEquals(1700L, campaign.readyGameTime(), "ready time");
        assertEquals(20, campaign.reservedCost(), "reserved cost");
    }

    private static void rejectsHostileAndUnderfundedCampaigns() {
        UUID ownerId = UUID.randomUUID();
        UUID commanderId = UUID.randomUUID();
        SettlementRecord settlement = SettlementRecord.create("minecraft:overworld", 0, 64, 0)
                .withRecruit(commanderId)
                .withCommander(commanderId)
                .withCommanderPolicy(new CommanderPolicy(true, 4, 40, 10, 1200));
        KingdomRecord kingdom = new KingdomRecord(
                UUID.randomUUID(), ownerId, "galacticwars:republic", settlement);

        RecruitmentCampaignDecision hostile = RecruitmentService.evaluateCommanderCampaignWithLimit(
                kingdom, commanderId, "galacticwars:b1_battle_droid", "galacticwars:separatist",
                "", 20, 48, 0, Set.of("galacticwars:mandalorian"), 12);
        RecruitmentCampaignDecision underfunded = RecruitmentService.evaluateCommanderCampaignWithLimit(
                kingdom, commanderId, "galacticwars:clone_trooper", "galacticwars:republic",
                "", 20, 25, 0, Set.of("galacticwars:mandalorian"), 12);

        assertEquals("hostile_faction", hostile.reasonCode(), "hostile reason");
        assertEquals("treasury_reserve_required", underfunded.reasonCode(), "treasury reason");
    }

    private static void pausesCampaignWhileSettlementIsUnloaded() {
        RecruitmentCampaign campaign = new RecruitmentCampaign(
                UUID.randomUUID(),
                "galacticwars:clone_trooper",
                "",
                25,
                1200,
                RecruitmentCampaignState.RESERVED,
                "reserved");
        RecruitmentCampaign delayed = campaign.delay(400);
        assertEquals(1600L, delayed.readyGameTime(), "paused campaign ready time");
        assertEquals("settlement_unloaded", delayed.reasonCode(), "pause reason");
        RecruitmentCampaign completed = delayed.complete();
        assertEquals(completed, completed.delay(400), "completed campaigns do not delay");
    }

    private static void invalidCampaignStatesFailClosedAndRefundsRemainPending() {
        assertEquals(RecruitmentCampaignState.CANCELLED,
                RecruitmentCampaignState.byId("future_unknown_state"),
                "unknown campaign state");
        RecruitmentCampaign campaign = new RecruitmentCampaign(
                UUID.randomUUID(),
                "galacticwars:clone_trooper",
                "",
                25,
                1200,
                RecruitmentCampaignState.RESERVED,
                "reserved");
        RecruitmentCampaign cancelled = campaign.cancel("commander_lost");
        assertTrue(cancelled.refundPending(), "cancelled reservation remains refundable");
        RecruitmentCampaign partiallyRefunded = cancelled.applyRefund(10);
        assertEquals(15, partiallyRefunded.reservedCost(), "partial refund remainder");
        assertEquals(0, partiallyRefunded.applyRefund(15).reservedCost(), "completed refund remainder");
        RecruitmentCampaign legacyCancelled = new RecruitmentCampaign(
                UUID.randomUUID(),
                "galacticwars:clone_trooper",
                "",
                25,
                1200,
                RecruitmentCampaignState.CANCELLED,
                "legacy_cancelled");
        assertTrue(!legacyCancelled.refundPending(), "legacy cancelled campaign does not duplicate a refund");
    }

    private static void directHiringUsesTheSameFactionCapacityAndUpkeepRules() {
        KingdomRecord kingdom = new KingdomRecord(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "galacticwars:republic",
                SettlementRecord.create("minecraft:overworld", 0, 64, 0));
        RecruitmentEligibility allied = RecruitmentService.evaluateDirectHireWithLimit(
                kingdom, "galacticwars:mandalorian", 25, 25, true,
                Set.of("galacticwars:mandalorian"), 12);
        RecruitmentEligibility hostile = RecruitmentService.evaluateDirectHireWithLimit(
                kingdom, "galacticwars:separatist", 25, 25, true,
                Set.of("galacticwars:mandalorian"), 12);
        RecruitmentEligibility unpaid = RecruitmentService.evaluateDirectHireWithLimit(
                kingdom, "galacticwars:republic", 25, 25, false,
                Set.of("galacticwars:mandalorian"), 12);

        assertTrue(allied.accepted(), "allied direct hire");
        assertEquals("hostile_faction", hostile.reasonCode(), "hostile direct hire reason");
        assertEquals("upkeep_unpaid", unpaid.reasonCode(), "unpaid upkeep reason");
    }

    private static void rejectsDirectAndCampaignRecruitmentAtTheEffectiveFactionLimit() {
        UUID commanderId = UUID.randomUUID();
        SettlementRecord settlement = SettlementRecord.create("minecraft:overworld", 0, 64, 0)
                .withRecruit(commanderId)
                .withRecruit(UUID.randomUUID())
                .withCommander(commanderId)
                .withCommanderPolicy(new CommanderPolicy(true, 4, 40, 10, 1200));
        KingdomRecord kingdom = new KingdomRecord(
                UUID.randomUUID(), UUID.randomUUID(), "galacticwars:republic", settlement);

        RecruitmentEligibility direct = RecruitmentService.evaluateDirectHireWithLimit(
                kingdom, "galacticwars:republic", 20, 20, true, Set.of(), 2);
        RecruitmentCampaignDecision campaign = RecruitmentService.evaluateCommanderCampaignWithLimit(
                kingdom,
                commanderId,
                "galacticwars:clone_trooper",
                "galacticwars:republic",
                "",
                20,
                100,
                0,
                Set.of(),
                2);

        assertEquals("recruit_limit_reached", direct.reasonCode(), "direct recruit cap reason");
        assertEquals("recruit_limit_reached", campaign.reasonCode(), "campaign recruit cap reason");
    }

    private static void assertTrue(boolean value, String label) {
        if (!value) {
            throw new AssertionError(label + " expected true");
        }
    }

    private static void assertEquals(Object expected, Object actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
