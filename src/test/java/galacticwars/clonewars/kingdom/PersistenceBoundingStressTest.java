package galacticwars.clonewars.kingdom;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.data.LaunchContentRuntime;
import galacticwars.clonewars.menu.CommandCenterDashboardCodec;
import galacticwars.clonewars.progression.ProgressionDecision;
import galacticwars.clonewars.progression.ProgressionEvent;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.progression.ProgressionState;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Save/reload and packet stress coverage for bounded campaign and settlement histories. */
public final class PersistenceBoundingStressTest {
    private static final int DELIVERY_COUNT = 10_000;
    private static final int BUILD_HISTORY_COUNT = 1_000;
    private static final int WORK_ORDER_HISTORY_COUNT = 10_000;

    private PersistenceBoundingStressTest() {
    }

    public static void main(String[] args) {
        installContent();
        progressionHistoryRemainsBoundedAndReplaySafe();
        settlementHistoryRemainsBoundedAndAuthoritySafe();
        System.out.println("PersistenceBoundingStressTest passed");
    }

    private static void progressionHistoryRemainsBoundedAndReplaySafe() {
        UUID playerId = new UUID(0x475750524f475245L, 0x5353494f4e000001L);
        ProgressionSavedData savedData = new ProgressionSavedData();
        ProgressionDecision pledge = savedData.apply(new ProgressionEvent(
                new UUID(1L, 1L), playerId, ProgressionEventType.FACTION_PLEDGED,
                "galacticwars:republic", 1));
        require(pledge.accepted() && pledge.changed(), "faction pledge must initialize campaign state");
        require(savedData.pendingCreditRewards(playerId) == 40,
                "eligible campaign quest must award exactly once before delivery churn");

        ProgressionEvent firstDelivery = null;
        ProgressionEvent lastDelivery = null;
        for (int index = 0; index < DELIVERY_COUNT; index++) {
            UUID eventId = new UUID(2L, index + 1L);
            ProgressionEvent delivery = new ProgressionEvent(
                    eventId, playerId, ProgressionEventType.DELIVERY_COMPLETED,
                    "courier/" + eventId, 1);
            if (index == 0) {
                firstDelivery = delivery;
            }
            lastDelivery = delivery;
            ProgressionDecision decision = savedData.apply(delivery);
            require(decision.accepted() && decision.changed(),
                    "each authoritative delivery must be counted exactly once");
        }

        ProgressionState state = savedData.state(playerId);
        require(state.total(ProgressionEventType.DELIVERY_COMPLETED) == DELIVERY_COUNT,
                "delivery total must survive compaction");
        require(state.processedEventIds().size() == ProgressionState.MAX_PROCESSED_EVENT_IDS,
                "authoritative replay window must have a fixed upper bound");
        Set<String> deliverySubjects = state.eventSubjects()
                .getOrDefault(ProgressionEventType.DELIVERY_COMPLETED, Set.of());
        require(deliverySubjects.size() == ProgressionState.MAX_RECENT_DELIVERY_SUBJECTS,
                "delivery detail must be reduced to bounded recent history");
        require(lastDelivery != null && state.processed(lastDelivery.id())
                        && state.hasSubject(ProgressionEventType.DELIVERY_COMPLETED,
                                lastDelivery.subjectId()),
                "most recent delivery must remain replay protected and inspectable");
        require(firstDelivery != null && !state.hasSubject(
                        ProgressionEventType.DELIVERY_COMPLETED, firstDelivery.subjectId()),
                "old delivery subjects must age into the aggregate total");

        JsonElement encoded = ProgressionSavedData.CODEC
                .encodeStart(JsonOps.INSTANCE, savedData).getOrThrow();
        require(encoded.toString().length() < 100_000,
                "10k deliveries must not create an unbounded progression save payload");
        ProgressionSavedData reloaded = ProgressionSavedData.CODEC
                .parse(JsonOps.INSTANCE, encoded).getOrThrow();
        ProgressionState reloadedState = reloaded.state(playerId);
        require(reloadedState.total(ProgressionEventType.DELIVERY_COMPLETED) == DELIVERY_COUNT
                        && reloadedState.processedEventIds().size()
                                == ProgressionState.MAX_PROCESSED_EVENT_IDS,
                "bounded aggregate and replay history must survive codec round trip");

        ProgressionDecision duplicate = reloaded.apply(lastDelivery);
        require(duplicate.accepted() && !duplicate.changed()
                        && duplicate.state().total(ProgressionEventType.DELIVERY_COMPLETED)
                                == DELIVERY_COUNT,
                "recent delivery replay must stay idempotent after save/reload");
        ProgressionDecision questReplay = reloaded.apply(new ProgressionEvent(
                new UUID(3L, 1L), playerId, ProgressionEventType.QUEST_ADVANCED,
                "republic_chapter_1", 1));
        require(questReplay.accepted() && !questReplay.changed()
                        && reloaded.pendingCreditRewards(playerId) == 40,
                "durable quest subjects must prevent rewards from being minted twice");
    }

    private static void settlementHistoryRemainsBoundedAndAuthoritySafe() {
        UUID settlementId = new UUID(4L, 1L);
        ArrayList<BuildProject> projects = new ArrayList<>();
        BuildProject activeProject = project(new UUID(5L, 1L), 0, BuildProjectState.ACTIVE);
        BuildProject blockedProject = new BuildProject(
                new UUID(5L, 2L), "command_center", "minecraft:overworld",
                1, 64, 1, 0, "stress", List.of(), BuildProjectState.BLOCKED,
                "awaiting_materials", 1);
        projects.add(activeProject);
        projects.add(blockedProject);
        for (int index = 2; index < SettlementRecord.MAX_ACTIVE_BUILD_PROJECTS; index++) {
            projects.add(project(new UUID(5L, index + 1L), -index, BuildProjectState.ACTIVE));
        }
        BuildProject oldestCompleted = null;
        BuildProject newestCompleted = null;
        for (int index = 0; index < BUILD_HISTORY_COUNT; index++) {
            BuildProject completed = project(
                    new UUID(6L, index + 1L), index + 10, BuildProjectState.COMPLETED);
            if (index == 0) {
                oldestCompleted = completed;
            }
            newestCompleted = completed;
            projects.add(completed);
        }

        ArrayList<WorkOrder> workOrders = new ArrayList<>();
        WorkOrder queuedOrder = workOrder(new UUID(7L, 1L), WorkOrderState.QUEUED, 0);
        WorkOrder blockedOrder = workOrder(new UUID(7L, 2L), WorkOrderState.BLOCKED, 0);
        workOrders.add(queuedOrder);
        workOrders.add(blockedOrder);
        for (int index = 2; index < SettlementRecord.MAX_ACTIVE_WORK_ORDERS; index++) {
            workOrders.add(workOrder(new UUID(7L, index + 1L), WorkOrderState.QUEUED, 0));
        }
        WorkOrder oldestCompletedOrder = null;
        WorkOrder newestCompletedOrder = null;
        for (int index = 0; index < WORK_ORDER_HISTORY_COUNT; index++) {
            WorkOrder completed = workOrder(
                    new UUID(8L, index + 1L), WorkOrderState.COMPLETED, 1);
            if (index == 0) {
                oldestCompletedOrder = completed;
            }
            newestCompletedOrder = completed;
            workOrders.add(completed);
        }

        SettlementRecord settlement = new SettlementRecord(
                settlementId, "minecraft:overworld", 0, 64, 0, 48, 4,
                List.of(), Optional.empty(), CommanderPolicy.defaults(), List.of(),
                projects, workOrders, List.of(), SettlementRewards.none(), 0);
        require(settlement.buildProjects().size()
                        == SettlementRecord.MAX_ACTIVE_BUILD_PROJECTS
                                + SettlementRecord.MAX_RECENT_TERMINAL_BUILD_PROJECTS,
                "full completed build records must have a fixed recent-history bound");
        require(settlement.workOrders().size()
                        == SettlementRecord.MAX_ACTIVE_WORK_ORDERS
                                + SettlementRecord.MAX_RECENT_TERMINAL_WORK_ORDERS,
                "full terminal work-order records must have a fixed recent-history bound");
        require(settlement.buildProjects().contains(activeProject)
                        && settlement.buildProjects().contains(blockedProject)
                        && settlement.workOrders().contains(queuedOrder)
                        && settlement.workOrders().contains(blockedOrder),
                "compaction must never discard active or blocked settlement work");
        SettlementTerminalLedger ledger = settlement.rewards().terminalLedger();
        require(ledger.terminalProjectIds().size() == BUILD_HISTORY_COUNT
                        && ledger.completedBuildKeys().size() == BUILD_HISTORY_COUNT
                        && ledger.terminalWorkOrderIds().size() == WORK_ORDER_HISTORY_COUNT,
                "lightweight authority ledger must preserve every terminal identifier");
        require(oldestCompleted != null && newestCompleted != null
                        && oldestCompletedOrder != null && newestCompletedOrder != null,
                "stress fixtures must include oldest and newest terminal records");
        require(!settlement.buildProjects().contains(oldestCompleted)
                        && settlement.buildProjects().contains(newestCompleted)
                        && !settlement.workOrders().contains(oldestCompletedOrder)
                        && settlement.workOrders().contains(newestCompletedOrder),
                "only the most recent full terminal records should remain");

        BuildProject replayedProjectId = new BuildProject(
                oldestCompleted.id(), "command_center", "minecraft:overworld",
                50_000, 64, 0, 0, "stress", List.of(), BuildProjectState.ACTIVE, "", 0);
        BuildProject replayedCompletionSite = new BuildProject(
                new UUID(9L, 1L), oldestCompleted.blueprintId(), oldestCompleted.dimensionId(),
                oldestCompleted.originX(), oldestCompleted.originY(), oldestCompleted.originZ(),
                0, "stress", List.of(), BuildProjectState.ACTIVE, "", 0);
        require(settlement.withNewBuildProject(replayedProjectId) == settlement
                        && settlement.withNewBuildProject(replayedCompletionSite) == settlement,
                "compacted builds must not be replayed by identifier or completion site");
        WorkOrder replayedOrder = workOrder(
                oldestCompletedOrder.id(), WorkOrderState.QUEUED, 0);
        require(settlement.withWorkOrder(replayedOrder, true) == settlement,
                "compacted terminal work orders must not be reinserted");
        BuildProject excessActiveProject = project(
                new UUID(9L, 2L), 60_000, BuildProjectState.ACTIVE);
        WorkOrder excessActiveOrder = workOrder(
                new UUID(9L, 3L), WorkOrderState.QUEUED, 0);
        require(settlement.withNewBuildProject(excessActiveProject) == settlement
                        && settlement.withWorkOrder(excessActiveOrder, true) == settlement,
                "new live work must be rejected cleanly once operational capacity is reached");

        JsonElement encoded = KingdomCodecs.SETTLEMENT
                .encodeStart(JsonOps.INSTANCE, settlement).getOrThrow();
        JsonObject encodedObject = encoded.getAsJsonObject();
        require(encodedObject.getAsJsonArray("build_projects").size()
                        == settlement.buildProjects().size()
                        && encodedObject.getAsJsonArray("work_orders").size()
                                == settlement.workOrders().size(),
                "save codec must emit only bounded full terminal records");
        SettlementRecord reloaded = KingdomCodecs.SETTLEMENT
                .parse(JsonOps.INSTANCE, encoded).getOrThrow();
        require(reloaded.equals(settlement)
                        && reloaded.rewards().terminalLedger().terminalProject(
                                oldestCompleted.id())
                        && reloaded.rewards().terminalLedger().terminalWorkOrder(
                                oldestCompletedOrder.id()),
                "settlement history and compact authority ledger must survive save/reload");

        UUID ownerId = new UUID(10L, 1L);
        KingdomRecord kingdom = new KingdomRecord(
                new UUID(10L, 2L), ownerId, "galacticwars:republic", reloaded);
        CommandCenterDashboardState dashboard = CommandCenterDashboardState.capture(
                ownerId, kingdom, List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), List.of(), ProgressionState.create(ownerId), 0, true,
                CommandCenterDashboardState.ActionAvailability.accepted(), List.of(),
                List.of(), List.of(), List.of(), 20L);
        require(dashboard.builds().size() <= CommandCenterDashboardState.MAX_DASHBOARD_ENTRIES
                        && dashboard.workOrders().size()
                                <= CommandCenterDashboardState.MAX_DASHBOARD_ENTRIES,
                "dashboard projections must stay within their packet bounds");
        require(dashboard.builds().size() == CommandCenterDashboardState.MAX_DASHBOARD_ENTRIES
                        && dashboard.workOrders().size()
                                == CommandCenterDashboardState.MAX_DASHBOARD_ENTRIES,
                "stress projection must exercise the dashboard's exact upper bounds");
        require(dashboard.builds().stream().anyMatch(summary -> summary.id().equals(activeProject.id()))
                        && dashboard.builds().stream().anyMatch(
                                summary -> summary.id().equals(blockedProject.id()))
                        && dashboard.workOrders().stream().anyMatch(
                                summary -> summary.id().equals(queuedOrder.id()))
                        && dashboard.workOrders().stream().anyMatch(
                                summary -> summary.id().equals(blockedOrder.id())),
                "dashboard bounding must prioritize all live work over terminal history");
        require(dashboard.workOrders().stream().noneMatch(summary ->
                        summary.state().equals(WorkOrderState.COMPLETED.id())
                                || summary.state().equals(WorkOrderState.CANCELLED.id())),
                "a full live queue must never be displaced by terminal dashboard history");

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            CommandCenterDashboardCodec.write(buffer, dashboard);
            require(buffer.readableBytes() < 65_536,
                    "high-churn dashboard packet must remain comfortably bounded");
            CommandCenterDashboardState decodedDashboard = CommandCenterDashboardCodec.read(buffer);
            require(decodedDashboard.equals(dashboard),
                    "bounded dashboard must round trip through the wire codec");
        } finally {
            buffer.release();
        }
    }

    private static BuildProject project(UUID id, int x, BuildProjectState state) {
        return new BuildProject(
                id, "command_center", "minecraft:overworld", x, 64, 0,
                0, "stress", List.of(), state, "", state.terminal() ? 1 : 0);
    }

    private static WorkOrder workOrder(UUID id, WorkOrderState state, int completedQuantity) {
        Optional<UUID> assigned = switch (state) {
            case CLAIMED, IN_PROGRESS, BLOCKED, COMPLETED -> Optional.of(new UUID(11L, 1L));
            case QUEUED, CANCELLED -> Optional.empty();
        };
        String blockedReason = state == WorkOrderState.BLOCKED ? "awaiting_route" : "";
        return new WorkOrder(
                id, WorkOrderType.COURIER, assigned, state, Optional.empty(), Optional.empty(),
                "minecraft:overworld", 0, 64, 0, "minecraft:iron_ingot", 1,
                completedQuantity, blockedReason, state.terminal() ? 2 : 0);
    }

    private static void installContent() {
        LaunchContentRuntime.install(
                new LaunchContentDefinitions(
                        Map.of(), Map.of(), Map.of(),
                        Map.of("republic_chapter_1", new LaunchContentDefinitions.QuestDefinition(
                                "republic_chapter_1", List.of("faction_pledged"), 40,
                                Set.of("workforce"))),
                        Map.of(), Map.of()),
                List.of("galacticwars:republic"), Map.of());
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
