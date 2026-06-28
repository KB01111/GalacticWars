package middleearth.lotr.warmod.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import middleearth.lotr.warmod.faction.FactionId;

public record MiddleEarthRegionCatalog(Map<MiddleEarthRegionId, MiddleEarthRegionDefinition> definitions) {
    public MiddleEarthRegionCatalog(List<MiddleEarthRegionDefinition> definitions) {
        this(indexDefinitions(definitions));
    }

    public MiddleEarthRegionCatalog {
        Objects.requireNonNull(definitions, "definitions");
        LinkedHashMap<MiddleEarthRegionId, MiddleEarthRegionDefinition> copied = new LinkedHashMap<>();
        for (Map.Entry<MiddleEarthRegionId, MiddleEarthRegionDefinition> entry : definitions.entrySet()) {
            MiddleEarthRegionId id = Objects.requireNonNull(entry.getKey(), "catalog key cannot be null");
            MiddleEarthRegionDefinition definition =
                    Objects.requireNonNull(entry.getValue(), "catalog value cannot be null");
            if (!id.equals(definition.id())) {
                throw new IllegalArgumentException("Mismatched region catalog key and definition id: "
                        + id + " vs " + definition.id());
            }
            copied.put(id, definition);
        }
        definitions = Collections.unmodifiableMap(copied);
    }

    public Optional<MiddleEarthRegionDefinition> definition(MiddleEarthRegionId regionId) {
        return Optional.ofNullable(definitions.get(Objects.requireNonNull(regionId, "regionId")));
    }

    public List<MiddleEarthRegionDefinition> regionsForFaction(FactionId factionId) {
        Objects.requireNonNull(factionId, "factionId");
        ArrayList<MiddleEarthRegionDefinition> matches = new ArrayList<>();
        for (MiddleEarthRegionDefinition definition : definitions.values()) {
            if (definition.controllingFaction().equals(factionId)) {
                matches.add(definition);
            }
        }
        return List.copyOf(matches);
    }

    public List<MiddleEarthRegionDefinition> regionsForClimate(MiddleEarthRegionClimate climate) {
        Objects.requireNonNull(climate, "climate");
        ArrayList<MiddleEarthRegionDefinition> matches = new ArrayList<>();
        for (MiddleEarthRegionDefinition definition : definitions.values()) {
            if (definition.climate() == climate) {
                matches.add(definition);
            }
        }
        return List.copyOf(matches);
    }

    private static Map<MiddleEarthRegionId, MiddleEarthRegionDefinition> indexDefinitions(
            List<MiddleEarthRegionDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        LinkedHashMap<MiddleEarthRegionId, MiddleEarthRegionDefinition> indexed = new LinkedHashMap<>();
        for (MiddleEarthRegionDefinition definition : definitions) {
            Objects.requireNonNull(definition, "definition");
            MiddleEarthRegionDefinition previous = indexed.putIfAbsent(definition.id(), definition);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate Middle-earth region id: " + definition.id());
            }
        }
        return indexed;
    }
}
