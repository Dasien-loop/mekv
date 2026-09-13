package com.dasien.mekv.inventory;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class SingleStackHandler implements IItemHandlerModifiable {
    private final Supplier<ItemStack> getter;
    private final Consumer<ItemStack> setter;
    private final Predicate<ItemStack> valid;
    private final int limit;

    public SingleStackHandler(Supplier<ItemStack> getter, Consumer<ItemStack> setter, Predicate<ItemStack> valid) {
        this(getter, setter, valid, 1);
    }

    public SingleStackHandler(Supplier<ItemStack> getter, Consumer<ItemStack> setter, Predicate<ItemStack> valid, int limit) {
        this.getter = getter;
        this.setter = setter;
        this.valid = valid;
        this.limit = Math.max(1, limit);
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return getter.get();
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        setter.accept(stack);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !isItemValid(slot, stack)) {
            return stack;
        }
        ItemStack current = getter.get();
        if (!current.isEmpty()) {
            return stack;
        }
        ItemStack inserted = stack.copy();
        inserted.setCount(Math.min(limit, inserted.getCount()));
        if (!simulate) {
            setter.accept(inserted);
        }
        ItemStack remainder = stack.copy();
        remainder.shrink(inserted.getCount());
        return remainder;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack current = getter.get();
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = current.copy();
        extracted.setCount(Math.min(amount, current.getCount()));
        if (!simulate) {
            setter.accept(ItemStack.EMPTY);
        }
        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        return limit;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return !stack.isEmpty() && valid.test(stack);
    }
}
