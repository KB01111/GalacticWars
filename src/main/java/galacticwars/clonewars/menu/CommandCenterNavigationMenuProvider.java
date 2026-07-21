package galacticwars.clonewars.menu;

import galacticwars.clonewars.data.LaunchContentDefinitions;
import galacticwars.clonewars.world.PlanetTravelService;
import galacticwars.clonewars.world.PlanetTravelService.NavigationDestination;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public final class CommandCenterNavigationMenuProvider implements ExtendedMenuProvider {
    private final List<NavigationDestination> destinations;

    public CommandCenterNavigationMenuProvider(ServerPlayer player) {
        this.destinations = PlanetTravelService.navigationOptions(player);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.galacticwars.navigation_console");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CommandCenterNavigationMenu(containerId, inventory, destinations);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buffer) {
        if (destinations.size() > CommandCenterNavigationMenu.MAX_DESTINATION_IDS) {
            throw new IllegalStateException(
                    "destination list exceeds navigation payload cap: " + destinations.size());
        }
        buffer.writeVarInt(destinations.size());
        destinations.forEach(destination -> {
            buffer.writeUtf(destination.destinationId(),
                    LaunchContentDefinitions.MAX_SERIALIZED_PLANET_ID_BYTES);
            buffer.writeBoolean(destination.available());
            buffer.writeUtf(destination.reason(), CommandCenterNavigationMenu.MAX_REASON_BYTES);
            buffer.writeUtf(destination.theme(), CommandCenterNavigationMenu.MAX_METADATA_BYTES);
            buffer.writeUtf(destination.arrivalProfile(), CommandCenterNavigationMenu.MAX_METADATA_BYTES);
        });
    }
}
