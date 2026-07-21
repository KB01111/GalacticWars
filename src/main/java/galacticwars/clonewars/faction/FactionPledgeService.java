package galacticwars.clonewars.faction;

import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.data.CoreContentBindings;
import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.kingdom.KingdomActionId;
import galacticwars.clonewars.kingdom.KingdomGameplayAction;
import galacticwars.clonewars.kingdom.KingdomGameplayResult;
import galacticwars.clonewars.kingdom.KingdomGameplayRuntimeService;
import galacticwars.clonewars.kingdom.KingdomGameplayTransactionService;
import galacticwars.clonewars.kingdom.KingdomRecord;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.progression.ProgressionState;
import galacticwars.clonewars.settlement.CommandCenterBlockEntity;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** One authoritative transaction for menu- and item-driven faction pledges. */
public final class FactionPledgeService {
    private FactionPledgeService() {
    }

    public static Result pledge(
            ServerPlayer player,
            CommandCenterBlockEntity hall,
            String requestedFactionId
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(hall, "hall");
        if (!(player.level() instanceof ServerLevel level)
                || hall.getLevel() != level
                || level.getBlockEntity(hall.getBlockPos()) != hall
                || !hall.isOwner(player)
                || player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(hall.getBlockPos())) > 64.0D) {
            return Result.rejected("invalid_hall", requestedFactionId);
        }
        if (!GameplayDataManager.isReady()) {
            return Result.rejected("content_unavailable", requestedFactionId);
        }

        FactionDefinition faction;
        try {
            faction = GameplayDataManager.snapshot().factions()
                    .definition(FactionId.of(requestedFactionId)).orElse(null);
        } catch (RuntimeException invalidId) {
            faction = null;
        }
        if (faction == null || !CoreContentBindings.factionChips().containsKey(requestedFactionId)) {
            return Result.rejected("data_missing", requestedFactionId);
        }
        String requiredChipId = CoreContentBindings.factionChips().get(requestedFactionId);
        if (!requiredChipId.equals(faction.pledgeTokenItemId())) {
            return Result.rejected("data_missing", requestedFactionId);
        }

        KingdomSavedData kingdoms = KingdomSavedData.get(level);
        ProgressionSavedData progression = ProgressionSavedData.get(level);
        FactionAlignmentSavedData alignments = FactionAlignmentSavedData.get(level);
        ProgressionState progressionBefore = progression.state(player.getUUID());
        KingdomRecord existing = kingdoms.kingdomForOwner(player.getUUID()).orElse(null);
        if (existing != null) {
            if (isCommittedPledge(level, player, hall, existing, progressionBefore, requestedFactionId)) {
                return Result.alreadyCommitted(
                        requestedFactionId,
                        alignments.alignment(player.getUUID()).score(faction.id()));
            }
            return Result.rejected("data_conflict", requestedFactionId);
        }
        if (kingdoms.kingdomForPlayer(player.getUUID()).isPresent()) {
            return Result.rejected("already_member", requestedFactionId);
        }
        if (!progressionBefore.factionId().isEmpty()) {
            return Result.rejected("data_conflict", requestedFactionId);
        }

        BlockPos hallPos = hall.getBlockPos();
        String dimensionId = level.dimension().identifier().toString();
        if (!kingdoms.canActivateHall(player.getUUID(), dimensionId, hallPos)) {
            return Result.rejected("claim_conflict", requestedFactionId);
        }
        Item requiredChip = registeredItem(requiredChipId);
        int chipSlot = findItemSlot(player, requiredChip);
        if (requiredChip == null || chipSlot < 0) {
            return Result.rejected("chip_required", requestedFactionId);
        }

        KingdomGameplayAction action = new KingdomGameplayAction(
                KingdomActionId.of("faction_pledge", player.getUUID(), requestedFactionId),
                player.getUUID(), ProgressionEventType.FACTION_PLEDGED, requestedFactionId, 1);
        KingdomGameplayResult evaluation = KingdomGameplayTransactionService.evaluate(progressionBefore, action);
        if (!evaluation.accepted() || !evaluation.changed()) {
            return Result.rejected(evaluation.reason(), requestedFactionId);
        }

        boolean progressionWasStored = progression.hasStoredState(player.getUUID());
        FactionAlignment alignmentBefore = alignments.alignment(player.getUUID());
        boolean alignmentWasStored = alignments.hasStoredAlignment(player.getUUID());
        String hallFactionBefore = hall.factionId();
        KingdomRecord created = kingdoms.activateHall(
                player.getUUID(), requestedFactionId, dimensionId, hallPos).orElse(null);
        if (created == null) {
            return Result.rejected("claim_conflict", requestedFactionId);
        }

        ProgressionState progressionAfter = progressionBefore;
        FactionAlignment alignmentAfter = alignmentBefore;
        try {
            KingdomGameplayResult committed = KingdomGameplayRuntimeService.applyProgression(progression, action);
            if (!committed.accepted() || !committed.changed()) {
                if (!kingdoms.rollbackFreshKingdom(player.getUUID(), created.id())) {
                    GalacticWars.LOGGER.error("Could not compensate rejected faction pledge kingdom {}", created.id());
                    return Result.rejected("reconciliation_required", requestedFactionId);
                }
                return Result.rejected(committed.reason(), requestedFactionId);
            }
            progressionAfter = progression.state(player.getUUID());
            FactionAlignmentUpdateResult alignmentResult = alignments.applyPledge(
                    player.getUUID(), faction, GameplayDataManager.snapshot().factions());
            alignmentAfter = alignmentResult.alignment();
            hall.setFaction(requestedFactionId);
            if (!player.hasInfiniteMaterials()) {
                ItemStack chip = player.getInventory().getItem(chipSlot);
                if (!chip.is(requiredChip) || chip.isEmpty()) {
                    throw new IllegalStateException("pledge chip changed during transaction");
                }
                chip.shrink(1);
                player.getInventory().setChanged();
            }
            return Result.committed(
                    requestedFactionId, alignmentAfter.score(faction.id()), requiredChipId);
        } catch (RuntimeException failure) {
            boolean hallRestored = restoreHallFaction(hall, hallFactionBefore);
            boolean alignmentRestored = alignmentAfter.equals(alignmentBefore)
                    || alignments.restoreAfterFailedTransaction(
                            player.getUUID(), alignmentAfter, alignmentBefore, alignmentWasStored);
            boolean progressionRestored = progressionAfter.equals(progressionBefore)
                    || progression.restoreAfterFailedTransaction(
                            player.getUUID(), progressionAfter, progressionBefore, progressionWasStored);
            boolean kingdomRestored = kingdoms.rollbackFreshKingdom(player.getUUID(), created.id());
            GalacticWars.LOGGER.error(
                    "Faction pledge transaction failed for {}; compensation hall={}, alignment={}, progression={}, kingdom={}",
                    player.getGameProfile().name(), hallRestored, alignmentRestored,
                    progressionRestored, kingdomRestored, failure);
            return Result.rejected(
                    hallRestored && alignmentRestored && progressionRestored && kingdomRestored
                            ? "transaction_failed"
                            : "reconciliation_required",
                    requestedFactionId);
        }
    }

    private static boolean isCommittedPledge(
            ServerLevel level,
            ServerPlayer player,
            CommandCenterBlockEntity hall,
            KingdomRecord kingdom,
            ProgressionState progression,
            String factionId
    ) {
        var settlement = kingdom.settlement();
        return kingdom.factionId().equals(factionId)
                && progression.factionId().equals(factionId)
                && hall.factionId().equals(factionId)
                && KingdomSavedData.get(level).isHallActive(player.getUUID())
                && settlement.dimensionId().equals(level.dimension().identifier().toString())
                && settlement.hallX() == hall.getBlockPos().getX()
                && settlement.hallY() == hall.getBlockPos().getY()
                && settlement.hallZ() == hall.getBlockPos().getZ();
    }

    private static Item registeredItem(String itemId) {
        try {
            Identifier id = Identifier.parse(itemId);
            Item item = BuiltInRegistries.ITEM.getValue(id);
            return item != null && id.equals(BuiltInRegistries.ITEM.getKey(item)) ? item : null;
        } catch (RuntimeException invalidId) {
            return null;
        }
    }

    private static int findItemSlot(ServerPlayer player, Item item) {
        if (item == null) {
            return -1;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(item)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean restoreHallFaction(CommandCenterBlockEntity hall, String factionId) {
        try {
            hall.setFaction(factionId);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public record Result(
            boolean accepted,
            boolean changed,
            String reason,
            String factionId,
            int alignment,
            String consumedChipId
    ) {
        private static Result committed(String factionId, int alignment, String chipId) {
            return new Result(true, true, "accepted", factionId, alignment, chipId);
        }

        private static Result alreadyCommitted(String factionId, int alignment) {
            return new Result(true, false, "already_committed", factionId, alignment, "");
        }

        private static Result rejected(String reason, String factionId) {
            return new Result(false, false, reason, factionId, 0, "");
        }
    }
}
