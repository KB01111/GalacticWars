package galacticwars.clonewars.menu;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.registry.ModMenuTypes;
import galacticwars.clonewars.world.PlanetTravelService;
import galacticwars.clonewars.world.PlanetTravelService.NavigationDestination;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public final class CommandCenterNavigationMenu extends AbstractContainerMenu {
    public static final int MAX_DESTINATION_IDS = 32;
    public static final int MAX_REASON_BYTES = 64;
    private final List<NavigationDestination> destinations;

    public CommandCenterNavigationMenu(int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(containerId, inventory, readDestinations(extraData));
    }

    public CommandCenterNavigationMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, inventory.player instanceof ServerPlayer serverPlayer
                ? PlanetTravelService.navigationOptions(serverPlayer)
                : List.of());
    }

    CommandCenterNavigationMenu(
            int containerId,
            Inventory inventory,
            List<NavigationDestination> destinations
    ) {
        super(ModMenuTypes.COMMAND_CENTER_NAVIGATION.get(), containerId);
        if (destinations.size() > MAX_DESTINATION_IDS) {
            throw new IllegalArgumentException("navigation destination count exceeds payload cap");
        }
        this.destinations = List.copyOf(destinations);
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !this.stillValid(player)
                || buttonId < 0
                || buttonId >= destinations.size()) {
            return false;
        }
        String destinationId = destinations.get(buttonId).destinationId();
        PlanetTravelService.TravelResult result = PlanetTravelService.travel(
                serverPlayer, destinationId);
        if (result.accepted()) {
            player.sendSystemMessage(Component.translatable(
                    result.squadTransferred()
                            ? "message.galacticwars.travel.success_with_squad"
                            : "message.galacticwars.travel.success",
                    destinationName(result.destinationId())));
            serverPlayer.closeContainer();
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.galacticwars.travel." + result.reason()));
        }
        return result.accepted();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.isAlive()
                && player instanceof ServerPlayer serverPlayer
                && PlanetTravelService.hasActiveCommandCenter(serverPlayer);
    }

    public List<String> destinationIds() {
        return destinations.stream().map(NavigationDestination::destinationId).toList();
    }

    public List<NavigationDestination> destinations() {
        return destinations;
    }

    public static Component destinationName(String destinationId) {
        return PlanetTravelService.HOME_DESTINATION_ID.equals(destinationId)
                ? Component.translatable("destination.galacticwars.home")
                : Component.translatable("planet.galacticwars." + destinationId);
    }

    private static List<NavigationDestination> readDestinations(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        if (size < 0 || size > MAX_DESTINATION_IDS) {
            throw new IllegalArgumentException("invalid navigation payload size " + size);
        }
        ArrayList<NavigationDestination> destinations = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            String id = buffer.readUtf(LaunchContentDefinitions.MAX_SERIALIZED_PLANET_ID_BYTES);
            boolean available = buffer.readBoolean();
            String reason = buffer.readUtf(MAX_REASON_BYTES);
            destinations.add(new NavigationDestination(id, available, reason));
        }
        return List.copyOf(destinations);
    }
}
