package galacticwars.clonewars.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import galacticwars.clonewars.GalacticWars;
import galacticwars.clonewars.progression.ProgressionEventType;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

final class LaunchContentValidator {
    private static final Set<String> RUNTIME_UNLOCKS = Set.of(
            "faction_intro", "treasury", "recruitment", "workforce",
            "commander", "planet_travel", "vehicle_crafting", "advanced_trading",
            "vehicle_control", "veteran_trades", "supply_depot");

    private LaunchContentValidator() {
    }

    static LaunchContentDefinitions load(ResourceManager manager, Set<String> factionIds) throws IOException {
        Map<String, LaunchContentDefinitions.PlanetDefinition> planets = loadPlanets(manager, factionIds);
        Map<String, LaunchContentDefinitions.VehicleDefinition> vehicles = loadVehicles(manager);
        Map<String, LaunchContentDefinitions.ForceAbilityDefinition> forceAbilities = loadForceAbilities(manager);
        Map<String, LaunchContentDefinitions.QuestDefinition> quests = loadQuests(manager);
        Map<String, LaunchContentDefinitions.TradeDefinition> trades = loadTrades(manager, factionIds);
        Map<String, LaunchContentDefinitions.ConquestRegionDefinition> regions =
                loadRegions(manager, planets.keySet(), factionIds);
        requireCount("planets", planets, 4);
        requireCount("vehicles", vehicles, 5);
        requireCount("force abilities", forceAbilities, 6);
        requireMinimum("quests", quests, 15);
        requireMinimum("trades", trades, factionIds.size() + regions.size());
        requireCount("conquest regions", regions, 4);
        validateReferences(vehicles, forceAbilities, quests, trades, regions);
        for (String factionId : factionIds) {
            if (trades.values().stream().noneMatch(trade ->
                    trade.factionId().equals(factionId) && trade.stockTier() == 1)) {
                throw new IllegalArgumentException(
                        "Faction " + factionId + " requires a campaign-safe tier-one trade");
            }
        }
        for (String regionId : regions.keySet()) {
            if (trades.values().stream().noneMatch(trade ->
                    trade.stockTier() > 1 && trade.regionalPrerequisite().equals(regionId))) {
                throw new IllegalArgumentException(
                        "Conquest region " + regionId + " requires a veteran trade");
            }
        }
        return new LaunchContentDefinitions(planets, vehicles, forceAbilities, quests, trades, regions);
    }

    static void validateReferences(
            Map<String, LaunchContentDefinitions.VehicleDefinition> vehicles,
            Map<String, LaunchContentDefinitions.ForceAbilityDefinition> forceAbilities,
            Map<String, LaunchContentDefinitions.QuestDefinition> quests,
            Map<String, LaunchContentDefinitions.TradeDefinition> trades
    ) {
        validateReferences(vehicles, forceAbilities, quests, trades, Map.of());
    }

    static void validateReferences(
            Map<String, LaunchContentDefinitions.VehicleDefinition> vehicles,
            Map<String, LaunchContentDefinitions.ForceAbilityDefinition> forceAbilities,
            Map<String, LaunchContentDefinitions.QuestDefinition> quests,
            Map<String, LaunchContentDefinitions.TradeDefinition> trades,
            Map<String, LaunchContentDefinitions.ConquestRegionDefinition> regions
    ) {
        LinkedHashSet<String> knownUnlocks = new LinkedHashSet<>(RUNTIME_UNLOCKS);
        knownUnlocks.addAll(quests.keySet());
        quests.values().forEach(quest -> knownUnlocks.addAll(quest.unlocks()));
        for (LaunchContentDefinitions.QuestDefinition quest : quests.values()) {
            for (LaunchContentDefinitions.QuestObjectiveDefinition objective : quest.objectives()) {
                try {
                    ProgressionEventType.valueOf(objective.eventType().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException unknownType) {
                    throw new IllegalArgumentException("Quest " + quest.id()
                            + " objective " + objective.id()
                            + " references unknown event type " + objective.eventType(), unknownType);
                }
            }
        }
        for (LaunchContentDefinitions.ForceAbilityDefinition ability : forceAbilities.values()) {
            if (!quests.containsKey(ability.requiredQuest())) {
                throw new IllegalArgumentException("Force ability " + ability.id() + " references unknown quest");
            }
            if (!knownUnlocks.contains(ability.activeUnlock())) {
                throw new IllegalArgumentException("Force ability " + ability.id() + " references unknown unlock");
            }
        }
        for (LaunchContentDefinitions.VehicleDefinition vehicle : vehicles.values()) {
            if (!knownUnlocks.contains(vehicle.requiredUnlock())) {
                throw new IllegalArgumentException("Vehicle " + vehicle.id() + " references unknown unlock");
            }
            if (vehicle.deploymentRequirements().stream().anyMatch(requirement -> !knownUnlocks.contains(requirement))) {
                throw new IllegalArgumentException("Vehicle " + vehicle.id()
                        + " references unknown deployment requirement");
            }
        }
        for (LaunchContentDefinitions.TradeDefinition trade : trades.values()) {
            if (!knownUnlocks.contains(trade.requiredUnlock())) {
                throw new IllegalArgumentException("Trade " + trade.id() + " references unknown unlock");
            }
            if (trade.stockTier() == 1 && !trade.regionalPrerequisite().isEmpty()) {
                throw new IllegalArgumentException(
                        "Tier-one trade " + trade.id() + " cannot require regional control");
            }
            if (trade.stockTier() > 1 && trade.regionalPrerequisite().isEmpty()) {
                throw new IllegalArgumentException(
                        "Veteran trade " + trade.id() + " requires a conquest region");
            }
            if (!trade.regionalPrerequisite().isEmpty()
                    && !regions.containsKey(trade.regionalPrerequisite())) {
                throw new IllegalArgumentException("Trade " + trade.id()
                        + " references unknown conquest region " + trade.regionalPrerequisite());
            }
        }
    }

    private static Map<String, LaunchContentDefinitions.PlanetDefinition> loadPlanets(
            ResourceManager manager, Set<String> factions) throws IOException {
        LinkedHashMap<String, LaunchContentDefinitions.PlanetDefinition> result = new LinkedHashMap<>();
        for (JsonObject json : objects(manager, "planets", "planets")) {
            var definition = new LaunchContentDefinitions.PlanetDefinition(
                    string(json, "id"), string(json, "dimension"), string(json, "arrival"),
                    string(json, "theme"), string(json, "faction"));
            if (!factions.contains(definition.factionId())) throw new IllegalArgumentException("Unknown planet faction " + definition.factionId());
            put(result, definition.id(), definition, "planet");
        }
        return result;
    }

    private static Map<String, LaunchContentDefinitions.VehicleDefinition> loadVehicles(ResourceManager manager) throws IOException {
        LinkedHashMap<String, LaunchContentDefinitions.VehicleDefinition> result = new LinkedHashMap<>();
        for (JsonObject json : objects(manager, "vehicles", "vehicles")) {
            var definition = new LaunchContentDefinitions.VehicleDefinition(
                    string(json, "id"), string(json, "movement"), integer(json, "seats"),
                    integer(json, "max_health"), integer(json, "fuel_capacity"), string(json, "unlock"),
                    decimal(json, "max_speed"), decimal(json, "strafe_multiplier"),
                    decimal(json, "vertical_speed"), integer(json, "fuel_rate_ticks"),
                    string(json, "weapon_effect"), strings(json, "seat_roles"),
                    integer(json, "fabrication_credits"), integerMap(json, "fabrication_inputs"),
                    Set.copyOf(strings(json, "deployment_requirements")));
            put(result, definition.id(), definition, "vehicle");
        }
        return result;
    }

    private static Map<String, LaunchContentDefinitions.ForceAbilityDefinition> loadForceAbilities(ResourceManager manager) throws IOException {
        LinkedHashMap<String, LaunchContentDefinitions.ForceAbilityDefinition> result = new LinkedHashMap<>();
        for (JsonObject json : objects(manager, "force_abilities", "abilities")) {
            var definition = new LaunchContentDefinitions.ForceAbilityDefinition(
                    string(json, "id"), string(json, "path"), integer(json, "energy"),
                    integer(json, "cooldown_ticks"), string(json, "quest"), true,
                    string(json, "effect"), decimal(json, "range"),
                    integer(json, "duration_ticks"), string(json, "active_unlock"));
            put(result, definition.id(), definition, "Force ability");
        }
        return result;
    }

    private static Map<String, LaunchContentDefinitions.QuestDefinition> loadQuests(ResourceManager manager) throws IOException {
        LinkedHashMap<String, LaunchContentDefinitions.QuestDefinition> result = new LinkedHashMap<>();
        for (JsonObject json : objects(manager, "quests", "quests", 2)) {
            var definition = new LaunchContentDefinitions.QuestDefinition(
                    string(json, "id"), questObjectives(json), integer(json, "reward_credits"),
                    Set.copyOf(strings(json, "unlocks")));
            put(result, definition.id(), definition, "quest");
        }
        return result;
    }

    private static Map<String, LaunchContentDefinitions.TradeDefinition> loadTrades(
            ResourceManager manager, Set<String> factions) throws IOException {
        LinkedHashMap<String, LaunchContentDefinitions.TradeDefinition> result = new LinkedHashMap<>();
        for (JsonObject json : objects(manager, "trades", "trades")) {
            var definition = new LaunchContentDefinitions.TradeDefinition(
                    string(json, "id"), string(json, "faction"), integer(json, "cost"),
                    string(json, "item"), integer(json, "count"), string(json, "unlock"),
                    integer(json, "stock_tier"), optionalString(json, "regional_prerequisite"));
            if (!factions.contains(definition.factionId())) throw new IllegalArgumentException("Unknown trade faction " + definition.factionId());
            put(result, definition.id(), definition, "trade");
        }
        return result;
    }

    private static Map<String, LaunchContentDefinitions.ConquestRegionDefinition> loadRegions(
            ResourceManager manager, Set<String> planetIds, Set<String> factionIds) throws IOException {
        LinkedHashMap<String, LaunchContentDefinitions.ConquestRegionDefinition> result = new LinkedHashMap<>();
        for (JsonObject json : objects(manager, "conquest_regions", "regions")) {
            var definition = new LaunchContentDefinitions.ConquestRegionDefinition(
                    string(json, "id"), string(json, "planet"), integer(json, "protected_radius"),
                    integer(json, "capture_ticks"), integer(json, "reward_credits"),
                    integer(json, "landmark_x"), integer(json, "landmark_z"),
                    integer(json, "capture_radius"), string(json, "defender_faction"));
            if (!planetIds.contains(definition.planetId())) throw new IllegalArgumentException("Unknown region planet " + definition.planetId());
            if (!factionIds.contains(definition.defenderFaction())) throw new IllegalArgumentException("Unknown region defender faction " + definition.defenderFaction());
            put(result, definition.id(), definition, "conquest region");
        }
        return result;
    }

    private static List<JsonObject> objects(ResourceManager manager, String directory, String array) throws IOException {
        return objects(manager, directory, array, 1);
    }

    private static List<JsonObject> objects(
            ResourceManager manager,
            String directory,
            String array,
            int expectedSchemaVersion
    ) throws IOException {
        FileToIdConverter converter = FileToIdConverter.json("galacticwars/" + directory);
        Map<Identifier, Resource> resources = converter.listMatchingResources(manager).entrySet().stream()
                .filter(entry -> entry.getKey().getNamespace().equals(GalacticWars.MODID))
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (first, ignored) -> first,
                        java.util.LinkedHashMap::new));
        if (resources.isEmpty()) throw new IllegalArgumentException("Missing launch content directory " + directory);
        java.util.ArrayList<JsonObject> result = new java.util.ArrayList<>();
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (!root.has("schema_version")
                        || root.get("schema_version").getAsInt() != expectedSchemaVersion) {
                    throw new IllegalArgumentException("Unsupported " + directory + " schema in " + entry.getKey());
                }
                JsonArray values = root.getAsJsonArray(array);
                if (values == null) throw new IllegalArgumentException("Missing " + array + " in " + entry.getKey());
                for (JsonElement value : values) result.add(value.getAsJsonObject());
            }
        }
        return List.copyOf(result);
    }

    private static List<LaunchContentDefinitions.QuestObjectiveDefinition> questObjectives(
            JsonObject quest
    ) {
        JsonArray values = quest.getAsJsonArray("objectives");
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Missing objectives for quest " + string(quest, "id"));
        }
        java.util.ArrayList<LaunchContentDefinitions.QuestObjectiveDefinition> result =
                new java.util.ArrayList<>();
        for (JsonElement element : values) {
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException(
                        "Quest objectives must use schema-two object definitions");
            }
            JsonObject objective = element.getAsJsonObject();
            result.add(new LaunchContentDefinitions.QuestObjectiveDefinition(
                    string(objective, "id"),
                    string(objective, "event"),
                    objective.has("subjects")
                            ? Set.copyOf(strings(objective, "subjects"))
                            : Set.of(),
                    objective.has("count") ? integer(objective, "count") : 1));
        }
        return List.copyOf(result);
    }

    private static String string(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).getAsString().isBlank()) throw new IllegalArgumentException("Missing " + key);
        return json.get(key).getAsString();
    }

    private static int integer(JsonObject json, String key) {
        if (!json.has(key)) throw new IllegalArgumentException("Missing " + key);
        return json.get(key).getAsInt();
    }

    private static double decimal(JsonObject json, String key) {
        if (!json.has(key)) throw new IllegalArgumentException("Missing " + key);
        return json.get(key).getAsDouble();
    }

    private static String optionalString(JsonObject json, String key) {
        return json.has(key) ? json.get(key).getAsString().trim() : "";
    }

    private static List<String> strings(JsonObject json, String key) {
        JsonArray values = json.getAsJsonArray(key);
        if (values == null) throw new IllegalArgumentException("Missing " + key);
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        values.forEach(value -> result.add(value.getAsString()));
        return List.copyOf(result);
    }

    private static Map<String, Integer> integerMap(JsonObject json, String key) {
        JsonObject values = json.getAsJsonObject(key);
        if (values == null) throw new IllegalArgumentException("Missing " + key);
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        values.entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsInt()));
        return Map.copyOf(result);
    }

    private static <V> void put(Map<String, V> target, String id, V value, String label) {
        if (target.putIfAbsent(id, value) != null) throw new IllegalArgumentException("Duplicate " + label + " " + id);
    }

    private static void requireCount(String label, Map<?, ?> values, int expected) {
        if (values.size() != expected) throw new IllegalArgumentException(label + " expected " + expected + " entries but found " + values.size());
    }

    private static void requireMinimum(String label, Map<?, ?> values, int minimum) {
        if (values.size() < minimum) {
            throw new IllegalArgumentException(label + " expected at least " + minimum
                    + " entries but found " + values.size());
        }
    }
}
