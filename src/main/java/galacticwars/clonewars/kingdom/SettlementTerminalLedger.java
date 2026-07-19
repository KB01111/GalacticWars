package galacticwars.clonewars.kingdom;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Compact authority ledger for terminal settlement operations whose detailed records have aged
 * out of the operational history. The ledger intentionally stores identifiers and normalized
 * completion keys only; full project placement and work-order payloads remain bounded.
 */
public record SettlementTerminalLedger(
        Set<UUID> terminalProjectIds,
        Set<String> completedBuildKeys,
        Set<UUID> terminalWorkOrderIds
) {
    public SettlementTerminalLedger {
        terminalProjectIds = immutableUuidSet(terminalProjectIds, "terminalProjectIds");
        completedBuildKeys = immutableKeySet(completedBuildKeys);
        terminalWorkOrderIds = immutableUuidSet(terminalWorkOrderIds, "terminalWorkOrderIds");
    }

    public static SettlementTerminalLedger empty() {
        return new SettlementTerminalLedger(Set.of(), Set.of(), Set.of());
    }

    public boolean terminalProject(UUID projectId) {
        return terminalProjectIds.contains(Objects.requireNonNull(projectId, "projectId"));
    }

    public boolean completedBuild(BuildProject project) {
        return completedBuildKeys.contains(completionKey(project));
    }

    public boolean terminalWorkOrder(UUID workOrderId) {
        return terminalWorkOrderIds.contains(Objects.requireNonNull(workOrderId, "workOrderId"));
    }

    public SettlementTerminalLedger recordProject(BuildProject project) {
        Objects.requireNonNull(project, "project");
        if (project.state() != BuildProjectState.COMPLETED
                && project.state() != BuildProjectState.CANCELLED) {
            return this;
        }
        LinkedHashSet<UUID> projects = new LinkedHashSet<>(terminalProjectIds);
        projects.add(project.id());
        LinkedHashSet<String> completed = new LinkedHashSet<>(completedBuildKeys);
        if (project.state() == BuildProjectState.COMPLETED) {
            completed.add(completionKey(project));
        }
        if (projects.equals(terminalProjectIds) && completed.equals(completedBuildKeys)) {
            return this;
        }
        return new SettlementTerminalLedger(projects, completed, terminalWorkOrderIds);
    }

    public SettlementTerminalLedger recordWorkOrder(WorkOrder workOrder) {
        Objects.requireNonNull(workOrder, "workOrder");
        if (!workOrder.state().terminal() || terminalWorkOrderIds.contains(workOrder.id())) {
            return this;
        }
        LinkedHashSet<UUID> orders = new LinkedHashSet<>(terminalWorkOrderIds);
        orders.add(workOrder.id());
        return new SettlementTerminalLedger(terminalProjectIds, completedBuildKeys, orders);
    }

    public static String completionKey(BuildProject project) {
        Objects.requireNonNull(project, "project");
        return String.join("|",
                project.dimensionId(),
                project.blueprintId(),
                Integer.toString(project.originX()),
                Integer.toString(project.originY()),
                Integer.toString(project.originZ()));
    }

    private static Set<UUID> immutableUuidSet(Set<UUID> source, String label) {
        LinkedHashSet<UUID> copy = new LinkedHashSet<>();
        Objects.requireNonNull(source, label).forEach(value ->
                copy.add(Objects.requireNonNull(value, label + " value")));
        return Collections.unmodifiableSet(copy);
    }

    private static Set<String> immutableKeySet(Set<String> source) {
        LinkedHashSet<String> copy = new LinkedHashSet<>();
        Objects.requireNonNull(source, "completedBuildKeys").forEach(value -> {
            String normalized = Objects.requireNonNull(value, "completedBuildKeys value")
                    .trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException("completed build key cannot be blank");
            }
            copy.add(normalized);
        });
        return Collections.unmodifiableSet(copy);
    }
}
