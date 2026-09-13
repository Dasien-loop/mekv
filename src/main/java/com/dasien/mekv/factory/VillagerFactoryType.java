package com.dasien.mekv.factory;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public enum VillagerFactoryType implements StringRepresentable {
    TRADER("trader", Blocks.NETHERITE_BLOCK, Blocks.NETHERITE_BLOCK),
    IRON_GOLEM("iron_golem", Blocks.LAVA, Blocks.STONE),
    FARMER("farmer", Blocks.DIRT, Blocks.DIRT);

    private final String name;
    private final Block floorFront;
    private final Block floorBack;

    VillagerFactoryType(String name, Block floorFront, Block floorBack) {
        this.name = name;
        this.floorFront = floorFront;
        this.floorBack = floorBack;
    }

    public Block floorFront() {
        return floorFront;
    }

    public Block floorBack() {
        return floorBack;
    }

    public String translationKey() {
        return "block.mekv." + name + "_factory";
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}












