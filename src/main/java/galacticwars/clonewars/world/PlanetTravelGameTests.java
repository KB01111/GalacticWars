package galacticwars.clonewars.world;

import galacticwars.clonewars.army.ArmyFormation;
import galacticwars.clonewars.army.ArmyGroupLifecycleState;
import galacticwars.clonewars.army.ArmyLocation;
import galacticwars.clonewars.data.GameplayDataManager;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.faction.FactionAlignmentSavedData;
import galacticwars.clonewars.faction.FactionId;
import galacticwars.clonewars.kingdom.BuildProject;
import galacticwars.clonewars.kingdom.KingdomSavedData;
import galacticwars.clonewars.menu.CommandCenterNavigationMenu;
import galacticwars.clonewars.menu.RecruitCommandMenu;
import galacticwars.clonewars.progression.ProgressionEvent;
import galacticwars.clonewars.progression.ProgressionEventType;
import galacticwars.clonewars.progression.ProgressionSavedData;
import galacticwars.clonewars.registry.ModBlocks;
import galacticwars.clonewars.registry.ModEntityTypes;
import galacticwars.clonewars.registry.ModItems;
import galacticwars.clonewars.settlement.CommandCenterBlockEntity;
import galacticwars.clonewars.settlement.KingdomBaseBlueprint;
import galacticwars.clonewars.vehicle.GalacticVehicleEntity;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public final class PlanetTravelGameTests {
    private static final int LANDING_RADIUS = 8;

    private PlanetTravelGameTests() {
    }

    public static void roundTripHome(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel();
        ServerLevel planet = overworld.getServer().getLevel(Level.NETHER);
        if (planet == null) {
            helper.fail("The GameTest server did not provide its loaded planet stand-in dimension");
            return;
        }

        BlockPos hallPos = helper.absolutePos(new BlockPos(1, 1, 1));
        prepareLandingArea(overworld, hallPos);
        CommandCenterBlockEntity hall = placeCommandCenter(overworld, hallPos);
        ServerPlayer owner = makePlayer(helper);
        ServerPlayer passenger = makePlayer(helper);
        if (!hall.claim(owner)) {
            helper.fail("Round-trip setup could not claim the authoritative Command Center");
            return;
        }
        hall.setFaction("galacticwars:republic");

        KingdomSavedData kingdoms = KingdomSavedData.get(overworld);
        kingdoms.foundKingdom(
                owner.getUUID(), hall.factionId(),
                overworld.dimension().identifier().toString(), hallPos);
        ProgressionSavedData progression = ProgressionSavedData.get(overworld);
        applyProgression(progression, owner, ProgressionEventType.FACTION_PLEDGED, "galacticwars:republic");
        applyProgression(progression, owner, ProgressionEventType.BUILDING_COMPLETED, "forward_base");
        applyProgression(progression, passenger, ProgressionEventType.FACTION_PLEDGED, "galacticwars:republic");

        KingdomBaseBlueprint forwardBase = GameplayDataManager.snapshot()
                .blueprint("galacticwars:forward_base").orElseThrow();
        BuildProject completedBase = fullyProgressProject(
                kingdoms, owner.getUUID(), forwardBase,
                overworld.dimension().identifier().toString(), hallPos.offset(0, 0, 32));
        if (!kingdoms.completeBuildProject(owner.getUUID(), completedBase, forwardBase)) {
            helper.fail("Round-trip setup could not complete its Forward Base");
            return;
        }

        FactionAlignmentSavedData.get(overworld).setScore(
                owner.getUUID(), FactionId.of("republic"), 100);
        GalacticRecruitEntity commander = spawnRecruit(
                overworld, ModEntityTypes.CLONE_TROOPER.get(), hallPos.offset(2, 0, 0));
        GalacticRecruitEntity member = spawnRecruit(
                overworld, ModEntityTypes.ARC_TROOPER.get(), hallPos.offset(3, 0, 0));
        if (!teleportPlayer(owner, overworld, commander.getX(), commander.getY(), commander.getZ())) {
            helper.fail("Round-trip setup could not move the owner into the source chunk");
            return;
        }
        if (!commander.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE)
                || !member.handleMenuButton(owner, RecruitCommandMenu.BUTTON_HIRE)
                || !kingdoms.promoteCommander(owner.getUUID(), commander.getUUID())) {
            helper.fail("Round-trip setup could not hire and promote its squad");
            return;
        }
        var group = kingdoms.createOrReclaimArmyGroup(
                owner.getUUID(), commander.getUUID(), ArmyFormation.LINE,
                new ArmyLocation(overworld.dimension().identifier().toString(),
                        commander.getX(), commander.getY(), commander.getZ()),
                overworld.getGameTime()).orElseThrow();

        GalacticVehicleEntity vehicle = ModEntityTypes.LAAT_GUNSHIP.get().create(
                overworld, EntitySpawnReason.TRIGGERED);
        if (vehicle == null) {
            helper.fail("Round-trip setup could not create its passenger vehicle");
            return;
        }
        vehicle.setPos(hallPos.getX() + 6.5D, hallPos.getY(), hallPos.getZ() + 0.5D);
        vehicle.deploy(owner.getUUID(), "galacticwars:republic");
        if (!overworld.addFreshEntity(vehicle)) {
            helper.fail("Round-trip setup could not add its passenger vehicle");
            return;
        }
        if (!teleportPlayer(owner, overworld, vehicle.getX(), vehicle.getY(), vehicle.getZ())
                || !teleportPlayer(passenger, overworld, vehicle.getX(), vehicle.getY(), vehicle.getZ())) {
            helper.fail("Round-trip setup could not create authoritative source-chunk player tickets");
            return;
        }
        if (!owner.startRiding(vehicle, true, true)
                || !passenger.startRiding(vehicle, true, true)) {
            helper.fail("Round-trip setup could not mount the driver and passenger");
            return;
        }

        owner.getInventory().add(new ItemStack(ModItems.CREDIT_CHIP.get(), 17));
        passenger.getInventory().add(new ItemStack(Items.DIAMOND, 3));
        int initialFuel = vehicle.fuel();
        int initialHealth = vehicle.health();
        int initialFuelCapacity = vehicle.syncedFuelCapacity();
        int initialMaximumHealth = vehicle.syncedMaximumHealth();
        UUID vehicleId = vehicle.getUUID();

        BlockPos planetArrival = new BlockPos(256, 160, 256);
        ChunkPos sourceChunk = new ChunkPos(hallPos.getX() >> 4, hallPos.getZ() >> 4);
        ChunkPos destinationChunk = new ChunkPos(
                planetArrival.getX() >> 4, planetArrival.getZ() >> 4);
        overworld.setChunkForced(sourceChunk.x(), sourceChunk.z(), true);
        planet.setChunkForced(destinationChunk.x(), destinationChunk.z(), true);
        long entityIndexWaitStartedTick = helper.getTick();
        boolean[] transferStarted = {false};
        helper.onEachTick(() -> {
            if (transferStarted[0]) {
                return;
            }
            boolean commanderIndexed = overworld.getEntity(commander.getUUID()) == commander;
            boolean memberIndexed = overworld.getEntity(member.getUUID()) == member;
            boolean vehicleIndexed = overworld.getEntity(vehicleId) == vehicle;
            boolean sourceTicking = overworld.areEntitiesActuallyLoadedAndTicking(sourceChunk);
            if (!sourceTicking || !commanderIndexed || !memberIndexed || !vehicleIndexed) {
                if (helper.getTick() - entityIndexWaitStartedTick >= 120L) {
                    transferStarted[0] = true;
                    overworld.setChunkForced(sourceChunk.x(), sourceChunk.z(), false);
                    planet.setChunkForced(destinationChunk.x(), destinationChunk.z(), false);
                    helper.fail("Round-trip party never entered the authoritative entity index: commander="
                            + commanderIndexed + ", member=" + memberIndexed
                            + ", vehicle=" + vehicleIndexed + ", ticking=" + sourceTicking);
                }
                return;
            }
            transferStarted[0] = true;
        try {
        prepareLandingArea(planet, planetArrival);
        PlanetTravelService.TravelResult outbound = PlanetTravelService.executeAuthorizedTransfer(
                owner, "tatooine", planet, planetArrival);
        GalacticVehicleEntity planetVehicle = owner.getVehicle() instanceof GalacticVehicleEntity traveled
                ? traveled : null;
        if (!outbound.accepted() || !outbound.squadTransferred()
                || owner.level() != planet || passenger.level() != planet
                || planetVehicle == null || planetVehicle.level() != planet
                || passenger.getVehicle() != planetVehicle
                || !planetVehicle.getUUID().equals(vehicleId)
                || planetVehicle.fuel() != initialFuel || planetVehicle.health() != initialHealth
                || planetVehicle.syncedFuelCapacity() != initialFuelCapacity
                || planetVehicle.syncedMaximumHealth() != initialMaximumHealth
                || !respawnMatches(owner, Level.NETHER, planetArrival)
                || !respawnMatches(passenger, Level.NETHER, planetArrival)) {
            helper.fail("Outbound transfer did not preserve its complete travel party: accepted="
                    + outbound.accepted() + ", squad=" + outbound.squadTransferred()
                    + ", levels=" + owner.level().dimension().identifier() + "/"
                    + passenger.level().dimension().identifier() + "/"
                    + (planetVehicle == null ? "missing_vehicle"
                            : planetVehicle.level().dimension().identifier())
                    + ", mounts=" + (planetVehicle != null) + "/"
                    + (passenger.getVehicle() == planetVehicle)
                    + ", respawns=" + respawnMatches(owner, Level.NETHER, planetArrival)
                    + "/" + respawnMatches(passenger, Level.NETHER, planetArrival)
                    + ", reason=" + outbound.reason());
            return;
        }
        var outboundGroup = kingdoms.armyGroup(group.id()).orElseThrow();
        if (outboundGroup.simulation().lifecycleState() != ArmyGroupLifecycleState.VIRTUAL
                || !outboundGroup.simulation().anchor().dimensionId()
                        .equals(Level.NETHER.identifier().toString())
                || outboundGroup.snapshots().size() != 2
                || !commander.isRemoved() || !member.isRemoved()
                || !progression.state(owner.getUUID()).hasSubject(
                        ProgressionEventType.PLANET_VISITED, "tatooine")
                || !progression.state(passenger.getUUID()).hasSubject(
                        ProgressionEventType.PLANET_VISITED, "tatooine")) {
            helper.fail("Outbound transfer did not virtualize its squad or record its planet visit");
            return;
        }

        CommandCenterNavigationMenu navigation = new CommandCenterNavigationMenu(
                0, owner.getInventory());
        int homeButton = navigation.destinationIds().indexOf(PlanetTravelService.HOME_DESTINATION_ID);
        if (homeButton < 0 || !navigation.clickMenuButton(owner, homeButton)) {
            helper.fail("The real navigation menu did not execute the authoritative home route");
            return;
        }

        var returnedGroup = kingdoms.armyGroup(group.id()).orElseThrow();
        BlockPos ownerRespawn = respawnPosition(owner);
        GalacticVehicleEntity homeVehicle = owner.getVehicle() instanceof GalacticVehicleEntity traveled
                ? traveled : null;
        if (owner.level() != overworld || passenger.level() != overworld
                || homeVehicle == null || homeVehicle.level() != overworld
                || passenger.getVehicle() != homeVehicle
                || !homeVehicle.getUUID().equals(vehicleId)
                || homeVehicle.fuel() != initialFuel || homeVehicle.health() != initialHealth
                || homeVehicle.syncedFuelCapacity() != initialFuelCapacity
                || homeVehicle.syncedMaximumHealth() != initialMaximumHealth
                || countItem(owner, ModItems.CREDIT_CHIP.get()) != 17
                || countItem(passenger, Items.DIAMOND) != 3
                || !respawnMatches(owner, Level.OVERWORLD, ownerRespawn)
                || !respawnMatches(passenger, Level.OVERWORLD, ownerRespawn)
                || ownerRespawn.distSqr(hallPos) > 24L * 24L) {
            helper.fail("Home transfer did not preserve passengers, inventory, vehicle, or respawn state");
            return;
        }
        if (returnedGroup.simulation().lifecycleState() != ArmyGroupLifecycleState.VIRTUAL
                || !returnedGroup.simulation().anchor().dimensionId()
                        .equals(Level.OVERWORLD.identifier().toString())
                || returnedGroup.snapshots().size() != 2
                || overworld.getEntity(commander.getUUID()) != null
                || overworld.getEntity(member.getUUID()) != null
                || progression.state(owner.getUUID()).hasSubject(
                        ProgressionEventType.PLANET_VISITED, PlanetTravelService.HOME_DESTINATION_ID)
                || !progression.state(owner.getUUID()).hasSubject(
                        ProgressionEventType.PLANET_VISITED, "tatooine")) {
            helper.fail("Home transfer duplicated its squad or corrupted progression semantics");
            return;
        }
        helper.succeed();
        } finally {
            overworld.setChunkForced(sourceChunk.x(), sourceChunk.z(), false);
            planet.setChunkForced(destinationChunk.x(), destinationChunk.z(), false);
        }
        });
    }

    private static void prepareLandingArea(ServerLevel level, BlockPos center) {
        level.getChunkAt(center);
        for (int x = -LANDING_RADIUS; x <= LANDING_RADIUS; x++) {
            for (int z = -LANDING_RADIUS; z <= LANDING_RADIUS; z++) {
                level.setBlock(center.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
                for (int y = 0; y <= 4; y++) {
                    level.setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    private static CommandCenterBlockEntity placeCommandCenter(ServerLevel level, BlockPos position) {
        if (!level.setBlock(position, ModBlocks.COMMAND_CENTER.get().defaultBlockState(), 3)
                || !(level.getBlockEntity(position) instanceof CommandCenterBlockEntity hall)) {
            throw new IllegalStateException("Could not place round-trip Command Center at " + position);
        }
        return hall;
    }

    @SuppressWarnings("removal")
    private static ServerPlayer makePlayer(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        GameType.CREATIVE.updatePlayerAbilities(player.getAbilities());
        return player;
    }

    private static GalacticRecruitEntity spawnRecruit(
            ServerLevel level,
            net.minecraft.world.entity.EntityType<GalacticRecruitEntity> type,
            BlockPos position
    ) {
        GalacticRecruitEntity recruit = type.create(level, EntitySpawnReason.TRIGGERED);
        if (recruit == null) {
            throw new IllegalStateException("Could not create round-trip recruit");
        }
        recruit.setPos(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D);
        recruit.initializeFromSpawnEgg();
        if (!level.addFreshEntity(recruit)) {
            throw new IllegalStateException("Could not add round-trip recruit");
        }
        recruit.tick();
        return recruit;
    }

    private static boolean teleportPlayer(
            ServerPlayer player,
            ServerLevel level,
            double x,
            double y,
            double z
    ) {
        if (!player.teleportTo(level, x, y, z, Set.of(), 0.0F, 0.0F, true)) {
            return false;
        }
        // A real client acknowledges the teleport and moves its chunk-tracking ticket. The
        // GameTest EmbeddedChannel has no movement packets, so mirror that authoritative update.
        level.getChunkSource().move(player);
        return true;
    }

    private static void applyProgression(
            ProgressionSavedData progression,
            ServerPlayer player,
            ProgressionEventType type,
            String subject
    ) {
        var decision = progression.apply(new ProgressionEvent(
                UUID.randomUUID(), player.getUUID(), type, subject, 1));
        if (!decision.accepted()) {
            throw new IllegalStateException(
                    "Round-trip progression setup rejected " + type + "/" + subject
                            + ": " + decision.reason());
        }
    }

    private static BuildProject fullyProgressProject(
            KingdomSavedData data,
            UUID ownerId,
            KingdomBaseBlueprint blueprint,
            String dimensionId,
            BlockPos origin
    ) {
        BuildProject project = data.startBuildProject(ownerId, blueprint, dimensionId, origin, 0)
                .orElseThrow();
        for (int placement = 0; placement < blueprint.placements().size(); placement++) {
            project = project.markCompleted(placement);
            if (!data.replaceBuildProject(ownerId, project)) {
                throw new IllegalStateException(
                        "Could not persist round-trip blueprint placement " + placement);
            }
        }
        return project;
    }

    private static int countItem(ServerPlayer player, Item item) {
        return player.getInventory().getNonEquipmentItems().stream()
                .filter(stack -> stack.is(item))
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private static boolean respawnMatches(
            ServerPlayer player,
            net.minecraft.resources.ResourceKey<Level> dimension,
            BlockPos position
    ) {
        ServerPlayer.RespawnConfig config = player.getRespawnConfig();
        return config != null
                && config.respawnData().dimension().equals(dimension)
                && config.respawnData().pos().equals(position)
                && config.forced();
    }

    private static BlockPos respawnPosition(ServerPlayer player) {
        ServerPlayer.RespawnConfig config = player.getRespawnConfig();
        if (config == null) {
            throw new IllegalStateException("Round-trip traveler has no respawn configuration");
        }
        return config.respawnData().pos();
    }
}
