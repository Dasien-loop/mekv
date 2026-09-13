package com.dasien.mekv.menu;

import com.dasien.mekv.network.FactoryNetwork;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.ItemStack;

/** Vanilla slot packets encode counts as a byte. Large factory slots use integer NBT instead. */
public record FactorySynchronizer(ServerPlayer player, ContainerSynchronizer delegate) implements ContainerSynchronizer {
    @Override
    public void sendInitialData(AbstractContainerMenu menu, NonNullList<ItemStack> items, ItemStack carried, int[] data) {
        NonNullList<ItemStack> vanilla = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            vanilla.set(i, stack.getCount() > 127 ? stack.copyWithCount(1) : stack);
        }
        delegate.sendInitialData(menu, vanilla, carried, data);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getCount() > 127) {
                FactoryNetwork.sendSlot(player, menu, i, items.get(i));
            }
        }
    }

    @Override
    public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack stack) {
        if (stack.getCount() > 127) {
            FactoryNetwork.sendSlot(player, menu, slot, stack);
        } else {
            delegate.sendSlotChange(menu, slot, stack);
        }
    }

    @Override
    public void sendCarriedChange(AbstractContainerMenu menu, ItemStack stack) {
        delegate.sendCarriedChange(menu, stack);
    }

    @Override
    public void sendDataChange(AbstractContainerMenu menu, int index, int value) {
        delegate.sendDataChange(menu, index, value);
    }
}
