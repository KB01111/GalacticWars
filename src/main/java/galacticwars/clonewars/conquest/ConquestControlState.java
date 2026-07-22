package galacticwars.clonewars.conquest;

import java.util.Objects;

public record ConquestControlState(
        String regionId,
        String dimensionId,
        int beaconX,
        int beaconY,
        int beaconZ,
        String controllingFaction,
        String controllingKingdom,
        String capturingPlayer,
        int progress,
        long revision,
        long counterattackAt,
        long counterattackEndsAt,
        int counterattackProgress,
        String attackingFaction
) {
    public ConquestControlState {
        Objects.requireNonNull(regionId, "regionId");
        Objects.requireNonNull(dimensionId, "dimensionId");
        controllingFaction = controllingFaction == null ? "" : controllingFaction;
        controllingKingdom = controllingKingdom == null ? "" : controllingKingdom;
        capturingPlayer = capturingPlayer == null ? "" : capturingPlayer;
        attackingFaction = attackingFaction == null ? "" : attackingFaction;
        if (progress < 0 || revision < 0L || counterattackAt < 0L
                || counterattackEndsAt < 0L || counterattackProgress < 0) {
            throw new IllegalArgumentException("Invalid conquest state");
        }
    }

    public ConquestControlState(
            String regionId, String dimensionId, int beaconX, int beaconY, int beaconZ,
            String controllingFaction, String controllingKingdom, String capturingPlayer,
            int progress, long revision
    ) {
        this(regionId, dimensionId, beaconX, beaconY, beaconZ, controllingFaction,
                controllingKingdom, capturingPlayer, progress, revision, 0L, 0L, 0, "");
    }

    public ConquestControlState withProgress(String playerId, int updatedProgress) {
        return new ConquestControlState(regionId, dimensionId, beaconX, beaconY, beaconZ,
                controllingFaction, controllingKingdom, playerId, updatedProgress, revision,
                counterattackAt, counterattackEndsAt, counterattackProgress, attackingFaction);
    }

    public ConquestControlState captured(
            String faction, String kingdom, long scheduledCounterattack, String attackerFaction
    ) {
        return new ConquestControlState(regionId, dimensionId, beaconX, beaconY, beaconZ,
                faction, kingdom, "", 0, revision + 1L,
                scheduledCounterattack, 0L, 0, attackerFaction);
    }

    public ConquestControlState startCounterattack(long endsAt) {
        return new ConquestControlState(regionId, dimensionId, beaconX, beaconY, beaconZ,
                controllingFaction, controllingKingdom, "", 0, revision,
                counterattackAt, endsAt, 0, attackingFaction);
    }

    public ConquestControlState advanceDefense(int amount) {
        int updated = (int) Math.min(Integer.MAX_VALUE,
                Math.max(0L, (long) counterattackProgress + amount));
        return new ConquestControlState(regionId, dimensionId, beaconX, beaconY, beaconZ,
                controllingFaction, controllingKingdom, "", 0, revision,
                counterattackAt, counterattackEndsAt, updated, attackingFaction);
    }

    public ConquestControlState defended(long nextCounterattackAt) {
        return new ConquestControlState(regionId, dimensionId, beaconX, beaconY, beaconZ,
                controllingFaction, controllingKingdom, "", 0, revision + 1L,
                nextCounterattackAt, 0L, 0, attackingFaction);
    }

    public ConquestControlState counterattackLost() {
        return new ConquestControlState(regionId, dimensionId, beaconX, beaconY, beaconZ,
                attackingFaction, "", "", 0, revision + 1L, 0L, 0L, 0, "");
    }

    public boolean counterattackActive() {
        return counterattackEndsAt > 0L;
    }

    public ConquestControlState withControllingFaction(String faction) {
        return new ConquestControlState(regionId, dimensionId, beaconX, beaconY, beaconZ,
                faction, controllingKingdom, capturingPlayer, progress, revision,
                counterattackAt, counterattackEndsAt, counterattackProgress, attackingFaction);
    }
}
