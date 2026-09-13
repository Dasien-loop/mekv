package com.dasien.mekv.factory;

import com.dasien.mekv.compat.ExtrasCompat;
import net.neoforged.fml.ModList;
import mekanism.api.tier.BaseTier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Rarity;

public enum FactoryTier implements StringRepresentable {
    BASIC("basic", 3, 3, 1.0f, 20_000, 0x8A8A8A, Rarity.COMMON),
    ADVANCED("advanced", 5, 5, 1.5f, 80_000, 0xC45C4A, Rarity.UNCOMMON),
    ELITE("elite", 7, 7, 2.0f, 320_000, 0x3A7BD5, Rarity.RARE),
    ULTIMATE("ultimate", 9, 9, 3.0f, 1_280_000, 0x9B59B6, Rarity.EPIC),
    ABSOLUTE("absolute", 0),
    SUPREME("supreme", 1),
    COSMIC("cosmic", 2),
    INFINITE("infinite", 3);

    private final String name;
    private final int processes;
    private final int parallel;
    private final float speed;
    private final int energyCapacity;
    private final int frameColor;
    private final Rarity rarity;
    private final int extraIndex;

    FactoryTier(String name, int processes, int parallel, float speed, int energyCapacity, int frameColor, Rarity rarity) {
        this.name = name;
        this.processes = processes;
        this.parallel = parallel;
        this.speed = speed;
        this.energyCapacity = energyCapacity;
        this.frameColor = frameColor;
        this.rarity = rarity;
        this.extraIndex = -1;
    }

    FactoryTier(String name, int index) {
        this.name = name;
        this.processes = extrasLoaded() ? ExtrasCompat.processes(index) : 0;
        this.parallel = processes;
        // Extras increases process count, not the base speed of each process.
        this.speed = 3.0f;
        this.energyCapacity = (int) ((1_280_000L * processes + 8) / 9);
        this.frameColor = extrasLoaded() ? ExtrasCompat.color(index) : 0;
        this.rarity = Rarity.EPIC;
        this.extraIndex = index;
    }

    public boolean isExtra() {
        return extraIndex >= 0;
    }

    public static boolean extrasLoaded() {
        return ModList.get().isLoaded("mekanism_extras");
    }

    public boolean isAvailable() {
        return !isExtra() || extrasLoaded();
    }

    public int stackMultiplier() {
        return isExtra() ? 8 << extraIndex : 1;
    }

    public int guiWidth() {
        return isExtra() ? 248 + 38 * extraIndex : 214;
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
        FactoryTier next = ordinal() < values().length - 1 ? values()[ordinal() + 1] : null;
        return next != null && next.isAvailable() ? next : null;
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












