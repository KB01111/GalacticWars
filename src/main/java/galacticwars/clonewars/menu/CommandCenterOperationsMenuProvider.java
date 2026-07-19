package galacticwars.clonewars.menu;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import galacticwars.clonewars.kingdom.CommandCenterDashboardState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.Objects;

public final class CommandCenterOperationsMenuProvider implements ExtendedMenuProvider {
    private final BlockPos hallPos;
    private CommandCenterDashboardState preparedDashboard;

    public CommandCenterOperationsMenuProvider(BlockPos hallPos) {
        this.hallPos = hallPos.immutable();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.galacticwars.command_center.operations");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        CommandCenterOperationsMenu menu = new CommandCenterOperationsMenu(
                containerId, inventory, hallPos);
        preparedDashboard = menu.dashboardState();
        return menu;
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(hallPos);
        CommandCenterDashboardCodec.write(buffer, Objects.requireNonNull(
                preparedDashboard, "createMenu must run before extra menu data is encoded"));
    }
}
