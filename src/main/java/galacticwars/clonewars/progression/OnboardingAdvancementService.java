package galacticwars.clonewars.progression;

import galacticwars.clonewars.GalacticWars;
import java.util.ArrayList;
import java.util.Objects;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** Awards data-driven onboarding milestones from authoritative campaign state. */
public final class OnboardingAdvancementService {
    private static final Identifier FACTION_PLEDGED = Identifier.fromNamespaceAndPath(
            GalacticWars.MODID, "onboarding/faction_pledged");
    private static final Identifier FIRST_RECRUIT = Identifier.fromNamespaceAndPath(
            GalacticWars.MODID, "onboarding/first_recruit");

    private OnboardingAdvancementService() {
    }

    public static void synchronize(ServerPlayer player, ProgressionState progression) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(progression, "progression");
        if (!player.getUUID().equals(progression.playerId())) {
            throw new SecurityException("Cannot award onboarding progress for another player");
        }
        if (!progression.factionId().isBlank()) {
            award(player, FACTION_PLEDGED);
        }
        if (progression.total(ProgressionEventType.RECRUIT_HIRED) > 0) {
            award(player, FIRST_RECRUIT);
        }
    }

    private static void award(ServerPlayer player, Identifier id) {
        AdvancementHolder advancement = player.level().getServer().getAdvancements().get(id);
        if (advancement == null) {
            return;
        }
        var progress = player.getAdvancements().getOrStartProgress(advancement);
        if (progress.isDone()) {
            return;
        }
        ArrayList<String> remaining = new ArrayList<>();
        progress.getRemainingCriteria().forEach(remaining::add);
        remaining.forEach(criterion -> player.getAdvancements().award(advancement, criterion));
    }
}
