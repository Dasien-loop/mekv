package com.dasien.mekv.client.screen;

import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.GuiInsetElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public abstract class CompatGuiInsetElement<D> extends GuiInsetElement<D> {
    protected CompatGuiInsetElement(net.minecraft.resources.ResourceLocation texture, IGuiWrapper gui, D data, int x, int y, int width, int height, boolean left) {
        super(texture, gui, data, x, y, width, height, left);
    }
    protected void displayTooltips(GuiGraphics g, int mx, int my, Component c) { if (isMouseOver(mx,my)) gui().renderItemTooltipWithExtra(g, net.minecraft.world.item.ItemStack.EMPTY, mx, my, java.util.List.of(c)); }
    protected void drawTextScaledBound(GuiGraphics g, Component c, int x, int y, int color, int width) { g.drawString(font(), c, x, y, color, false); }
}
