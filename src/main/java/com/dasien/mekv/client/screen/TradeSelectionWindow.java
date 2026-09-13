package com.dasien.mekv.client.screen;

import com.dasien.mekv.menu.FactoryMenu;
import com.dasien.mekv.menu.TraderFactoryMenu;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiElementHolder;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.button.MekanismButton;
import mekanism.client.gui.element.scroll.GuiScrollList;
import mekanism.client.gui.element.slot.SlotType;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.inventory.container.SelectedWindowData;
import mekanism.common.inventory.container.slot.SlotOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;
import java.util.function.IntConsumer;

import static com.dasien.mekv.client.screen.TradeListLayout.*;

/** Selecting a row previews it; applying a trade waits for the menu's authoritative selection. */
public class TradeSelectionWindow extends GuiWindow {
    public static final int WIDTH = 304;
    public static final int HEIGHT = 212;
    private static final int BODY_Y = 30;
    private static final int DETAIL_X = 178;
    private static final int DETAIL_WIDTH = 120;
    private static final int APPLIED_COLOR = 0xFF63B98B;
    private static final int PREVIEW_COLOR = 0xFFFFCF70;
    private static final int WARNING_COLOR = 0xFFFF8585;
    private static final ResourceLocation ARROW = MekGui.mek("gui/right_arrow.png");
    private static final Bounds DETAIL_COST_A = new Bounds(8, 54, 18, 18);
    private static final Bounds DETAIL_COST_B = new Bounds(34, 54, 18, 18);
    private static final Bounds DETAIL_RESULT = new Bounds(92, 54, 18, 18);

    private final TraderFactoryMenu menu;
    private final int process;
    private final IntConsumer click;
    private final TradeList tradeList;
    private final MekanismButton applyButton;
    private int pendingIndex = -1;
    private int pendingTicks;
    private boolean unconfirmed;

    public TradeSelectionWindow(IGuiWrapper gui, int x, int y, TraderFactoryMenu menu,
                                int process, IntConsumer click) {
        super(gui, x, y, WIDTH, HEIGHT, SelectedWindowData.WindowType.UNSPECIFIED);
        this.menu = menu;
        this.process = process;
        this.click = click;
        interactionStrategy = InteractionStrategy.NONE;
        tradeList = addChild(new TradeList(gui, relativeX + 6, relativeY + BODY_Y));
        addChild(new TradeDetails(gui, relativeX + DETAIL_X, relativeY + BODY_Y));
        applyButton = addChild(new MekanismButton(gui, relativeX + DETAIL_X + 6,
                relativeY + BODY_Y + LIST_HEIGHT - 22, DETAIL_WIDTH - 12, 16,
                Component.translatable("gui.mekv.trade_apply"), this::applySelection,
                (element, graphics, mouseX, mouseY) -> element.displayTooltips(graphics, mouseX, mouseY,
                        menu.isTradeSelectionLocked(process)
                                ? Component.translatable("gui.mekv.trade_selection_locked_tooltip")
                                : scope())));
        applyButton.setButtonBackground(ButtonBackground.DIGITAL);
        updateApplyButton();
    }

    private List<MerchantOffer> offers() {
        MerchantOffers offers = menu.getTradeOffers();
        return offers == null ? List.of() : offers;
    }

    private int appliedIndex() {
        int index = process < 0 ? menu.getTradeIndex() : menu.getSlotTradeIndex(process);
        return index >= 0 && index < offers().size() ? index : -1;
    }

    private MerchantOffer previewOffer() {
        List<MerchantOffer> offers = offers();
        return tradeList.selected >= 0 && tradeList.selected < offers.size()
                ? offers.get(tradeList.selected) : null;
    }

    private Component scope() {
        return Component.translatable(process < 0 ? "gui.mekv.trade_select_global"
                : "gui.mekv.trade_select_process", process + 1);
    }

    private void applySelection() {
        if (previewOffer() == null || pendingIndex >= 0 || menu.isTradeSelectionLocked(process)) {
            return;
        }
        int button = FactoryMenu.tradeSelectionButton(process, tradeList.selected);
        if (button >= 0) {
            pendingIndex = tradeList.selected;
            pendingTicks = 0;
            unconfirmed = false;
            click.accept(button);
            updateApplyButton();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (pendingIndex >= 0) {
            if (appliedIndex() == pendingIndex) {
                pendingIndex = -1;
            } else if (++pendingTicks >= 60) {
                pendingIndex = -1;
                unconfirmed = true;
            }
        }
        if (tradeList.selected >= offers().size()) {
            tradeList.clearSelection();
        }
        if (unconfirmed && tradeList.selected >= 0 && tradeList.selected == appliedIndex()) {
            unconfirmed = false;
        }
        updateApplyButton();
    }

    private void updateApplyButton() {
        boolean locked = menu.isTradeSelectionLocked(process);
        boolean applied = tradeList.selected >= 0 && tradeList.selected == appliedIndex();
        boolean unsupportedOffer = previewOffer() != null && unsupported(previewOffer());
        applyButton.active = previewOffer() != null && !unsupportedOffer && !locked && pendingIndex < 0
                && FactoryMenu.tradeSelectionButton(process, tradeList.selected) >= 0
                && (!applied || process < 0);
        applyButton.setMessage(Component.translatable(pendingIndex >= 0 ? "gui.mekv.trade_applying"
                : locked ? "gui.mekv.trade_selection_locked"
                : applied && process >= 0 ? "gui.mekv.trade_applied" : "gui.mekv.trade_apply"));
    }

    @Override
    public void renderForeground(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderForeground(graphics, mouseX, mouseY);
        drawTitleText(graphics, Component.translatable("gui.mekv.trade_select_window"), 5);
        drawScaledTextScaledBound(graphics, scope(), relativeX + 8, relativeY + 19,
                titleTextColor(), WIDTH - 16, 0.8F);
        Component footer = Component.translatable("gui.mekv.trade_offer_count", offers().size());
        drawScaledTextScaledBound(graphics, footer, relativeX + 8, relativeY + 199,
                titleTextColor(), LIST_WIDTH - 4, 0.7F);
        drawScaledTextScaledBound(graphics, Component.translatable("gui.mekv.trade_current_offer",
                        appliedIndex() < 0 ? "-" : appliedIndex() + 1), relativeX + DETAIL_X,
                relativeY + 199, titleTextColor(), DETAIL_WIDTH, 0.7F);
        updateApplyButton();
    }

    private Component stock(MerchantOffer offer) {
        return Component.translatable(menu.getFactory().hasInfiniteTradeUpgrade() ? "gui.mekv.trade_unlimited"
                : offer.isOutOfStock() ? "gui.mekv.trade_sold_out" : "gui.mekv.trade_uses",
                offer.getUses(), offer.getMaxUses());
    }

    private boolean soldOut(MerchantOffer offer) {
        return !menu.getFactory().hasInfiniteTradeUpgrade() && offer.isOutOfStock();
    }

    private static boolean unsupported(MerchantOffer offer) {
        return !offer.getCostA().isEmpty() && !offer.getCostB().isEmpty()
                && !ItemStack.isSameItemSameTags(offer.getCostA(), offer.getCostB());
    }

    private static ItemStack stack(MerchantOffer offer, int part) {
        return switch (part) {
            case 0 -> offer.getCostA();
            case 1 -> offer.getCostB();
            case 2 -> offer.getResult();
            default -> ItemStack.EMPTY;
        };
    }

    private void renderStack(GuiGraphics graphics, ItemStack stack, int x, int y, boolean result) {
        if (stack.isEmpty()) {
            return;
        }
        SlotType type = result ? SlotType.OUTPUT : SlotType.INPUT;
        graphics.blit(type.getTexture(), x, y, 0, 0, 18, 18, 18, 18);
        gui().renderItemWithOverlay(graphics, stack, x + 1, y + 1, 1.0F, null);
    }

    private static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private class TradeList extends GuiScrollList {
        private int selected;

        TradeList(IGuiWrapper gui, int x, int y) {
            super(gui, x, y, LIST_WIDTH, LIST_HEIGHT, ROW_HEIGHT, GuiElementHolder.HOLDER, 32);
            selected = appliedIndex();
            if (selected >= VISIBLE_ROWS && needsScrollBars()) {
                int first = Math.min(selected, getElements());
                scroll = (double) first / getElements();
            }
        }

        @Override
        public boolean hasSelection() {
            return selected >= 0 && selected < offers().size();
        }

        @Override
        protected int getMaxElements() {
            return offers().size();
        }

        @Override
        protected void setSelected(int index) {
            if (index >= 0 && index < offers().size() && pendingIndex < 0) {
                selected = index;
                unconfirmed = false;
            }
        }

        @Override
        public void clearSelection() {
            selected = -1;
        }

        private int offerAt(double mouseX, double mouseY) {
            // barXShift is the scrollbar's X offset, not its width.
            return TradeListLayout.offerAt(mouseX - getX(), mouseY - getY(),
                    barXShift, getCurrentSelection(), offers().size());
        }

        @Override
        public void onClick(double mouseX, double mouseY, int button) {
            int index = offerAt(mouseX, mouseY);
            if (button == 0 && index >= 0) {
                setSelected(index);
            } else if (mouseX >= getX() + barXShift) {
                // Keep Mekanism's scrollbar dragging without its inclusive row-bottom hit test.
                super.onClick(mouseX, mouseY, button);
            }
        }

        @Override
        protected void renderElements(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int first = getCurrentSelection();
            int hovered = offerAt(mouseX, mouseY);
            int applied = appliedIndex();
            int contentWidth = contentWidth(barXShift);
            for (int row = 0; row < VISIBLE_ROWS && first + row < offers().size(); row++) {
                int index = first + row;
                int x = relativeX + 1;
                int y = relativeY + rowY(row);
                int textureY = index == selected ? 24 : index == hovered ? 0 : 12;
                // Stretch only the label portion of one 12px Mekanism selection frame.
                graphics.blit(MekGui.UPGRADE_SELECTION, x, y, contentWidth, ROW_HEIGHT,
                        10, textureY, 48, 12, 58, 36);
                if (index == applied) {
                    graphics.fill(x + 1, y + 1, x + 3, y + ROW_HEIGHT - 1, APPLIED_COLOR);
                }
                if (index == selected) {
                    outline(graphics, x, y, contentWidth, ROW_HEIGHT,
                            index == applied ? APPLIED_COLOR : PREVIEW_COLOR);
                }
            }
        }

        @Override
        public void renderForeground(GuiGraphics graphics, int mouseX, int mouseY) {
            super.renderForeground(graphics, mouseX, mouseY);
            List<MerchantOffer> offers = offers();
            int first = getCurrentSelection();
            for (int row = 0; row < VISIBLE_ROWS && first + row < offers.size(); row++) {
                int index = first + row;
                MerchantOffer offer = offers.get(index);
                int x = relativeX + 1;
                int y = relativeY + rowY(row);
                int textColor = index == selected ? 0xFFFFFFFF : titleTextColor();
                drawScaledTextScaledBound(graphics, offer.getResult().getHoverName(), x + 6, y + 2,
                        textColor, contentWidth(barXShift) - 22, 0.75F);
                if (index == appliedIndex()) {
                    SlotOverlay check = SlotOverlay.CHECK;
                    graphics.blit(check.getTexture(), x + contentWidth(barXShift) - 13, y + 1,
                            10, 10, 0, 0, check.getWidth(), check.getHeight(), check.getWidth(), check.getHeight());
                }
                for (int part = 0; part < 3; part++) {
                    Bounds bounds = itemBounds(part);
                    renderStack(graphics, stack(offer, part), x + bounds.x(), y + bounds.y(), part == 2);
                }
                if (!offer.getCostB().isEmpty()) {
                    drawTextWithScale(graphics, Component.literal("+"), x + 25, y + 18, textColor, 0.75F);
                }
                graphics.blit(ARROW, x + 55, y + 14, 0, 0, 22, 15, 22, 15);
                drawScaledTextScaledBound(graphics, stock(offer), x + 104, y + 13,
                        soldOut(offer) ? index == selected ? WARNING_COLOR : 0xFF9C2424 : textColor, 48, 0.65F);
                drawScaledTextScaledBound(graphics, Component.translatable("gui.mekv.trade_offer_number", index + 1),
                        x + 104, y + 23, textColor, 48, 0.65F);
            }
            if (offers.isEmpty()) {
                drawScaledCenteredTextScaledBound(graphics, Component.translatable("gui.mekv.no_trades"),
                        relativeX + LIST_WIDTH / 2F, relativeY + 70, titleTextColor(), LIST_WIDTH - 16, 0.8F);
            }
        }

        @Override
        public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
            int index = offerAt(mouseX, mouseY);
            if (index < 0) {
                return;
            }
            MerchantOffer offer = offers().get(index);
            int row = index - getCurrentSelection();
            int part = itemAt(mouseX - getX() - 1, mouseY - getY() - rowY(row));
            ItemStack hovered = stack(offer, part);
            if (!hovered.isEmpty()) {
                gui().renderItemTooltip(graphics, hovered, mouseX, mouseY);
            } else if (mouseY - getY() - rowY(row) < 11) {
                gui().renderItemTooltip(graphics, offer.getResult(), mouseX, mouseY);
            }
        }
    }

    private class TradeDetails extends GuiInnerScreen {
        TradeDetails(IGuiWrapper gui, int x, int y) {
            super(gui, x, y, DETAIL_WIDTH, LIST_HEIGHT);
            // GuiScalableElement disables hit testing by default, including tooltip dispatch.
            active = true;
        }

        @Override
        public void renderForeground(GuiGraphics graphics, int mouseX, int mouseY) {
            super.renderForeground(graphics, mouseX, mouseY);
            MerchantOffer offer = previewOffer();
            if (offer == null) {
                drawScaledCenteredTextScaledBound(graphics, Component.translatable("gui.mekv.no_trades"),
                        relativeX + 60, relativeY + 64, screenTextColor(), 108, 0.8F);
                return;
            }
            boolean applied = tradeList.selected == appliedIndex();
            int accent = applied ? APPLIED_COLOR : PREVIEW_COLOR;
            drawScaledTextScaledBound(graphics, Component.translatable(applied
                            ? "gui.mekv.trade_applied" : "gui.mekv.trade_preview"),
                    relativeX + 7, relativeY + 6, accent, 106, 0.85F);
            drawScaledTextScaledBound(graphics, offer.getResult().getHoverName(),
                    relativeX + 7, relativeY + 21, screenTextColor(), 106, 0.9F);
            drawScaledTextScaledBound(graphics, Component.translatable("gui.mekv.trade_cost"),
                    relativeX + 8, relativeY + 42, screenTextColor(), 48, 0.7F);
            drawScaledTextScaledBound(graphics, Component.translatable("gui.mekv.trade_result"),
                    relativeX + 89, relativeY + 42, screenTextColor(), 26, 0.7F);
            Bounds[] bounds = {DETAIL_COST_A, DETAIL_COST_B, DETAIL_RESULT};
            for (int part = 0; part < 3; part++) {
                Bounds slot = bounds[part];
                renderStack(graphics, stack(offer, part), relativeX + slot.x(), relativeY + slot.y(), part == 2);
            }
            if (!offer.getCostB().isEmpty()) {
                drawTextWithScale(graphics, Component.literal("+"), relativeX + 28,
                        relativeY + 60, screenTextColor(), 0.8F);
            }
            graphics.blit(ARROW, relativeX + 61, relativeY + 56, 0, 0, 22, 15, 22, 15);
            outline(graphics, relativeX + 91, relativeY + 53, 20, 20, accent);
            drawScaledTextScaledBound(graphics, stock(offer), relativeX + 7, relativeY + 83,
                    soldOut(offer) ? WARNING_COLOR : screenTextColor(), 106, 0.8F);
            Component status = menu.isTradeSelectionLocked(process)
                    ? Component.translatable("gui.mekv.trade_selection_locked")
                    : unconfirmed ? Component.translatable("gui.mekv.trade_unconfirmed")
                    : unsupported(offer) ? Component.translatable("gui.mekv.status.unsupported_trade")
                    : process < 0 ? Component.translatable("gui.mekv.trade_global_scope")
                    : Component.translatable("gui.mekv.trade_offer_number", tradeList.selected + 1);
            drawWrappedTextWithScale(graphics, status, relativeX + 7, relativeY + 99,
                    menu.isTradeSelectionLocked(process) || unconfirmed || unsupported(offer)
                            ? WARNING_COLOR : screenTextColor(), 106, 0.7F);
        }

        @Override
        public void renderToolTip(GuiGraphics graphics, int mouseX, int mouseY) {
            MerchantOffer offer = previewOffer();
            if (offer == null) {
                return;
            }
            double x = mouseX - getX();
            double y = mouseY - getY();
            Bounds[] slots = {DETAIL_COST_A, DETAIL_COST_B, DETAIL_RESULT};
            for (int part = 0; part < slots.length; part++) {
                ItemStack hovered = stack(offer, part);
                if (slots[part].contains(x, y) && !hovered.isEmpty()) {
                    gui().renderItemTooltip(graphics, hovered, mouseX, mouseY);
                    return;
                }
            }
            if (x >= 7 && x < 113 && y >= 19 && y < 34) {
                gui().renderItemTooltip(graphics, offer.getResult(), mouseX, mouseY);
            } else if (menu.isTradeSelectionLocked(process) && y >= 97) {
                displayTooltips(graphics, mouseX, mouseY,
                        Component.translatable("gui.mekv.trade_selection_locked_tooltip"));
            }
        }
    }
}
