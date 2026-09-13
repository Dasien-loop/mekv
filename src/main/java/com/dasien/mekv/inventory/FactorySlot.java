package com.dasien.mekv.inventory;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class FactorySlot extends SlotItemHandler {
    private final int handlerSlot;

    public FactorySlot(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
        handlerSlot = index;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return getItemHandler() instanceof FactoryStackHandler handler
                ? handler.getStackLimit(handlerSlot, stack) : super.getMaxStackSize(stack);
    }
}
