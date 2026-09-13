package com.dasien.mekv.menu;

import net.minecraft.world.inventory.ContainerData;

/** Vanilla container properties carry signed shorts; split each int into two words. */
public record IntContainerData(ContainerData data) implements ContainerData {
    @Override
    public int get(int index) {
        return (data.get(index / 2) >>> ((index % 2) * 16)) & 0xFFFF;
    }

    @Override
    public void set(int index, int value) {
        int slot = index / 2;
        int shift = (index % 2) * 16;
        data.set(slot, (data.get(slot) & ~(0xFFFF << shift)) | ((value & 0xFFFF) << shift));
    }

    @Override
    public int getCount() {
        return data.getCount() * 2;
    }
}
