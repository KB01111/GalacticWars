package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.menu.MerchantTradeMenu;
import galacticwars.clonewars.menu.MerchantTradeOffer;
import galacticwars.clonewars.network.GalacticNetwork;
import galacticwars.clonewars.network.MenuActionPayload;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

/** Responsive merchant screen rendered exclusively from a bounded server offer snapshot. */
public final class MerchantTradeScreen extends Screen implements MenuAccess<MerchantTradeMenu> {
    private static final int MAX_VISIBLE_ROWS = 8;
    private static final int ROW_HEIGHT = 22;
    private static final int PANEL_COLOR = 0xE018202B;
    private static final int PANEL_BORDER = 0xFF53657A;
    private static final int MUTED_COLOR = 0xFFAAB7C4;
    private static final int ACCENT_COLOR = 0xFFFFD36A;

    private final MerchantTradeMenu menu;
    private int offerPage;
    private int pageSize = 1;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelBottom;

    public MerchantTradeScreen(MerchantTradeMenu menu, Inventory inventory, Component title) {
        super(title);
        this.menu = menu;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(340, Math.max(220, width - 32));
        pageSize = Math.max(1, Math.min(MAX_VISIBLE_ROWS, (height - 92) / ROW_HEIGHT));
        int pageCount = pageCount();
        offerPage = Math.max(0, Math.min(offerPage, pageCount - 1));
        int visibleOffers = Math.min(pageSize, Math.max(0, menu.offers().size() - offerPage * pageSize));
        int pagerHeight = pageCount > 1 ? 24 : 0;
        int panelHeight = 50 + Math.max(1, visibleOffers) * ROW_HEIGHT + pagerHeight;
        panelLeft = (width - panelWidth) / 2;
        panelTop = Math.max(10, (height - panelHeight) / 2);
        panelBottom = panelTop + panelHeight;

        int firstOffer = offerPage * pageSize;
        int lastOffer = Math.min(menu.offers().size(), firstOffer + pageSize);
        for (int index = firstOffer; index < lastOffer; index++) {
            MerchantTradeOffer offer = menu.offers().get(index);
            Component label = Component.translatable(
                    "screen.galacticwars.trade.offer",
                    tradeItemName(offer.itemId()),
                    offer.itemCount(),
                    offer.creditPrice());
            int buttonId = index;
            Button button = Button.builder(label, pressed -> GalacticNetwork.CHANNEL.sendToServer(
                            new MenuActionPayload(UUID.randomUUID(), menu.containerId, buttonId)))
                    .bounds(panelLeft + 8, panelTop + 25 + (index - firstOffer) * ROW_HEIGHT,
                            panelWidth - 16, 20)
                    .build();
            button.active = offer.eligible();
            if (!offer.eligible()) {
                button.setTooltip(Tooltip.create(Component.translatable(offer.reasonTranslationKey())));
            }
            addRenderableWidget(button);
        }

        if (pageCount > 1) {
            int pagerY = panelBottom - 45;
            int pagerWidth = Math.min(138, (panelWidth - 22) / 2);
            Button previous = Button.builder(
                            Component.translatable("screen.galacticwars.trade.previous", offerPage + 1, pageCount),
                            pressed -> {
                                offerPage--;
                                rebuildWidgets();
                            })
                    .bounds(panelLeft + 8, pagerY, pagerWidth, 20)
                    .build();
            previous.active = offerPage > 0;
            addRenderableWidget(previous);

            Button next = Button.builder(
                            Component.translatable("screen.galacticwars.trade.next", offerPage + 1, pageCount),
                            pressed -> {
                                offerPage++;
                                rebuildWidgets();
                            })
                    .bounds(panelLeft + panelWidth - pagerWidth - 8, pagerY, pagerWidth, 20)
                    .build();
            next.active = offerPage + 1 < pageCount;
            addRenderableWidget(next);
        }
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        graphics.fill(0, 0, width, height, 0xB0080C12);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelBottom, PANEL_COLOR);
        graphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 1, PANEL_BORDER);
        graphics.fill(panelLeft, panelBottom - 1, panelLeft + panelWidth, panelBottom, PANEL_BORDER);
        graphics.fill(panelLeft, panelTop, panelLeft + 1, panelBottom, PANEL_BORDER);
        graphics.fill(panelLeft + panelWidth - 1, panelTop,
                panelLeft + panelWidth, panelBottom, PANEL_BORDER);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        drawCentered(graphics, title, panelTop + 8, ACCENT_COLOR);
        if (menu.offers().isEmpty()) {
            drawCentered(graphics, Component.translatable("screen.galacticwars.trade.empty"),
                    panelTop + 31, MUTED_COLOR);
        }
        drawCentered(graphics, Component.translatable("screen.galacticwars.trade.server_verified"),
                panelBottom - 16, MUTED_COLOR);
    }

    private int pageCount() {
        return Math.max(1, (menu.offers().size() + pageSize - 1) / pageSize);
    }

    private void drawCentered(GuiGraphicsExtractor graphics, Component text, int y, int color) {
        graphics.text(font, text, (width - font.width(text)) / 2, y, color);
    }

    private static Component tradeItemName(String itemId) {
        try {
            Identifier id = Identifier.parse(itemId);
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item != null && id.equals(BuiltInRegistries.ITEM.getKey(item))) {
                return Component.translatable(item.getDescriptionId());
            }
        } catch (RuntimeException ignored) {
            // The bounded server-authored id is still safe to display if the client lacks the item.
        }
        return Component.literal(itemId);
    }

    @Override
    public MerchantTradeMenu getMenu() {
        return menu;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
