package com.dasien.mekv.factory;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum SideMode implements StringRepresentable {
    NONE("none", ChatFormatting.DARK_GRAY, false, false, false),
    INPUT("input", ChatFormatting.BLUE, true, false, false),
    OUTPUT("output", ChatFormatting.RED, false, true, false),
    INPUT_OUTPUT("input_output", ChatFormatting.DARK_PURPLE, true, true, false),
    ENERGY("energy", ChatFormatting.GREEN, false, false, true);

    private final String name;
    private final ChatFormatting color;
    private final boolean itemInput;
    private final boolean itemOutput;
    private final boolean energyInput;

    SideMode(String name, ChatFormatting color, boolean itemInput, boolean itemOutput, boolean energyInput) {
        this.name = name;
        this.color = color;
        this.itemInput = itemInput;
        this.itemOutput = itemOutput;
        this.energyInput = energyInput;
    }

    public boolean itemInput() {
        return itemInput;
    }

    public boolean itemOutput() {
        return itemOutput;
    }

    public boolean energyInput() {
        return energyInput;
    }

    public SideMode next(boolean allowItemInput) {
        SideMode next = values()[(ordinal() + 1) % values().length];
        if (next == ENERGY || (!allowItemInput && next.itemInput)) {
            return next.next(allowItemInput);
        }
        return next;
    }

    public SideMode prev(boolean allowItemInput) {
        int index = ordinal() - 1;
        if (index < 0) {
            index = values().length - 1;
        }
        SideMode prev = values()[index];
        if (prev == ENERGY || (!allowItemInput && prev.itemInput)) {
            return prev.prev(allowItemInput);
        }
        return prev;
    }

    public Component displayName() {
        return Component.translatable("gui.mekv.side." + name).withStyle(color);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}












