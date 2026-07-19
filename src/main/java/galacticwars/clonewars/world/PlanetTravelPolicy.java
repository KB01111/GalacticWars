package galacticwars.clonewars.world;

import galacticwars.clonewars.progression.LaunchContentCatalog;

public final class PlanetTravelPolicy {
    public static final String HOME_DESTINATION_ID = "home";

    private PlanetTravelPolicy() {
    }

    public static TravelAuthorization authorize(
            String destinationId,
            boolean owner,
            boolean authoritativeCommandCenter,
            boolean factionPledged,
            boolean travelUnlocked,
            boolean upkeepPaid,
            boolean destinationLoaded,
            boolean alreadyThere
    ) {
        boolean returningHome = HOME_DESTINATION_ID.equals(destinationId);
        if (!returningHome && !LaunchContentCatalog.planets().contains(destinationId)) {
            return TravelAuthorization.rejected("unknown_planet");
        }
        if (!owner || !authoritativeCommandCenter) {
            return TravelAuthorization.rejected("not_owner");
        }
        if (!returningHome && !factionPledged) {
            return TravelAuthorization.rejected("faction_required");
        }
        if (!returningHome && !travelUnlocked) {
            return TravelAuthorization.rejected("planet_travel_locked");
        }
        if (!returningHome && !upkeepPaid) {
            return TravelAuthorization.rejected("upkeep_unpaid");
        }
        if (!destinationLoaded) {
            return TravelAuthorization.rejected("destination_unavailable");
        }
        if (alreadyThere) {
            return TravelAuthorization.rejected("already_there");
        }
        return TravelAuthorization.allow();
    }

    public record TravelAuthorization(boolean accepted, String reason) {
        private static TravelAuthorization allow() {
            return new TravelAuthorization(true, "accepted");
        }

        private static TravelAuthorization rejected(String reason) {
            return new TravelAuthorization(false, reason);
        }
    }
}
