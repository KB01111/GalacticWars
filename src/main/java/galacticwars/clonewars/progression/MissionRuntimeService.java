package galacticwars.clonewars.progression;

import galacticwars.clonewars.data.LaunchContentDefinitions;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

/** Converts completed embodied mission requirements into replay-safe persisted progression facts. */
public final class MissionRuntimeService {
    private MissionRuntimeService() {
    }

    /** Starts the next unlocked faction chapter mission exactly once. Retries are runtime-gated. */
    public static ProgressionState startEligibleMission(ProgressionState state) {
        for (LaunchContentDefinitions.MissionDefinition mission
                : LaunchContentCatalog.data().missions().values().stream()
                .sorted(Comparator.comparing(LaunchContentDefinitions.MissionDefinition::questId))
                .toList()) {
            if (!matchesFaction(state, mission.questId())
                    || state.hasSubject(ProgressionEventType.MISSION_COMPLETED, mission.id())
                    || !questAvailable(state, mission.questId())) {
                continue;
            }
            int started = state.subjectTotal(ProgressionEventType.MISSION_STARTED, java.util.Set.of(mission.id()));
            int failed = state.subjectTotal(ProgressionEventType.MISSION_FAILED, java.util.Set.of(mission.id()));
            if (started > failed || started > 0) {
                return state;
            }
            return applyMissionFact(state, ProgressionEventType.MISSION_STARTED,
                    mission.id(), 1);
        }
        return state;
    }

    /** Starts a persisted retry after the server-owned cooldown has elapsed. */
    public static ProgressionState startRetry(ProgressionState state, String missionId) {
        LaunchContentDefinitions.MissionDefinition mission =
                LaunchContentCatalog.data().missions().get(missionId);
        if (mission == null || !matchesFaction(state, mission.questId())
                || state.hasSubject(ProgressionEventType.MISSION_COMPLETED, missionId)
                || !questAvailable(state, mission.questId())) {
            return state;
        }
        int started = state.subjectTotal(ProgressionEventType.MISSION_STARTED, java.util.Set.of(missionId));
        int failed = state.subjectTotal(ProgressionEventType.MISSION_FAILED, java.util.Set.of(missionId));
        if (started > failed) {
            return state;
        }
        return applyMissionFact(state, ProgressionEventType.MISSION_STARTED,
                missionId, started + 1);
    }

    public static ProgressionState failActiveMission(ProgressionState state, String missionId) {
        int started = state.subjectTotal(ProgressionEventType.MISSION_STARTED, java.util.Set.of(missionId));
        int failed = state.subjectTotal(ProgressionEventType.MISSION_FAILED, java.util.Set.of(missionId));
        if (started <= failed || state.hasSubject(ProgressionEventType.MISSION_COMPLETED, missionId)) {
            return state;
        }
        return applyMissionFact(state, ProgressionEventType.MISSION_FAILED,
                missionId, failed + 1);
    }

    public static boolean active(ProgressionState state, String missionId) {
        return !state.hasSubject(ProgressionEventType.MISSION_COMPLETED, missionId)
                && state.subjectTotal(ProgressionEventType.MISSION_STARTED, java.util.Set.of(missionId))
                > state.subjectTotal(ProgressionEventType.MISSION_FAILED, java.util.Set.of(missionId));
    }

    public static ProgressionState completeEligibleMissions(ProgressionState state) {
        ProgressionState current = state;
        boolean advanced;
        do {
            advanced = false;
            for (LaunchContentDefinitions.MissionDefinition mission
                    : LaunchContentCatalog.data().missions().values()) {
                if (!matchesFaction(current, mission.questId())
                        || current.hasSubject(ProgressionEventType.MISSION_COMPLETED, mission.id())
                        || !active(current, mission.id())
                        || !current.hasSubject(
                        ProgressionEventType.MISSION_OBJECTIVE_COMPLETED, mission.id())
                        || !requirementsComplete(current, mission)) {
                    continue;
                }
                ProgressionEvent completion = new ProgressionEvent(
                        deterministicEventId(current.playerId(), mission.id()),
                        current.playerId(), ProgressionEventType.MISSION_COMPLETED, mission.id(), 1);
                ProgressionDecision decision = GalacticProgressionCoordinator.apply(current, completion);
                if (decision.accepted() && decision.changed()) {
                    current = decision.state();
                    advanced = true;
                    break;
                }
            }
        } while (advanced);
        return current;
    }

    public static boolean requirementsComplete(
            ProgressionState state,
            LaunchContentDefinitions.MissionDefinition mission
    ) {
        for (LaunchContentDefinitions.MissionRequirementDefinition requirement
                : mission.requirements()) {
            ProgressionEventType type;
            try {
                type = ProgressionEventType.valueOf(
                        requirement.eventType().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException unknown) {
                return false;
            }
            if (state.subjectTotal(type, requirement.subjectIds()) < requirement.requiredCount()) {
                return false;
            }
        }
        return true;
    }

    public static UUID deterministicEventId(UUID playerId, String missionId) {
        return UUID.nameUUIDFromBytes(("campaign:mission:" + playerId + ":" + missionId)
                .getBytes(StandardCharsets.UTF_8));
    }

    public static UUID deterministicLifecycleEventId(
            UUID playerId, ProgressionEventType type, String missionId, int attempt
    ) {
        return UUID.nameUUIDFromBytes(("campaign:mission:" + type.name().toLowerCase(Locale.ROOT)
                + ":" + playerId + ":" + missionId + ":" + attempt)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static ProgressionState applyMissionFact(
            ProgressionState state, ProgressionEventType type, String missionId, int attempt
    ) {
        ProgressionEvent event = new ProgressionEvent(
                deterministicLifecycleEventId(state.playerId(), type, missionId, attempt),
                state.playerId(), type, missionId, 1);
        ProgressionDecision decision = GalacticProgressionCoordinator.apply(state, event);
        return decision.accepted() && decision.changed() ? decision.state() : state;
    }

    private static boolean questAvailable(ProgressionState state, String questId) {
        if (questId.endsWith("_chapter_1")) {
            return state.total(ProgressionEventType.FACTION_PLEDGED) > 0;
        }
        return LaunchContentCatalog.questPrerequisites(questId).stream()
                .allMatch(prerequisite -> state.hasSubject(
                        ProgressionEventType.QUEST_ADVANCED, prerequisite));
    }

    private static boolean matchesFaction(ProgressionState state, String questId) {
        if (state.factionId().isBlank()) {
            return false;
        }
        String faction = state.factionId().substring(state.factionId().indexOf(':') + 1);
        return questId.startsWith(faction + "_chapter_");
    }
}
