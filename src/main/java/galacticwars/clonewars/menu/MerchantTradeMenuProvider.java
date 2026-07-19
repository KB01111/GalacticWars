package galacticwars.clonewars.menu;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import galacticwars.clonewars.economy.PhysicalTradeService;
import galacticwars.clonewars.entity.GalacticRecruitEntity;
import galacticwars.clonewars.progression.LaunchContentCatalog;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class MerchantTradeMenuProvider implements ExtendedMenuProvider {
    private final GalacticRecruitEntity merchant;
    private List<MerchantTradeOffer> preparedOffers = List.of();

    public MerchantTradeMenuProvider(GalacticRecruitEntity merchant) {
        this.merchant = merchant;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.galacticwars.merchant_trade");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        preparedOffers = offers(player);
        return new MerchantTradeMenu(containerId, inventory, merchant.getId(), preparedOffers);
    }

    @Override
    public void saveExtraData(FriendlyByteBuf buffer) {
        buffer.writeVarInt(merchant.getId());
        MerchantTradeOffer.writeOffers(buffer, preparedOffers);
    }

    private List<MerchantTradeOffer> offers(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return List.of();
        }
        String faction = merchant.factionIdForGameplay();
        String path = faction.contains(":") ? faction.substring(faction.indexOf(':') + 1) : faction;
        return LaunchContentCatalog.trades().values().stream()
                .filter(trade -> trade.factionId().equals(path))
                .sorted(java.util.Comparator.comparing(trade -> trade.id()))
                .limit(MerchantTradeMenu.MAX_OFFERS)
                .map(trade -> PhysicalTradeService.preview(serverPlayer, trade.id(), merchant))
                .map(MerchantTradeOffer::fromPreview)
                .toList();
    }
}
