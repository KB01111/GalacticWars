package galacticwars.clonewars.workforce;

import galacticwars.clonewars.kingdom.StorageEndpoint;
import java.util.UUID;

public final class SettlementSupplyLedgerTest {
    public static void main(String[] args) {
        UUID settlement = UUID.randomUUID();
        UUID demandId = UUID.randomUUID();
        UUID firstWorker = UUID.randomUUID();
        UUID secondWorker = UUID.randomUUID();
        StorageEndpoint storage = new StorageEndpoint("minecraft:overworld", 1, 64, 1, 9);
        SupplyDemand demand = new SupplyDemand(demandId, SupplyCategory.CONSTRUCTION,
                "minecraft:stone", 8, 0, 80, "build/project_1");
        SettlementSupplyLedger ledger = SettlementSupplyLedger.create(settlement).request(demand);

        var first = ledger.reserve(demandId, firstWorker, storage, 6, 8, 100, 20);
        assertTrue(first.accepted(), "first lease accepted");
        var second = first.ledger().reserve(demandId, secondWorker, storage, 6, 8, 100, 20);
        assertEquals(2, second.reservation().orElseThrow().quantity(), "physical stock not double reserved");

        SettlementSupplyLedger delivered = second.ledger().complete(
                first.reservation().orElseThrow().id(), firstWorker, 6, 110);
        assertEquals(2, delivered.nextDemand().orElseThrow().outstandingQuantity(), "delivery fulfills demand");
        SettlementSupplyLedger expired = delivered.releaseExpired(121);
        assertTrue(expired.reservation(second.reservation().orElseThrow().id()).orElseThrow().state()
                == SupplyReservation.State.RELEASED, "expired lease released");
        repeatedRequestsAndEndpointAccounting(demand, storage, firstWorker);
        System.out.println("SettlementSupplyLedgerTest passed");
    }

    private static void repeatedRequestsAndEndpointAccounting(
            SupplyDemand demand,
            StorageEndpoint firstStorage,
            UUID worker
    ) {
        SettlementSupplyLedger original = SettlementSupplyLedger.create(UUID.randomUUID()).request(demand);
        SettlementSupplyLedger repeated = original.request(demand);
        assertTrue(repeated == original, "identical request is a no-op");
        var first = repeated.reserve(demand.id(), worker, firstStorage, 3, 3, 200, 20);
        var retry = first.ledger().reserve(demand.id(), worker, firstStorage, 3, 3, 200, 20);
        assertTrue(retry.ledger() == first.ledger(), "reservation retry is a no-op");
        assertTrue(retry.reservation().orElseThrow().id().equals(first.reservation().orElseThrow().id()),
                "reservation retry keeps its identity");

        StorageEndpoint secondStorage = new StorageEndpoint("minecraft:overworld", 2, 64, 2, 9);
        var second = retry.ledger().reserve(demand.id(), UUID.randomUUID(), secondStorage, 10, 10, 200, 20);
        assertEquals(5, second.reservation().orElseThrow().quantity(),
                "separate endpoint stock remains capped by outstanding demand");

        SettlementSupplyLedger completed = second.ledger().complete(
                first.reservation().orElseThrow().id(), worker, 3, 201);
        assertTrue(completed.complete(first.reservation().orElseThrow().id(), worker, 3, 201) == completed,
                "completion retry is a no-op");

        var expiring = SettlementSupplyLedger.create(UUID.randomUUID()).request(demand)
                .reserve(demand.id(), UUID.randomUUID(), secondStorage, 1, 3, 300, 1);
        assertThrows(() -> expiring.ledger().complete(
                expiring.reservation().orElseThrow().id(),
                expiring.reservation().orElseThrow().workerId(), 1, 301), "expired completion rejected");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) throw new AssertionError(message + ": expected " + expected + ", got " + actual);
    }

    private static void assertThrows(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalStateException expected) {
            // Expected.
        }
    }
}
