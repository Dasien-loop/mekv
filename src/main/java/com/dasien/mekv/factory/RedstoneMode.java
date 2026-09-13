package com.dasien.mekv.factory;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum RedstoneMode implements StringRepresentable {
    IGNORED("ignored"),
    HIGH("high"),
    LOW("low");

    private final String name;

    RedstoneMode(String name) {
        this.name = name;
    }

    public RedstoneMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public RedstoneMode prev() {
        RedstoneMode[] values = values();
        return values[(ordinal() + values.length - 1) % values.length];
    }

    public boolean allows(boolean powered) {
        return switch (this) {
            case IGNORED -> true;
            case HIGH -> powered;
            case LOW -> !powered;
        };
    }

    public Component displayName() {
        return Component.translatable("gui.mekv.redstone." + name);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
