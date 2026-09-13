package com.dasien.mekv.client.screen;

import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInnerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Compatibility helpers for Mekanism 1.21.1 inner screens. */
public abstract class CompatGuiInnerScreen extends GuiInnerScreen {
    protected CompatGuiInnerScreen(IGuiWrapper gui, int x, int y, int width, int height) { super(gui, x, y, width, height); }
    protected void displayTooltips(GuiGraphics g, int mx, int my, Component c) { if (isMouseOver(mx, my)) gui().renderItemTooltipWithExtra(g, ItemStack.EMPTY, mx, my, java.util.List.of(c)); }
    protected void drawTextScaledBound(GuiGraphics g, Component c, int x, int y, int color, int width) {
        float scale = Math.min(1.0F, width / (float) Math.max(1, font().width(c)));
        drawScaled(g, c, x, y, color, scale, false);
    }
    protected void drawTextWithScale(GuiGraphics g, Component c, int x, int y, int color, float scale) { drawScaled(g, c, x, y, color, scale, false); }
    protected void drawCenteredText(GuiGraphics g, Component c, int x, int y, int color) { g.drawCenteredString(font(), c, x, y, color); }
    protected void drawScaledTextScaledBound(GuiGraphics g, Component c, int x, int y, int color, int width, float scale) {
        drawScaled(g, c, x, y, color, Math.min(scale, width / (float) Math.max(1, font().width(c))), false);
    }
    protected void drawScaledCenteredTextScaledBound(GuiGraphics g, Component c, int x, int y, int color, int width, float scale) {
        float actual = Math.min(scale, width / (float) Math.max(1, font().width(c)));
        drawScaled(g, c, x, y, color, actual, true);
    }
    protected void drawWrappedTextWithScale(GuiGraphics g, Component c, int x, int y, int color, int width, float scale) {
        drawScaledTextScaledBound(g, c, x, y, color, width, scale);
    }

    private void drawScaled(GuiGraphics g, Component c, int x, int y, int color, float scale, boolean centered) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0F);
        if (centered) {
            g.drawCenteredString(font(), c, 0, 0, color);
        } else {
            g.drawString(font(), c, 0, 0, color, false);
        }
        g.pose().popPose();
    }
}
