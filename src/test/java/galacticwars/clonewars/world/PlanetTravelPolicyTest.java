package galacticwars.clonewars.world;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.data.LaunchContentRuntime;
import java.util.List;
import java.util.Map;

public final class PlanetTravelPolicyTest {
    private PlanetTravelPolicyTest() {
    }

    public static void main(String[] args) {
        LaunchContentRuntime.install(new LaunchContentDefinitions(
                        Map.of("tatooine", new LaunchContentDefinitions.PlanetDefinition(
                                "tatooine", "galacticwars:tatooine", "spaceport", "desert", "hutt_cartel")),
                        Map.of(), Map.of(), Map.of(), Map.of(), Map.of()),
                List.of(), Map.of());
        assertRejected("unknown_planet", "alderaan", true, true, true, true, true, true, false);
        assertRejected("not_owner", "tatooine", false, true, true, true, true, true, false);
        assertRejected("not_owner", "tatooine", true, false, true, true, true, true, false);
        assertRejected("faction_required", "tatooine", true, true, false, true, true, true, false);
        assertRejected("planet_travel_locked", "tatooine", true, true, true, false, true, true, false);
        assertRejected("upkeep_unpaid", "tatooine", true, true, true, true, false, true, false);
        assertRejected("destination_unavailable", "tatooine", true, true, true, true, true, false, false);
        assertRejected("already_there", "tatooine", true, true, true, true, true, true, true);
        assertTrue(PlanetTravelPolicy.authorize(
                "tatooine", true, true, true, true, true, true, false).accepted(),
                "fully authorized travel should pass");
        assertRejected("not_owner", PlanetTravelPolicy.HOME_DESTINATION_ID,
                false, true, false, false, false, true, false);
        assertRejected("destination_unavailable", PlanetTravelPolicy.HOME_DESTINATION_ID,
                true, true, false, false, false, false, false);
        assertRejected("already_there", PlanetTravelPolicy.HOME_DESTINATION_ID,
                true, true, false, false, false, true, true);
        assertTrue(PlanetTravelPolicy.authorize(
                PlanetTravelPolicy.HOME_DESTINATION_ID,
                true, true, false, false, false, true, false).accepted(),
                "authorized return home must remain available when outbound gates lapse");
        var available = new PlanetTravelService.NavigationDestination(
                "tatooine", true, "accepted", "desert", "spaceport");
        assertTrue(available.available() && available.destinationId().equals("tatooine"),
                "server navigation option should preserve accepted state");
        assertTrue(available.theme().equals("desert")
                        && available.arrivalProfile().equals("spaceport"),
                "server navigation option should preserve destination metadata");
        assertTrue(PlanetArrivalService.ArrivalProfile.byId("spaceport").offsetX() == 1,
                "spaceport profile should use its registered approach");
        boolean mismatchRejected = false;
        try {
            new PlanetTravelService.NavigationDestination("tatooine", true, "upkeep_unpaid");
        } catch (IllegalArgumentException expected) {
            mismatchRejected = true;
        }
        assertTrue(mismatchRejected, "navigation option reason must agree with availability");
        System.out.println("PlanetTravelPolicyTest passed");
    }

    private static void assertRejected(
            String reason,
            String planetId,
            boolean owner,
            boolean authoritative,
            boolean pledged,
            boolean unlocked,
            boolean upkeep,
            boolean loaded,
            boolean alreadyThere
    ) {
        PlanetTravelPolicy.TravelAuthorization result = PlanetTravelPolicy.authorize(
                planetId, owner, authoritative, pledged, unlocked, upkeep, loaded, alreadyThere);
        assertTrue(!result.accepted() && result.reason().equals(reason), reason);
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
