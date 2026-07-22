package galacticwars.clonewars.progression;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.network.GalacticNetwork;
import galacticwars.clonewars.network.ObjectiveMarkerPayload;
import galacticwars.clonewars.world.PlanetArrivalService;
import java.util.Comparator;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;

/** Resolves campaign guidance from authoritative state without exposing unbounded world data. */
public final class ObjectiveMarkerService {
    private ObjectiveMarkerService() {
    }

    public static void synchronize(ServerPlayer player, ProgressionState progression) {
        if (!GalacticNetwork.canPlayerReceive(player, ObjectiveMarkerPayload.TYPE)) {
            return;
        }
        GalacticNetwork.CHANNEL.sendToPlayer(() -> player, resolve(player, progression));
    }

    public static ObjectiveMarkerPayload resolve(ServerPlayer player, ProgressionState progression) {
        String faction = path(progression.factionId());
        if (faction.isBlank()) {
            return ObjectiveMarkerPayload.inactive();
        }
        LaunchContentDefinitions definitions = LaunchContentCatalog.data();
        LaunchContentDefinitions.QuestDefinition quest = definitions.quests().values().stream()
                .filter(candidate -> path(candidate.id()).startsWith(faction + "_chapter_"))
                .filter(candidate -> !progression.hasSubject(
                        ProgressionEventType.QUEST_ADVANCED, candidate.id()))
                .sorted(Comparator.comparing(LaunchContentDefinitions.QuestDefinition::id))
                .findFirst().orElse(null);
        if (quest == null) {
            return ObjectiveMarkerPayload.inactive();
        }
        LaunchContentDefinitions.QuestObjectiveDefinition objective = quest.objectives().stream()
                .filter(candidate -> !GalacticProgressionCoordinator.objectiveComplete(
                        progression, candidate))
                .findFirst().orElse(null);
        if (objective == null) {
            return ObjectiveMarkerPayload.inactive();
        }

        Target target = targetForObjective(player, definitions, quest, objective);
        return new ObjectiveMarkerPayload(
                true, objective.id(), target != null,
                target == null ? "" : target.dimensionId(),
                target == null ? 0 : target.x(),
                target == null ? 0 : target.y(),
                target == null ? 0 : target.z());
    }

    private static Target targetForObjective(
            ServerPlayer player,
            LaunchContentDefinitions definitions,
            LaunchContentDefinitions.QuestDefinition quest,
            LaunchContentDefinitions.QuestObjectiveDefinition objective
    ) {
        if (objective.eventType().equals("planet_visited") && !objective.subjectIds().isEmpty()) {
            LaunchContentDefinitions.PlanetDefinition planet = definitions.planets().get(
                    objective.subjectIds().iterator().next());
            Target target = planetTarget(definitions, planet);
            if (target != null) {
                return target;
            }
        }
        LaunchContentDefinitions.MissionDefinition mission = definitions.missions().values().stream()
                .filter(candidate -> candidate.questId().equals(quest.id()))
                .findFirst().orElse(null);
        if (mission != null && (objective.eventType().equals("mission_completed")
                || objective.eventType().equals("region_captured")
                || objective.eventType().equals("vehicle_acquired"))) {
            if (!mission.targetRegionId().isBlank()) {
                Target target = regionTarget(definitions, mission.targetRegionId());
                if (target != null) {
                    return target;
                }
            }
            if (!mission.planetId().isBlank()) {
                Target target = planetTarget(definitions, definitions.planets().get(mission.planetId()));
                if (target != null) {
                    return target;
                }
            }
        }
        return KingdomSavedData.get(player.level()).kingdomForPlayer(player.getUUID())
                .map(kingdom -> kingdom.settlement())
                .map(settlement -> new Target(
                        settlement.dimensionId(), settlement.hallX(), settlement.hallY(), settlement.hallZ()))
                .orElse(null);
    }

    private static Target regionTarget(LaunchContentDefinitions definitions, String regionId) {
        LaunchContentDefinitions.ConquestRegionDefinition region =
                definitions.conquestRegions().get(regionId);
        if (region == null) {
            return null;
        }
        LaunchContentDefinitions.PlanetDefinition planet = definitions.planets().get(region.planetId());
        return planet == null ? null
                : new Target(planet.dimensionId(), region.landmarkX(), 64, region.landmarkZ());
    }

    private static Target planetTarget(
            LaunchContentDefinitions definitions,
            LaunchContentDefinitions.PlanetDefinition planet
    ) {
        if (planet == null) {
            return null;
        }
        var profile = PlanetArrivalService.ArrivalProfile.byId(planet.arrival());
        LaunchContentDefinitions.ConquestRegionDefinition region = definitions.conquestRegions().values().stream()
                .filter(candidate -> candidate.planetId().equals(planet.id()))
                .sorted(Comparator.comparing(LaunchContentDefinitions.ConquestRegionDefinition::id))
                .findFirst().orElse(null);
        if (region == null) {
            return new Target(planet.dimensionId(), 0, 64, 0);
        }
        int clearance = Math.addExact(region.protectedRadius(), 32);
        return new Target(
                planet.dimensionId(),
                Math.addExact(region.landmarkX(), profile.offsetX() * clearance),
                64,
                Math.addExact(region.landmarkZ(), profile.offsetZ() * clearance));
    }

    private static String path(String id) {
        String normalized = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
        int separator = normalized.indexOf(':');
        return separator < 0 ? normalized : normalized.substring(separator + 1);
    }

    private record Target(String dimensionId, int x, int y, int z) {
    }
}
