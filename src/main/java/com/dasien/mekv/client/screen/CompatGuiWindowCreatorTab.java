package com.dasien.mekv.client.screen;

import mekanism.client.gui.IGuiWrapper;
import mekanism.client.gui.element.tab.window.GuiWindowCreatorTab;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public abstract class CompatGuiWindowCreatorTab<D,E extends CompatGuiWindowCreatorTab<D,E>> extends GuiWindowCreatorTab<D,E> {
    protected CompatGuiWindowCreatorTab(net.minecraft.resources.ResourceLocation texture, IGuiWrapper gui, D data, int x, int y, int width, int height, boolean left, java.util.function.Supplier<E> self) { super(texture, gui, data, x, y, width, height, left, self); }
    protected void displayTooltips(GuiGraphics g,int mx,int my,Component c){if(isMouseOver(mx,my))gui().renderItemTooltipWithExtra(g,net.minecraft.world.item.ItemStack.EMPTY,mx,my,java.util.List.of(c));}
}
