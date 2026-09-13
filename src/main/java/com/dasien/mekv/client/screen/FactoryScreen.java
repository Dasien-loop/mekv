package com.dasien.mekv.client.screen;

import com.dasien.mekv.blockentity.VillagerFactoryBlockEntity;
import com.dasien.mekv.factory.RedstoneMode;
import com.dasien.mekv.factory.VillagerFactoryType;
import com.dasien.mekv.inventory.FilteredSlot;
import com.dasien.mekv.inventory.LockedSlot;
import com.dasien.mekv.inventory.OutputSlot;
import com.dasien.mekv.menu.FactoryMenu;
import mekanism.client.SpecialColors;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.element.GuiInsetElement;
import mekanism.client.gui.element.bar.GuiBar;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.tab.window.GuiWindowCreatorTab;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.api.security.SecurityMode;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.util.text.BooleanStateDisplay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FactoryScreen<T extends FactoryMenu> extends GuiMekanism<T> {
    private ConfigTab configTab;
    private TransporterTab transporterTab;
    private UpgradeTab upgradeTab;

    public FactoryScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = menu.guiWidth();
        this.imageHeight = menu.playerInventoryY() + 82;
        this.inventoryLabelX = menu.playerInventoryX();
        this.inventoryLabelY = menu.playerInventoryY() - 11;
        this.titleLabelY = 5;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiVerticalPowerBar(this, new GuiBar.IBarInfoHandler() {
            @Override
            public double getLevel() {
                int max = menu.getMaxEnergy();
                return max <= 0 ? 0 : (double) menu.getEnergy() / max;
            }

            @Override
            public Component getTooltip() {
                return Component.translatable("gui.mekv.energy", menu.getEnergy(), menu.getMaxEnergy());
            }
        }, menu.energyBarX(), 16));

        for (Slot slot : menu.slots) {
            if (menu.getUpgradeSlots().contains(slot)) {
                continue;
            }
            GuiSlot guiSlot = new GuiSlot(slotType(slot), this, slot.x - 1, slot.y - 1);
            configureGuiSlot(guiSlot, slot);
            // ContainerData is populated after the screen is constructed. Use
            // a supplier so dynamic overlays (for example a sold-out trade)
            // become visible as soon as the server state arrives.
            guiSlot.with(() -> overlayFor(slot));
            addRenderableWidget(guiSlot);
        }

        configTab = addRenderableWidget(new ConfigTab(() -> configTab));
        transporterTab = addRenderableWidget(new TransporterTab(() -> transporterTab));
        if (menu.getFactory().getFactoryType() == VillagerFactoryType.TRADER) {
            addRenderableWidget(new SortingTab());
        }
        upgradeTab = addRenderableWidget(new UpgradeTab(() -> upgradeTab));
        addRenderableWidget(new SecurityTab());
        addRenderableWidget(new RedstoneTab());
    }

    /**
     * Gives specialized factory screens a chance to add behavior to their
     * visual slots while retaining the common Mekanism slot rendering.
     */
    protected void configureGuiSlot(GuiSlot guiSlot, Slot slot) {
    }

    protected SlotType slotType(Slot slot) {
        if (slot == menu.getEnergySlot()) {
            return SlotType.POWER;
        }
        if (menu.getUpgradeSlots().contains(slot)) {
            return SlotType.NORMAL;
        }
        if (slot instanceof OutputSlot) {
            return SlotType.OUTPUT;
        }
        if (slot instanceof LockedSlot) {
            return SlotType.NORMAL;
        }
        if (slot instanceof FilteredSlot) {
            return SlotType.EXTRA;
        }
        if (slot.y >= menu.playerInventoryY()) {
            return SlotType.NORMAL;
        }
        return SlotType.INPUT;
    }

    protected SlotOverlay overlayFor(Slot slot) {
        if (slot == menu.getEnergySlot()) {
            return SlotOverlay.POWER;
        }
        if (menu.getUpgradeSlots().contains(slot)) {
            return SlotOverlay.UPGRADE;
        }
        if (slot instanceof LockedSlot) {
            return null;
        }
        if (slot instanceof FilteredSlot) {
            return slot.hasItem() ? null : SlotOverlay.PLUS;
        }
        return null;
    }

    protected void click(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            com.dasien.mekv.network.FactoryNetwork.clickButton(menu.containerId, buttonId);
        }
    }

    void clickSlot(Slot slot, int mouseButton) {
        if (minecraft != null && minecraft.gameMode != null && minecraft.player != null) {
            minecraft.gameMode.handleInventoryMouseClick(menu.containerId, slot.index, mouseButton,
                    ClickType.PICKUP, minecraft.player);
        }
    }

    protected List<Component> machineInfo() {
        List<Component> lines = new ArrayList<>();
        lines.add(menu.getStatusText());
        VillagerFactoryBlockEntity factory = menu.getFactory();
        if (factory.hasVillager()) {
            lines.add(factory.getVillagerDisplayName());
            if (factory.isVillagerBaby()) {
                lines.add(Component.translatable("gui.mekv.status.growing"));
            } else {
                lines.add(Component.translatable("gui.mekv.profession_level",
                        factory.getVillagerProfessionName(), factory.getVillagerLevel()));
            }
        } else {
            lines.add(Component.translatable("gui.mekv.status.no_villager"));
        }
        lines.add(Component.translatable("gui.mekv.parallel", factory.getParallel()));
        if (factory.getFactoryType() == VillagerFactoryType.TRADER) {
            lines.add(Component.translatable("gui.mekv.trade_duration", menu.getTradeDuration()));
        }
        lines.add(Component.translatable("gui.mekv.usage", menu.getEnergyUsage()));
        return lines;
    }

    private static ResourceLocation redstoneTexture(RedstoneMode mode) {
        return switch (mode) {
            case HIGH -> MekGui.REDSTONE_HIGH;
            case LOW -> MekGui.REDSTONE_LOW;
            case IGNORED -> MekGui.REDSTONE_DISABLED;
        };
    }

    private class ConfigTab extends CompatGuiWindowCreatorTab<FactoryMenu, ConfigTab> {
        ConfigTab(Supplier<ConfigTab> self) {
            super(MekGui.CONFIG, FactoryScreen.this, menu, -26, 6, 26, 18, true, self);
        }

        @Override
        protected void colorTab(GuiGraphics graphics) {
            MekanismRenderer.color(graphics, SpecialColors.TAB_CONFIGURATION);
        }

        @Override
        protected GuiWindow createWindow(SelectedWindowData data) {
            return new FactoryConfigWindow(FactoryScreen.this, getGuiWidth() / 2 - 78, 15, menu, FactoryScreen.this::click);
        }
        @Override
        protected SelectedWindowData getNextWindowData() {
            return new SelectedWindowData(SelectedWindowData.WindowType.SIDE_CONFIG);
        }

        @Override
        public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
            super.renderToolTip(graphics, mouseX, mouseY);
            displayTooltips(graphics, mouseX, mouseY, MekanismLang.SIDE_CONFIG.translate());
        }
    }

    private class TransporterTab extends CompatGuiWindowCreatorTab<FactoryMenu, TransporterTab> {
        TransporterTab(Supplier<TransporterTab> self) {
            super(MekGui.TRANSPORTER, FactoryScreen.this, menu, -26, 34, 26, 18, true, self);
        }

        @Override
        protected void colorTab(GuiGraphics graphics) {
            MekanismRenderer.color(graphics, SpecialColors.TAB_TRANSPORTER);
        }

        @Override
        protected GuiWindow createWindow(SelectedWindowData data) {
            return new FactoryTransporterWindow(FactoryScreen.this, getGuiWidth() / 2 - 78, 15, menu, FactoryScreen.this::click);
        }
        @Override
        protected SelectedWindowData getNextWindowData() {
            return new SelectedWindowData(SelectedWindowData.WindowType.TRANSPORTER_CONFIG);
        }

        @Override
        public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
            super.renderToolTip(graphics, mouseX, mouseY);
            displayTooltips(graphics, mouseX, mouseY, MekanismLang.TRANSPORTER_CONFIG.translate());
        }
    }

    private class UpgradeTab extends CompatGuiWindowCreatorTab<FactoryMenu, UpgradeTab> {
        UpgradeTab(Supplier<UpgradeTab> self) {
            super(MekGui.UPGRADE, FactoryScreen.this, menu, FactoryScreen.this.getXSize(), 6, 26, 18, false, self);
        }

        @Override
        protected void colorTab(GuiGraphics graphics) {
            MekanismRenderer.color(graphics, SpecialColors.TAB_UPGRADE);
        }

        @Override
        protected GuiWindow createWindow(SelectedWindowData data) {
            menu.setUpgradeWindowOpen(true);
            click(FactoryMenu.BUTTON_UPGRADE_WINDOW_OPEN);
            return new FactoryUpgradeWindow(FactoryScreen.this, getGuiWidth() / 2 - 78, 15, menu, FactoryScreen.this::click);
        }
        @Override
        protected SelectedWindowData getNextWindowData() {
            return new SelectedWindowData(SelectedWindowData.WindowType.UPGRADE);
        }

        @Override
        public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
            super.renderToolTip(graphics, mouseX, mouseY);
            displayTooltips(graphics, mouseX, mouseY, MekanismLang.UPGRADES.translate());
        }
    }

    /** Matches Mekanism's factory sorting tab placement, dimensions and state label. */
    private class SortingTab extends CompatGuiInsetElement<FactoryMenu> {
        SortingTab() {
            super(MekGui.SORTING, FactoryScreen.this, menu, -26, 62, 35, 18, true);
        }

        @Override
        public void drawBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            super.drawBackground(graphics, mouseX, mouseY, partialTicks);
            drawTextScaledBound(graphics, BooleanStateDisplay.OnOff.of(menu.isAutoSort()).getTextComponent(),
                    relativeX + 3, relativeY + 24, titleTextColor(), 21);
        }

        @Override
        protected void colorTab(GuiGraphics graphics) {
            MekanismRenderer.color(graphics, SpecialColors.TAB_FACTORY_SORT);
        }

        @Override
        public void onClick(double mouseX, double mouseY, int button) {
            click(FactoryMenu.BUTTON_AUTO_SORT);
        }

        @Override
        public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
            super.renderToolTip(graphics, mouseX, mouseY);
            displayTooltips(graphics, mouseX, mouseY, Component.translatable("gui.mekv.auto_sort"));
        }
    }

    private class RedstoneTab extends CompatGuiInsetElement<FactoryMenu> {
        RedstoneTab() {
            super(MekGui.REDSTONE_DISABLED, FactoryScreen.this, menu, FactoryScreen.this.getXSize(), FactoryScreen.this.getYSize() - 29, 26, 18, false);
        }

        @Override
        protected ResourceLocation getOverlay() {
            return redstoneTexture(menu.getRedstoneMode());
        }

        @Override
        protected void colorTab(GuiGraphics graphics) {
            MekanismRenderer.color(graphics, SpecialColors.TAB_REDSTONE_CONTROL);
        }

        @Override
        public void onClick(double mouseX, double mouseY, int button) {
            playClickSound(SoundEvents.UI_BUTTON_CLICK::value);
            click(button == 1 ? FactoryMenu.BUTTON_REDSTONE_PREV : FactoryMenu.BUTTON_REDSTONE);
        }

        @Override
        public boolean isValidClickButton(int button) {
            return button == 0 || button == 1;
        }

        @Override
        public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
            super.renderToolTip(graphics, mouseX, mouseY);
            displayTooltips(graphics, mouseX, mouseY, menu.getRedstoneMode().displayName());
        }
    }

    private class SecurityTab extends CompatGuiInsetElement<FactoryMenu> {
        SecurityTab() {
            super(MekGui.SECURITY_PUBLIC, FactoryScreen.this, menu, FactoryScreen.this.getXSize(), 34, 26, 18, false);
        }

        @Override
        protected ResourceLocation getOverlay() {
            return switch (menu.getSecurityMode()) {
                case PUBLIC -> MekGui.SECURITY_PUBLIC;
                case PRIVATE -> MekGui.SECURITY_PRIVATE;
                case TRUSTED -> MekGui.SECURITY_TRUSTED;
            };
        }

        @Override
        protected void colorTab(GuiGraphics graphics) {
            MekanismRenderer.color(graphics, SpecialColors.TAB_SECURITY);
        }

        @Override
        public void onClick(double mouseX, double mouseY, int button) {
            playClickSound(SoundEvents.UI_BUTTON_CLICK::value);
            click(button == 1 ? FactoryMenu.BUTTON_SECURITY_PREV : FactoryMenu.BUTTON_SECURITY_NEXT);
        }

        @Override
        public boolean isValidClickButton(int button) {
            return button == 0 || button == 1;
        }

        @Override
        public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
            super.renderToolTip(graphics, mouseX, mouseY);
            displayTooltips(graphics, mouseX, mouseY, menu.getSecurityMode().getTextComponent());
        }
    }
}















