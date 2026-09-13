package com.dasien.mekv.factory;

import net.minecraft.network.chat.Component;

public enum FactoryStatus {
    IDLE,
    WORKING,
    NO_VILLAGER,
    NO_ENERGY,
    NO_REDSTONE,
    OUTPUT_FULL,
    NO_INPUT,
    UNSUPPORTED_TRADE,
    OUT_OF_STOCK,
    GROWING,
    NO_WORKSTATION,
    PAUSED;

    public Component displayName() {
        return Component.translatable("gui.mekv.status." + name().toLowerCase());
    }
}
