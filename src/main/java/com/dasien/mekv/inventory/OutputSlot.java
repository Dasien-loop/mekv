package com.dasien.mekv.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;

public class OutputSlot extends FactorySlot {
    private final BooleanSupplier active;

    public OutputSlot(IItemHandler handler, int index, int x, int y) {
        this(handler, index, x, y, () -> true);
    }

    public OutputSlot(IItemHandler handler, int index, int x, int y, BooleanSupplier active) {
        super(handler, index, x, y);
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return active.getAsBoolean();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }
}












