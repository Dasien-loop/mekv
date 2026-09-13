package com.dasien.mekv.factory;

import com.dasien.mekv.compat.ExtrasSupport;
import java.util.Arrays;
import mekanism.api.tier.BaseTier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Rarity;

public enum FactoryTier implements StringRepresentable {
    BASIC("basic", 3, 3, 1.0f, 20_000, 0x8A8A8A, Rarity.COMMON),
    ADVANCED("advanced", 5, 5, 1.5f, 80_000, 0xC45C4A, Rarity.UNCOMMON),
    ELITE("elite", 7, 7, 2.0f, 320_000, 0x3A7BD5, Rarity.RARE),
    ULTIMATE("ultimate", 9, 9, 3.0f, 1_280_000, 0x9B59B6, Rarity.EPIC),
    ABSOLUTE("absolute", 11, 0x5FFFB8),
    SUPREME("supreme", 13, 0xFF806A),
    COSMIC("cosmic", 15, 0x4BF8FF),
    INFINITE("infinite", 17, 0xF787FF);

    private final String name;
    private final int processes;
    private final int parallel;
    private final float speed;
    private final int energyCapacity;
    private final int frameColor;
    private final Rarity rarity;

    FactoryTier(String name, int processes, int parallel, float speed, int energyCapacity, int frameColor, Rarity rarity) {
        this.name = name;
        this.processes = processes;
        this.parallel = parallel;
        this.speed = speed;
        this.energyCapacity = energyCapacity;
        this.frameColor = frameColor;
        this.rarity = rarity;
    }

    FactoryTier(String name, int processes, int frameColor) {
        this(name, processes, processes, 3.0f, (int) ((1_280_000L * processes + 8) / 9), frameColor, Rarity.EPIC);
    }

    public boolean isExtra() {
        return ordinal() >= ABSOLUTE.ordinal();
    }

    public static FactoryTier[] availableValues() {
        return availableValues(ExtrasSupport.isLoaded());
    }

    public static FactoryTier[] availableValues(boolean extrasLoaded) {
        return Arrays.stream(values()).filter(tier -> extrasLoaded || !tier.isExtra()).toArray(FactoryTier[]::new);
    }

    public int stackMultiplier() {
        return isExtra() ? 8 << (ordinal() - ABSOLUTE.ordinal()) : 1;
    }

    public int guiWidth() {
        return isExtra() ? 248 + 38 * (ordinal() - ABSOLUTE.ordinal()) : 214;
    }

    public int processX(int process) {
        int base = switch (this) {
            case BASIC -> 55;
            case ADVANCED -> 35;
            case ELITE -> 29;
            default -> 27;
        };
        int spacing = this == BASIC ? 38 : this == ADVANCED ? 26 : 19;
        return base + process * spacing;
    }

    public int centeredSlotX(int slot, int slotCount) {
        int middleX = (processX(0) + processX(processes - 1)) / 2;
        return middleX - (slotCount - 1) * 9 + slot * 18;
    }

    public int processes() {
        return processes;
    }

    /**
     * Number of output operations performed by one farmer or iron golem cycle.
     * Trader factories use the same value as their number of independent slots.
     */
    public int parallel() {
        return parallel;
    }

    public float speed() {
        return speed;
    }

    public int energyCapacity() {
        return energyCapacity;
    }

    public int maxEnergyTransfer() {
        return Math.max(256, energyCapacity / 40);
    }

    public int frameColor() {
        return frameColor;
    }

    public Rarity rarity() {
        return rarity;
    }

    public FactoryTier next() {
        return next(ExtrasSupport.isLoaded());
    }

    public FactoryTier next(boolean extrasLoaded) {
        FactoryTier next = ordinal() < values().length - 1 ? values()[ordinal() + 1] : null;
        return next != null && (extrasLoaded || !next.isExtra()) ? next : null;
    }

    public BaseTier baseTier() {
        return switch (this) {
            case BASIC -> BaseTier.BASIC;
            case ADVANCED -> BaseTier.ADVANCED;
            case ELITE -> BaseTier.ELITE;
            case ULTIMATE -> BaseTier.ULTIMATE;
            default -> null;
        };
    }

    public static FactoryTier fromBaseTier(BaseTier tier) {
        if (tier == null) {
            return null;
        }
        return switch (tier) {
            case BASIC -> BASIC;
            case ADVANCED -> ADVANCED;
            case ELITE -> ELITE;
            case ULTIMATE -> ULTIMATE;
            default -> null;
        };
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
