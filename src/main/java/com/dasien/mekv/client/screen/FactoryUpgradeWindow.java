package com.dasien.mekv.client.screen;

import com.dasien.mekv.blockentity.VillagerFactoryBlockEntity;
import com.dasien.mekv.factory.VillagerFactoryType;
import com.dasien.mekv.menu.FactoryMenu;
import com.dasien.mekv.registry.ModItems;
import mekanism.api.Upgrade;
import mekanism.api.text.TextComponentUtil;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiElementHolder;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.DigitalButton;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.custom.GuiSupportedUpgrades;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.scroll.GuiScrollList;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.render.IFancyFontRenderer;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.util.UpgradeUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

public class FactoryUpgradeWindow extends CompatGuiWindow {
    private final FactoryMenu menu;
    private final IntConsumer click;
    private final MekanismButton removeButton;
    private final UpgradeList scrollList;
    private final IFancyFontRenderer.WrappedTextRenderer noSelection;
    private Component lastTypeText;
    private IFancyFontRenderer.WrappedTextRenderer typeTextRenderer;

    public FactoryUpgradeWindow(IGuiWrapper gui, int x, int y, FactoryMenu menu, IntConsumer click) {
        super(gui, x, y, 156, Math.max(96, 76 + 12 * GuiSupportedUpgrades.calculateNeededRows(gui)), SelectedWindowData.WindowType.UPGRADE);
        this.menu = menu;
        this.click = click;
        this.noSelection = new IFancyFontRenderer.WrappedTextRenderer(this, MekanismLang.UPGRADE_NO_SELECTION.translate());
        interactionStrategy = InteractionStrategy.ALL;

        scrollList = addChild(new UpgradeList(gui, relativeX + 6, relativeY + 18));
        addChild(new GuiSupportedUpgrades(gui, relativeX + 6, relativeY + 68, supportedUpgrades()));
        addChild(new GuiInnerScreen(gui, relativeX + 72, relativeY + 18, 59, 50));
        addChild(new GuiProgress(menu::getUpgradeProgress, ProgressType.INSTALLING, gui, relativeX + 134, relativeY + 37));
        addChild(new GuiProgress(() -> 0, ProgressType.UNINSTALLING, gui, relativeX + 134, relativeY + 59));
        removeButton = addChild(new DigitalButton(gui, relativeX + 73, relativeY + 54, 56, 12,
                MekanismLang.UPGRADE_UNINSTALL, (element, mx, my) -> { uninstall(); return true; }));
        addChild(new GuiSlot(SlotType.NORMAL, gui, relativeX + 133, relativeY + 18).with(SlotOverlay.UPGRADE));
        addChild(new GuiSlot(SlotType.NORMAL, gui, relativeX + 133, relativeY + 73).with(SlotOverlay.UPGRADE));
        addChild(new UpgradeStackDisplay(gui, relativeX + 133, relativeY + 18, menu.getUpgradeSlots().get(0)));
        addChild(new UpgradeStackDisplay(gui, relativeX + 133, relativeY + 73, menu.getUpgradeSlots().get(1)));
        updateEnabledButtons();
    }

    private Set<Upgrade> supportedUpgrades() {
        if (menu.getFactory().getFactoryType() == VillagerFactoryType.TRADER) {
            return EnumSet.noneOf(Upgrade.class);
        }
        return EnumSet.of(Upgrade.SPEED, Upgrade.ENERGY, Upgrade.MUFFLING);
    }

    private void uninstall() {
        if (!scrollList.hasSelection()) {
            return;
        }
        boolean all = Screen.hasShiftDown();
        if (scrollList.infiniteSelected) {
            click.accept(all ? FactoryMenu.BUTTON_UNINSTALL_INFINITE_ALL : FactoryMenu.BUTTON_UNINSTALL_INFINITE);
        } else if (scrollList.selected != null) {
            click.accept((all ? FactoryMenu.BUTTON_UNINSTALL_ALL : FactoryMenu.BUTTON_UNINSTALL_ONE) + scrollList.selected.ordinal());
        }
    }

    private void updateEnabledButtons() {
        removeButton.active = scrollList.hasSelection();
    }

    @Override
    public void close() {
        menu.setUpgradeWindowOpen(false);
        click.accept(FactoryMenu.BUTTON_UPGRADE_WINDOW_CLOSE);
        super.close();
    }

    private static class UpgradeStackDisplay extends GuiElement {
        private final net.minecraft.world.inventory.Slot slot;

        UpgradeStackDisplay(IGuiWrapper gui, int x, int y, net.minecraft.world.inventory.Slot slot) {
            super(gui, x, y, 18, 18);
            this.slot = slot;
        }

        @Override
        public void renderForeground(GuiGraphics graphics, int mouseX, int mouseY) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                gui().renderItemWithOverlay(graphics, stack, getRelativeX() + 1, getRelativeY() + 1, 1.0F, null);
            }
        }

        @Override
        public void onClick(double mouseX, double mouseY, int button) {
            if (gui() instanceof FactoryScreen<?> screen) {
                screen.clickSlot(slot, button);
            }
        }

        @Override
        public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                gui().renderItemTooltipWithExtra(graphics, stack, mouseX, mouseY, java.util.List.of());
            }
        }
    }

    @Override
    public void renderForeground(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderForeground(graphics, mouseX, mouseY);
        drawTitleText(graphics, MekanismLang.UPGRADES.translate(), 5);
        updateEnabledButtons();
        if (scrollList.hasSelection()) {
            VillagerFactoryBlockEntity factory = menu.getFactory();
            int y = relativeY + 20;
            Component typeName;
            int installed;
            int max;
            List<Component> extra = new ArrayList<>();
            if (scrollList.infiniteSelected) {
                typeName = new ItemStack(ModItems.INFINITE_TRADE_UPGRADE.get()).getHoverName();
                installed = factory.hasInfiniteTradeUpgrade() ? 1 : 0;
                max = 1;
                extra.add(Component.translatable(factory.hasInfiniteTradeUpgrade()
                        ? "gui.mekv.upgrade.infinite_trade.on"
                        : "gui.mekv.upgrade.infinite_trade.off"));
            } else {
                Upgrade upgrade = scrollList.selected;
                typeName = TextComponentUtil.build(upgrade);
                installed = factory.countUpgrade(upgrade);
                max = upgrade.getMax();
                if (upgrade == Upgrade.SPEED) {
                    extra.add(Component.translatable("gui.mekv.upgrade.speed", installed,
                            String.format("%.2f", factory.speedMultiplier())));
                } else if (upgrade == Upgrade.ENERGY) {
                    extra.add(Component.translatable("gui.mekv.upgrade.energy", installed,
                            String.format("%.2f", factory.energyUsageMultiplier())));
                }
            }
            Component typeText = MekanismLang.UPGRADE_TYPE.translate(typeName);
            if (!typeText.equals(lastTypeText)) {
                lastTypeText = typeText;
                typeTextRenderer = new IFancyFontRenderer.WrappedTextRenderer(this, typeText);
            }
            // Match GuiUpgradeWindow: 0.6-scale wrapped type, then six-pixel rows.
            int typeLines = typeTextRenderer.renderWithScale(graphics, relativeX + 74, y,
                    IFancyFontRenderer.TextAlignment.LEFT, screenTextColor(), 55, 0.6f);
            y = relativeY + 22 + 6 * typeLines;
            drawScaledTextScaledBound(graphics, MekanismLang.UPGRADE_COUNT.translate(installed, max),
                    relativeX + 74, y, screenTextColor(), 54, 0.6f);
            y += 6;
            for (Component line : extra) {
                drawScaledScrollingString(graphics, line, 74, y - relativeY,
                        IFancyFontRenderer.TextAlignment.LEFT, screenTextColor(), 54, 0, false, 0.6f);
                y += 6;
            }
        } else {
            noSelection.renderWithScale(graphics, relativeX + 74, relativeY + 20,
                    IFancyFontRenderer.TextAlignment.LEFT, screenTextColor(), 56, 0.8f);
        }
    }

    private class UpgradeList extends GuiScrollList {
        private static final int ROW = 12;
        private Upgrade selected;
        private boolean infiniteSelected;

        UpgradeList(IGuiWrapper gui, int x, int y) {
            super(gui, x, y, 66, 50, ROW, GuiElementHolder.HOLDER, 32);
        }

        @Override
        public boolean hasSelection() {
            return infiniteSelected || selected != null;
        }

        @Override
        protected int getMaxElements() {
            return entries().size();
        }

        @Override
        protected void setSelected(int index) {
            List<Entry> entries = entries();
            if (index >= 0 && index < entries.size()) {
                Entry entry = entries.get(index);
                infiniteSelected = entry.infinite;
                selected = entry.upgrade;
                updateEnabledButtons();
            }
        }

        @Override
        public void clearSelection() {
            if (hasSelection()) {
                infiniteSelected = false;
                selected = null;
                updateEnabledButtons();
            }
        }

        private List<Entry> entries() {
            List<Entry> entries = new ArrayList<>();
            VillagerFactoryBlockEntity factory = menu.getFactory();
            if (factory.hasInfiniteTradeUpgrade()) {
                entries.add(new Entry(null, true, new ItemStack(ModItems.INFINITE_TRADE_UPGRADE.get()),
                        new ItemStack(ModItems.INFINITE_TRADE_UPGRADE.get()).getHoverName()));
            }
            for (Upgrade upgrade : new Upgrade[]{Upgrade.SPEED, Upgrade.ENERGY, Upgrade.MUFFLING}) {
                if (factory.countUpgrade(upgrade) > 0) {
                    entries.add(new Entry(upgrade, false, UpgradeUtils.getStack(upgrade), TextComponentUtil.build(upgrade)));
                }
            }
            return entries;
        }

        @Override
        public void renderForeground(GuiGraphics graphics, int mouseX, int mouseY) {
            super.renderForeground(graphics, mouseX, mouseY);
            List<Entry> entries = entries();
            int start = getCurrentSelection();
            int focused = getFocusedElements();
            for (int i = 0; i < focused && start + i < entries.size(); i++) {
                drawScaledScrollingString(graphics, entries.get(start + i).name,
                        13, 3 + i * ROW, IFancyFontRenderer.TextAlignment.LEFT, titleTextColor(), 44, 0, false, 0.7f);
            }
        }

        @Override
        protected void renderElements(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            List<Entry> entries = entries();
            if (infiniteSelected && entries.stream().noneMatch(entry -> entry.infinite)) {
                infiniteSelected = false;
            }
            if (selected != null && entries.stream().noneMatch(entry -> entry.upgrade == selected)) {
                selected = null;
            }
            int start = getCurrentSelection();
            int focused = getFocusedElements();
            for (int i = 0; i < focused && start + i < entries.size(); i++) {
                Entry entry = entries.get(start + i);
                boolean selectedRow = entry.infinite ? infiniteSelected : entry.upgrade == selected;
                int rowTop = getY() + 1 + i * ROW;
                boolean hovered = mouseX >= getX() + 1 && mouseX < getX() + width - 1 - barXShift
                        && mouseY >= rowTop && mouseY < rowTop + ROW;
                int textureY = selectedRow ? 24 : hovered ? 0 : 12;
                if (entry.upgrade != null) {
                    MekanismRenderer.color(graphics, entry.upgrade.getColor());
                }
                graphics.blit(MekGui.UPGRADE_SELECTION, relativeX + 1, relativeY + 1 + i * ROW, 0, textureY, 58, ROW, 58, 36);
                MekanismRenderer.resetColor(graphics);
                gui().renderItem(graphics, entry.icon, relativeX + 3, relativeY + 3 + i * ROW, 0.5f);
            }
        }

        @Override
        public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
            super.renderToolTip(graphics, mouseX, mouseY);
            if (mouseX < getX() + 1 || mouseX >= getX() + width - 1 - barXShift) {
                return;
            }
            List<Entry> entries = entries();
            int index = getCurrentSelection() + (mouseY - getY() - 1) / ROW;
            if (index >= 0 && index < entries.size()) {
                Entry entry = entries.get(index);
                if (entry.upgrade != null) {
                    displayTooltips(graphics, mouseX, mouseY, entry.upgrade.getDescription());
                } else {
                    displayTooltips(graphics, mouseX, mouseY, Component.translatable("tooltip.mekv.upgrade_infinite_trade"));
                }
            }
        }

        private record Entry(Upgrade upgrade, boolean infinite, ItemStack icon, Component name) {
        }
    }
}














