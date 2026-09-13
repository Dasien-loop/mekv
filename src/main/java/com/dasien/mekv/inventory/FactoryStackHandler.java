package com.dasien.mekv.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Extras-sized slots with integer counts in NBT; ordinary stacks retain their vanilla format. */
public class FactoryStackHandler extends ItemStackHandler {
    private final int multiplier;

    public FactoryStackHandler(int size, int multiplier) {
        super(size);
        this.multiplier = multiplier;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 64 * multiplier;
    }

    @Override
    public int getStackLimit(int slot, ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize() * multiplier);
    }

    public static CompoundTag saveStack(ItemStack stack, HolderLookup.Provider provider) {
        CompoundTag tag = (CompoundTag) stack.copyWithCount(1).save(provider);
        tag.putInt("FactoryCount", stack.getCount());
        return tag;
    }

    public static ItemStack loadStack(HolderLookup.Provider provider, CompoundTag tag) {
        ItemStack stack = ItemStack.parse(provider, tag).orElse(ItemStack.EMPTY);
        if (tag.contains("FactoryCount", Tag.TAG_INT)) {
            stack.setCount(Math.max(0, tag.getInt("FactoryCount")));
        }
        return stack;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        ListTag items = new ListTag();
        for (int slot = 0; slot < getSlots(); slot++) {
            ItemStack stack = getStackInSlot(slot);
            if (!stack.isEmpty()) {
                CompoundTag entry = stack.getCount() > stack.getMaxStackSize()
                        ? saveStack(stack, provider) : (CompoundTag) stack.save(provider);
                entry.putInt("Slot", slot);
                items.add(entry);
            }
        }
        tag.putInt("Size", getSlots());
        tag.put("Items", items);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        // The block tier owns the size, including when loading an upgraded factory item.
        for (int slot = 0; slot < getSlots(); slot++) {
            stacks.set(slot, ItemStack.EMPTY);
        }
        ListTag items = tag.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag entry = items.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot >= 0 && slot < getSlots()) {
                stacks.set(slot, loadStack(provider, entry));
            }
        }
        onLoad();
    }
}













