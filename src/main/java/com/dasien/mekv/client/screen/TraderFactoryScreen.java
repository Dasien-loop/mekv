package com.dasien.mekv.client.screen;

import com.dasien.mekv.inventory.LockedSlot;
import com.dasien.mekv.menu.FactoryMenu;
import com.dasien.mekv.menu.TraderFactoryMenu;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.ToggleButton;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.slot.GuiSlot;
import mekanism.common.inventory.container.slot.SlotOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.ArrayList;
import java.util.List;

/** Mekanism-style trading factory screen with one independent lane per process. */
public class TraderFactoryScreen extends FactoryScreen<TraderFactoryMenu> {
    private static final int PROCESS_PAUSE_SIZE = 18;

    private ToggleButton globalPause;
    private final List<ToggleButton> processPauseButtons = new ArrayList<>();

    public TraderFactoryScreen(TraderFactoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        processPauseButtons.clear();

        CenteredInnerScreen stockScreen = new CenteredInnerScreen(this, menu.headerX(TraderFactoryMenu.TRADE_STATUS_X), 15, 36, 20,
                this::tradeScreenText);
        stockScreen.tooltip(this::tradeTooltip);
        addRenderableWidget(stockScreen);

        globalPause = addRenderableWidget(new ToggleButton(this, menu.headerX(TraderFactoryMenu.GLOBAL_CONTROL_X),
                TraderFactoryMenu.GLOBAL_CONTROL_Y, PROCESS_PAUSE_SIZE,
                () -> !menu.isTradeEnabled(), (element, x, y) -> { click(FactoryMenu.BUTTON_TRADE_PAUSE); return true; }) {
            @Override
            public void updateTooltip(int mouseX, int mouseY) {
                super.updateTooltip(mouseX, mouseY);
                setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(globalPauseTooltip()));
            }
        });

        for (int process = 0; process < menu.getProcesses(); process++) {
            final int lane = process;
            addRenderableWidget(new GuiProgress(
                    () -> processProgress(lane), ProgressType.DOWN, this,
                    menu.processProgressX(process), TraderFactoryMenu.PROCESS_PROGRESS_Y));
            int x = menu.processX(process);
            ToggleButton pause = new ToggleButton(this, x,
                    TraderFactoryMenu.PROCESS_PAUSE_Y, PROCESS_PAUSE_SIZE,
                    () -> menu.isSlotPaused(lane), (element, mx, my) -> { click(FactoryMenu.BUTTON_TRADE_SLOT_PAUSE_START + lane); return true; }) {
                @Override
                public void updateTooltip(int mouseX, int mouseY) {
                    super.updateTooltip(mouseX, mouseY);
                    setTooltip(mekanism.client.gui.tooltip.TooltipUtils.create(processPauseTooltip(lane)));
                }
            };
            processPauseButtons.add(addRenderableWidget(pause));
        }
    }

    @Override
    protected void configureGuiSlot(GuiSlot guiSlot, Slot slot) {
        if (menu.isGlobalTradeSlot(slot)) {
            guiSlot.click((element, mouseX, mouseY) -> {
                openTradeSelection(-1);
                return true;
            });
            return;
        }
        int process = menu.getProcessTradeSlot(slot);
        if (process >= 0) {
            final int lane = process;
            guiSlot.click((element, mouseX, mouseY) -> {
                openTradeSelection(lane);
                return true;
            });
        }
    }

    private void openTradeSelection(int process) {
        MerchantOffers offers = menu.getTradeOffers();
        if (offers == null || offers.isEmpty()) {
            return;
        }
        addWindow(new TradeSelectionWindow(this, getXSize() / 2 - TradeSelectionWindow.WIDTH / 2,
                getYSize() / 2 - TradeSelectionWindow.HEIGHT / 2, menu, process, this::click));
    }

    private double processProgress(int process) {
        int max = Math.max(1, menu.getTradeDuration());
        return Math.min(1.0, Math.max(0.0, (double) menu.getSlotProgress(process) / max));
    }

    @Override
    protected SlotOverlay overlayFor(Slot slot) {
        if (slot instanceof LockedSlot) {
            MerchantOffer offer;
            if (menu.isGlobalTradeSlot(slot)) {
                offer = menu.getSelectedOffer();
            } else {
                int process = menu.getProcessTradeSlot(slot);
                offer = process < 0 ? null : menu.getProcessOffer(process);
            }
            if (offer != null && !menu.getFactory().hasInfiniteTradeUpgrade()
                    && offer.getMaxUses() > 0 && offer.getUses() >= offer.getMaxUses()) {
                return SlotOverlay.X;
            }
        }
        return super.overlayFor(slot);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (globalPause != null) {
            globalPause.active = true;
        }
        for (ToggleButton button : processPauseButtons) {
            button.active = true;
        }
    }

    private List<Component> tradeScreenText() {
        List<Component> lines = new ArrayList<>();
        int count = menu.getTradeCount();
        if (count <= 0) {
            lines.add(Component.translatable("gui.mekv.no_trades"));
            return lines;
        }
        if (menu.getFactory().hasInfiniteTradeUpgrade()) {
            lines.add(Component.translatable("gui.mekv.trade_unlimited"));
        } else {
            lines.add(Component.translatable("gui.mekv.trade_uses",
                    menu.getTradeUses(), menu.getTradeMaxUses()));
        }
        return lines;
    }

    private List<Component> tradeTooltip() {
        List<Component> lines = new ArrayList<>();
        if (menu.getFactory().hasVillager()) {
            lines.add(Component.translatable("gui.mekv.profession_level",
                    menu.getFactory().getVillagerProfessionName(), menu.getFactory().getVillagerLevel()));
        }
        return lines;
    }

    private List<Component> globalPauseTooltip() {
        List<Component> lines = new ArrayList<>();
        boolean running = menu.isTradeEnabled();
        lines.add(Component.translatable(running ? "gui.mekv.trade_pause" : "gui.mekv.trade_resume"));
        lines.add(Component.translatable(running ? "gui.mekv.trade_running" : "gui.mekv.trade_paused"));
        return lines;
    }

    private List<Component> tradeSelectionTooltip(int process) {
        List<Component> lines = new ArrayList<>();
        if (process < 0) {
            lines.add(Component.translatable("gui.mekv.trade_select_global"));
        } else {
            lines.add(Component.translatable("gui.mekv.trade_select_process", process + 1));
            if (menu.getTradeCount() > 0) {
                lines.add(Component.translatable("gui.mekv.trade_index",
                        menu.getSlotTradeIndex(process) + 1, menu.getTradeCount()));
            }
        }
        return lines;
    }

    private List<Component> processPauseTooltip(int process) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.mekv.process_index", process + 1, menu.getProcesses()));
        lines.add(Component.translatable("gui.mekv.process_pause"));
        lines.add(menu.isSlotPaused(process)
                ? Component.translatable("gui.mekv.process_paused")
                : Component.translatable("gui.mekv.process_running"));
        return lines;
    }

    /** Uses Mekanism's scaled centered renderer so changing label width never shifts the text. */
    private static final class CenteredInnerScreen extends CompatGuiInnerScreen {
        private static final int PADDING = 2;
        private static final int SPACING = 0;
        private static final float TEXT_SCALE = 0.7f;

        private final java.util.function.Supplier<List<Component>> renderStrings;
        private final int screenWidth;
        private final int screenHeight;

        private CenteredInnerScreen(mekanism.client.gui.IGuiWrapper gui, int x, int y, int width, int height,
                                    java.util.function.Supplier<List<Component>> renderStrings) {
            super(gui, x, y, width, height);
            this.renderStrings = renderStrings;
            this.screenWidth = width;
            this.screenHeight = height;
        }

        @Override
        public void renderForeground(GuiGraphics graphics, int mouseX, int mouseY) {
            // The no-supplier parent still renders its children and keeps the
            // normal GuiInnerScreen foreground lifecycle intact.
            super.renderForeground(graphics, mouseX, mouseY);
            List<Component> lines = renderStrings.get();
            if (lines == null || lines.isEmpty()) {
                return;
            }
            int contentHeight = lines.size() * 8 + Math.max(0, lines.size() - 1) * SPACING;
            float textY = relativeY + (screenHeight - contentHeight) / 2.0f;
            for (Component line : lines) {
                drawScaledCenteredTextScaledBound(graphics, line,
                        relativeX + screenWidth / 2, (int) textY, screenTextColor(),
                        screenWidth - PADDING * 2, TEXT_SCALE);
                textY += 8 + SPACING;
            }
        }
    }
}

















