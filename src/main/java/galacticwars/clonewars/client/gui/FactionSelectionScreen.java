package galacticwars.clonewars.client.gui;

import galacticwars.clonewars.data.CoreContentBindings;
import galacticwars.clonewars.menu.FactionSelectionMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class FactionSelectionScreen extends Screen implements MenuAccess<FactionSelectionMenu> {
    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 3;
    private final FactionSelectionMenu menu;
    private final Inventory inventory;
    private int selectedFaction = -1;
    private Component status = Component.translatable(
            "screen.galacticwars.faction_selection.choose_guidance");

    public FactionSelectionScreen(FactionSelectionMenu menu, Inventory inventory, Component title) {
        super(title);
        this.menu = menu;
        this.inventory = inventory;
    }

    @Override
    protected void init() {
        super.init();
        StringWidget heading = new StringWidget(
                0, 15, this.width, 16, this.title.copy().withColor(0xFFE082), this.font);
        this.addRenderableWidget(heading);
        StringWidget subtitle = new StringWidget(
                0, 32, this.width, 14,
                Component.translatable("screen.galacticwars.faction_selection.subtitle")
                        .withColor(0xD7E7F5), this.font);
        this.addRenderableWidget(subtitle);
        StringWidget warning = new StringWidget(
                0, this.height - 21, this.width, 14,
                Component.translatable("screen.galacticwars.faction_selection.warning")
                        .withColor(0xAAB7C4), this.font);
        this.addRenderableWidget(warning);
        int x = (this.width - BUTTON_WIDTH) / 2;
        int listHeight = this.menu.factionIds().size() * (BUTTON_HEIGHT + GAP) - GAP;
        int firstY = Math.max(58, Math.min(
                (this.height - listHeight) / 2 - 4,
                this.height - 70 - listHeight));
        for (int index = 0; index < this.menu.factionIds().size(); index++) {
            String factionId = this.menu.factionIds().get(index);
            int buttonId = index;
            this.addRenderableWidget(Button.builder(
                            this.factionButtonLabel(buttonId, factionId),
                            button -> this.previewFaction(buttonId))
                    .bounds(x, firstY + index * (BUTTON_HEIGHT + GAP), BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build());
        }
        int footerY = this.height - 45;
        Button confirm = Button.builder(this.confirmLabel(), button -> this.confirmFaction())
                .bounds(this.width / 2 + 3, footerY, 107, 20)
                .build();
        confirm.active = this.selectedFaction >= 0
                && this.chipCount(this.menu.factionIds().get(this.selectedFaction)) > 0;
        this.addRenderableWidget(confirm);
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.cancel"), button -> this.onClose())
                .bounds(this.width / 2 - 110, footerY, 107, 20)
                .build());
    }

    private void previewFaction(int buttonId) {
        this.selectedFaction = buttonId;
        String factionId = this.menu.factionIds().get(buttonId);
        Component description = Component.translatable(
                "screen.galacticwars.faction_selection.description." + path(factionId));
        this.status = description.copy().append(Component.literal(" — ")).append(
                this.chipCount(factionId) > 0
                        ? Component.translatable("screen.galacticwars.faction_selection.chip_ready")
                        : Component.translatable("screen.galacticwars.faction_selection.chip_missing"));
        this.rebuildWidgets();
    }

    private void confirmFaction() {
        if (this.selectedFaction < 0) {
            return;
        }
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(
                    this.menu.containerId, this.selectedFaction);
            this.status = Component.translatable(
                    "screen.galacticwars.faction_selection.submitted");
        }
    }

    private Component factionButtonLabel(int index, String factionId) {
        Component name = Component.translatable(FactionSelectionMenu.factionTranslation(factionId));
        Component label = Component.translatable(
                "screen.galacticwars.faction_selection.faction_with_chip",
                name, this.chipCount(factionId));
        return index == this.selectedFaction
                ? Component.literal("▶ ").append(label)
                : label;
    }

    private Component confirmLabel() {
        if (this.selectedFaction < 0) {
            return Component.translatable("screen.galacticwars.faction_selection.confirm");
        }
        String factionId = this.menu.factionIds().get(this.selectedFaction);
        return Component.translatable(
                "screen.galacticwars.faction_selection.confirm_named",
                Component.translatable(FactionSelectionMenu.factionTranslation(factionId)));
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        int availableWidth = Math.max(80, this.width - 24);
        Component display = this.font.width(this.status) <= availableWidth
                ? this.status
                : Component.literal(this.font.plainSubstrByWidth(
                        this.status.getString(), availableWidth - 8) + "...");
        graphics.text(this.font, display,
                (this.width - this.font.width(display)) / 2, 48, 0xFFE6C77A);
    }

    @Override
    public FactionSelectionMenu getMenu() {
        return menu;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.closeContainer();
        }
        super.onClose();
    }

    private static String path(String id) {
        int separator = id.indexOf(':');
        return separator < 0 ? id : id.substring(separator + 1);
    }

    private int chipCount(String factionId) {
        String itemId = CoreContentBindings.factionChips().get(factionId);
        if (itemId == null) {
            return 0;
        }
        Item item;
        try {
            item = BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId));
        } catch (RuntimeException invalidId) {
            return 0;
        }
        if (item == null) {
            return 0;
        }
        return this.inventory.getNonEquipmentItems().stream()
                .filter(stack -> stack.is(item))
                .mapToInt(ItemStack::getCount)
                .sum();
    }
}
