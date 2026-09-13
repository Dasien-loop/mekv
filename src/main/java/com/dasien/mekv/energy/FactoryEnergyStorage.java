package com.dasien.mekv.energy;

import net.minecraftforge.energy.EnergyStorage;

public class FactoryEnergyStorage extends EnergyStorage {
    private final Runnable onChanged;

    public FactoryEnergyStorage(int capacity, int maxTransfer, Runnable onChanged) {
        super(capacity, maxTransfer, 0);
        this.onChanged = onChanged;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (!simulate && received > 0) {
            onChanged.run();
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0 && capacity > 0;
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(capacity, energy));
    }

    public void updateCapacity(int newCapacity, int newMaxReceive) {
        this.capacity = Math.max(1, newCapacity);
        this.maxReceive = Math.max(0, newMaxReceive);
        if (this.energy > this.capacity) {
            this.energy = this.capacity;
        }
    }

    public boolean consume(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (energy < amount) {
            return false;
        }
        energy -= amount;
        onChanged.run();
        return true;
    }
}
