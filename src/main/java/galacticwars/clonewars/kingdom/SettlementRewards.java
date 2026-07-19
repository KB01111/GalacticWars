package galacticwars.clonewars.kingdom;

import java.util.Objects;

public record SettlementRewards(
        int externalStorageSlots,
        int commanderSlots,
        SettlementTerminalLedger terminalLedger
) {
    public SettlementRewards {
        if (externalStorageSlots < 0 || commanderSlots < 0) {
            throw new IllegalArgumentException("settlement rewards cannot be negative");
        }
        terminalLedger = Objects.requireNonNullElseGet(
                terminalLedger, SettlementTerminalLedger::empty);
    }

    public SettlementRewards(int externalStorageSlots, int commanderSlots) {
        this(externalStorageSlots, commanderSlots, SettlementTerminalLedger.empty());
    }

    public static SettlementRewards none() {
        return new SettlementRewards(0, 0);
    }

    public SettlementRewards add(int storageSlots, int newCommanderSlots) {
        return new SettlementRewards(
                Math.addExact(externalStorageSlots, Math.max(0, storageSlots)),
                Math.addExact(commanderSlots, Math.max(0, newCommanderSlots)),
                terminalLedger);
    }

    public SettlementRewards withTerminalLedger(SettlementTerminalLedger ledger) {
        Objects.requireNonNull(ledger, "ledger");
        return terminalLedger.equals(ledger)
                ? this
                : new SettlementRewards(externalStorageSlots, commanderSlots, ledger);
    }
}
