package com.dasien.mekv.client.screen;

import mekanism.client.gui.element.window.GuiWindow;
import mekanism.client.gui.IGuiWrapper;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Compatibility drawing helpers removed from Mekanism's 1.21.1 GUI API. */
public abstract class CompatGuiWindow extends GuiWindow {
    protected CompatGuiWindow(IGuiWrapper gui, int x, int y, int width, int height,
                              mekanism.common.inventory.container.SelectedWindowData.WindowType type) {
        super(gui, x, y, width, height, type);
    }

    protected void displayTooltips(GuiGraphics graphics, int mouseX, int mouseY, Component text) {
        if (isMouseOver(mouseX, mouseY)) {
            gui().renderItemTooltipWithExtra(graphics, ItemStack.EMPTY, mouseX, mouseY, java.util.List.of(text));
        }
    }

    protected void drawTextScaledBound(GuiGraphics graphics, Component text, int x, int y, int color, int width) {
        drawScaledScrollingString(graphics, text, x - relativeX, y - relativeY, mekanism.client.render.IFancyFontRenderer.TextAlignment.LEFT,
                color, width, 0, false, Math.min(1F, width / (float) Math.max(1, font().width(text))));
    }

    protected void drawTextWithScale(GuiGraphics graphics, Component text, int x, int y, int color, float scale) {
        drawScaledScrollingString(graphics, text, x - relativeX, y - relativeY, mekanism.client.render.IFancyFontRenderer.TextAlignment.LEFT,
                color, Math.max(1, (int) Math.ceil(font().width(text) * scale)), 0, false, scale);
    }

    protected void drawCenteredText(GuiGraphics graphics, Component text, int x, int y, int color) {
        graphics.drawCenteredString(font(), text, x, y, color);
    }

    protected void drawScaledTextScaledBound(GuiGraphics graphics, Component text, int x, int y, int color, int width, float scale) {
        drawScaledScrollingString(graphics, text, x - relativeX, y - relativeY, mekanism.client.render.IFancyFontRenderer.TextAlignment.LEFT,
                color, width, 0, false, Math.min(scale, width / (float) Math.max(1, font().width(text))));
    }

    protected void drawScaledCenteredTextScaledBound(GuiGraphics graphics, Component text, int x, int y, int color, int width, float scale) {
        drawScaledScrollingString(graphics, text, x - relativeX - width / 2, y - relativeY, mekanism.client.render.IFancyFontRenderer.TextAlignment.CENTER,
                color, width, 0, false, Math.min(scale, width / (float) Math.max(1, font().width(text))));
    }

    protected void drawWrappedTextWithScale(GuiGraphics graphics, Component text, int x, int y, int color, int width, float scale) {
        new mekanism.client.render.IFancyFontRenderer.WrappedTextRenderer(this, text).renderWithScale(
                graphics, x, y, mekanism.client.render.IFancyFontRenderer.TextAlignment.LEFT, color, width, scale);
    }

    protected java.util.function.Consumer<mekanism.client.gui.element.GuiElement> getOnHover(mekanism.api.text.ILangEntry entry) {
        return element -> element.setTooltip(entry);
    }

}

