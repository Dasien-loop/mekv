package com.dasien.mekv.inventory;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.function.BooleanSupplier;

public class FactoryItemHandler implements IItemHandlerModifiable {
    private final IItemHandlerModifiable input;
    private final IItemHandlerModifiable output;
    private final BooleanSupplier allowInsert;
    private final BooleanSupplier allowExtract;

    public FactoryItemHandler(IItemHandlerModifiable input, IItemHandlerModifiable output, boolean allowInsert, boolean allowExtract) {
        this(input, output, () -> allowInsert, () -> allowExtract);
    }

    public FactoryItemHandler(IItemHandlerModifiable input, IItemHandlerModifiable output, BooleanSupplier allowInsert, BooleanSupplier allowExtract) {
        this.input = input;
        this.output = output;
        this.allowInsert = allowInsert;
        this.allowExtract = allowExtract;
    }

    public int inputSlots() {
        return input.getSlots();
    }

    private boolean canInsert() {
        return allowInsert.getAsBoolean();
    }

    private boolean canExtract() {
        return allowExtract.getAsBoolean();
    }

    @Override
    public int getSlots() {
        return input.getSlots() + output.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < input.getSlots()) {
            return input.getStackInSlot(slot);
        }
        return output.getStackInSlot(slot - input.getSlots());
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot < input.getSlots()) {
            input.setStackInSlot(slot, stack);
        } else {
            output.setStackInSlot(slot - input.getSlots(), stack);
        }
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!canInsert() || slot >= input.getSlots()) {
            return stack;
        }
        return input.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!canExtract() || slot < input.getSlots()) {
            return ItemStack.EMPTY;
        }
        return output.extractItem(slot - input.getSlots(), amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        if (slot < input.getSlots()) {
            return input.getSlotLimit(slot);
        }
        return output.getSlotLimit(slot - input.getSlots());
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return canInsert() && slot < input.getSlots() && input.isItemValid(slot, stack);
    }
}
