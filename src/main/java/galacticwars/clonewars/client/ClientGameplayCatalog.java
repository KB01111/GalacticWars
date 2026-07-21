package galacticwars.clonewars.client;

import galacticwars.clonewars.network.GameplayCatalogPayload;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Client-only immutable projection installed exclusively through the packet bridge. */
public final class ClientGameplayCatalog {
    private static final AtomicLong REVISIONS = new AtomicLong();
    private static final AtomicReference<Snapshot> SNAPSHOT =
            new AtomicReference<>(Snapshot.empty());

    private ClientGameplayCatalog() {
    }

    public static void replace(GameplayCatalogPayload payload) {
        Objects.requireNonNull(payload, "payload");
        LinkedHashMap<String, GameplayCatalogPayload.VehicleEntry> vehicles = new LinkedHashMap<>();
        payload.vehicles().forEach(vehicle -> vehicles.put(vehicle.vehicleId(), vehicle));
        LinkedHashMap<String, GameplayCatalogPayload.BlueprintEntry> blueprints = new LinkedHashMap<>();
        payload.blueprints().forEach(blueprint -> blueprints.put(blueprint.blueprintId(), blueprint));
        SNAPSHOT.set(new Snapshot(
                REVISIONS.incrementAndGet(),
                payload.generation(),
                payload.contentHash(),
                payload.classes(),
                vehicles,
                blueprints));
    }

    public static Snapshot snapshot() {
        return SNAPSHOT.get();
    }

    public static void clear() {
        SNAPSHOT.set(Snapshot.empty());
    }

    public record Snapshot(
            long revision,
            long serverGeneration,
            String serverContentHash,
            List<GameplayCatalogPayload.ClassEntry> classes,
            Map<String, GameplayCatalogPayload.VehicleEntry> vehicles,
            Map<String, GameplayCatalogPayload.BlueprintEntry> blueprints
    ) {
        public Snapshot {
            if (revision < 0L || serverGeneration < -1L) {
                throw new IllegalArgumentException("invalid client gameplay catalog revision");
            }
            serverContentHash = Objects.requireNonNull(
                    serverContentHash, "serverContentHash");
            classes = List.copyOf(Objects.requireNonNull(classes, "classes"));
            vehicles = Collections.unmodifiableMap(new LinkedHashMap<>(
                    Objects.requireNonNull(vehicles, "vehicles")));
            blueprints = Collections.unmodifiableMap(new LinkedHashMap<>(
                    Objects.requireNonNull(blueprints, "blueprints")));
        }

        public Optional<GameplayCatalogPayload.VehicleEntry> vehicle(String vehicleId) {
            if (vehicleId == null || vehicleId.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(vehicles.get(vehicleId));
        }

        public Optional<GameplayCatalogPayload.BlueprintEntry> blueprint(String blueprintId) {
            if (blueprintId == null || blueprintId.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(blueprints.get(blueprintId));
        }

        private static Snapshot empty() {
            return new Snapshot(0L, -1L, "", List.of(), Map.of(), Map.of());
        }
    }
}
