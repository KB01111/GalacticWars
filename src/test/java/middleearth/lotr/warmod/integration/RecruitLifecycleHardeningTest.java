package middleearth.lotr.warmod.integration;

import java.util.UUID;
import middleearth.lotr.warmod.kingdom.RecruitmentCampaign;
import middleearth.lotr.warmod.kingdom.RecruitmentCampaignState;
import middleearth.lotr.warmod.kingdom.SettlementRecord;
import middleearth.lotr.warmod.menu.RecruitCommandMenu;
import middleearth.lotr.warmod.recruitment.RecruitDuty;
import middleearth.lotr.warmod.workforce.WorkerContractService;

public final class RecruitLifecycleHardeningTest {
    private RecruitLifecycleHardeningTest() {
    }

    public static void main(String[] args) {
        housingAndCommanderCleanup();
        campaignCancellation();
        supportedCommandButtonGuard();
        unsupportedCommandButtonRejection();
        workerExitGuards();
        System.out.println("RecruitLifecycleHardeningTest passed");
    }

    private static void housingAndCommanderCleanup() {
        UUID recruitId = UUID.randomUUID();
        SettlementRecord settlement = SettlementRecord.create("minecraft:overworld", 0, 64, 0)
                .withRecruit(recruitId)
                .withCommander(recruitId);

        assertTrue(settlement.containsRecruit(recruitId), "recruit registered");
        assertTrue(settlement.commanderId().isPresent(), "commander assigned");
        assertTrue(!settlement.hasHousingSpace(), "housing occupied");

        SettlementRecord removed = settlement.withoutRecruit(recruitId);

        assertTrue(!removed.containsRecruit(recruitId), "recruit unregistered");
        assertTrue(removed.commanderId().isEmpty(), "commander released");
        assertTrue(removed.hasHousingSpace(), "housing released");
        assertTrue(removed.revision() == settlement.revision() + 1, "revision incremented");

        SettlementRecord doubleRemoved = removed.withoutRecruit(recruitId);
        assertTrue(doubleRemoved == removed, "idempotent removal");
        assertTrue(doubleRemoved.revision() == removed.revision(), "no revision change on no-op");
    }

    private static void campaignCancellation() {
        UUID campaignId = UUID.randomUUID();
        RecruitmentCampaign active = new RecruitmentCampaign(
                campaignId,
                "gondor_soldier",
                "",
                50,
                1000L,
                RecruitmentCampaignState.RESERVED,
                "campaign_started"
        );

        assertTrue(active.active(), "campaign is active");
        assertTrue(active.state() == RecruitmentCampaignState.RESERVED, "campaign state RESERVED");
        assertTrue(active.reservedCost() == 50, "reserved cost recorded");

        RecruitmentCampaign cancelled = active.cancel("commander_lost");

        assertTrue(!cancelled.active(), "campaign no longer active");
        assertTrue(cancelled.state() == RecruitmentCampaignState.CANCELLED, "campaign state CANCELLED");
        assertTrue(cancelled.reasonCode().equals("commander_lost"), "cancellation reason recorded");
        assertTrue(cancelled.refundPending(), "refund pending for cancelled campaign with cost");
        assertTrue(cancelled.reservedCost() == 50, "reserved cost preserved for refund");

        RecruitmentCampaign refunded = cancelled.applyRefund(50);

        assertTrue(refunded.reservedCost() == 0, "cost cleared after refund");
        assertTrue(!refunded.refundPending(), "refund no longer pending");
    }

    private static void supportedCommandButtonGuard() {
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_HIRE), "hire button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_FOLLOW), "follow button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_HOLD), "hold button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_MOVE), "move button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_PROTECT), "protect button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_ATTACK), "attack button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_CLEAR), "clear button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_SET_WORKSITE), "set worksite button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_RETURN_WORKSITE), "return worksite button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_CLEAR_WORKSITE), "clear worksite button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_SET_STORAGE), "set storage button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_BUILD_STARTER_KEEP), "build starter keep button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_WORK_RADIUS_DECREASE), "work radius decrease button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_WORK_RADIUS_INCREASE), "work radius increase button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_PROMOTE_COMMANDER), "promote commander button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_TOGGLE_AUTO_RECRUITMENT), "toggle auto recruitment button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_START_RECRUITMENT), "start recruitment button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_NEXT_BLUEPRINT), "next blueprint button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_RETURN_TO_SOLDIER), "return to soldier button supported");
        assertTrue(RecruitCommandMenu.isSupportedButton(RecruitCommandMenu.BUTTON_CANCEL_BUILD), "cancel build button supported");
    }

    private static void unsupportedCommandButtonRejection() {
        assertTrue(!RecruitCommandMenu.isSupportedButton(-1), "negative button id rejected");
        assertTrue(!RecruitCommandMenu.isSupportedButton(999), "arbitrary high button id rejected");
        assertTrue(!RecruitCommandMenu.isSupportedButton(Integer.MIN_VALUE), "min int button id rejected");
        assertTrue(!RecruitCommandMenu.isSupportedButton(Integer.MAX_VALUE), "max int button id rejected");
    }

    private static void workerExitGuards() {
        assertTrue(
                WorkerContractService.evaluateExit(RecruitDuty.WORKER, true, true, false)
                        == WorkerContractService.ExitDecision.ACCEPTED,
                "clean worker exit accepted"
        );

        assertTrue(
                WorkerContractService.evaluateExit(RecruitDuty.WORKER, true, false, false)
                        == WorkerContractService.ExitDecision.CARRIED_ITEMS,
                "worker with carried items blocked"
        );

        assertTrue(
                WorkerContractService.evaluateExit(RecruitDuty.WORKER, true, true, true)
                        == WorkerContractService.ExitDecision.ACTIVE_BUILD,
                "worker with active build blocked"
        );

        assertTrue(
                WorkerContractService.evaluateExit(RecruitDuty.COMMANDER, true, true, false)
                        == WorkerContractService.ExitDecision.NOT_WORKER,
                "commander cannot exit to soldier via worker path"
        );

        assertTrue(
                WorkerContractService.evaluateExit(RecruitDuty.WORKER, false, true, false)
                        == WorkerContractService.ExitDecision.NOT_WORKER,
                "worker without profession blocked"
        );

        assertTrue(
                WorkerContractService.evaluateExit(RecruitDuty.SOLDIER, true, true, false)
                        == WorkerContractService.ExitDecision.NOT_WORKER,
                "soldier duty cannot use worker exit"
        );
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
