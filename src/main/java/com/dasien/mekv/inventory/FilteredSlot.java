package com.dasien.mekv.inventory;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

public class FilteredSlot extends SlotItemHandler {
    private final Predicate<ItemStack> predicate;
    private final int maxSize;
    private final BooleanSupplier active;

    public FilteredSlot(IItemHandler handler, int index, int x, int y, Predicate<ItemStack> predicate) {
        this(handler, index, x, y, predicate, 1);
    }

    public FilteredSlot(IItemHandler handler, int index, int x, int y, Predicate<ItemStack> predicate, int maxSize) {
        this(handler, index, x, y, predicate, maxSize, () -> true);
    }

    public FilteredSlot(IItemHandler handler, int index, int x, int y, Predicate<ItemStack> predicate, int maxSize,
                        BooleanSupplier active) {
        super(handler, index, x, y);
        this.predicate = predicate;
        this.maxSize = maxSize;
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return active.getAsBoolean();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return predicate.test(stack);
    }

    @Override
    public int getMaxStackSize() {
        return maxSize;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Math.min(maxSize, super.getMaxStackSize(stack));
    }
}












