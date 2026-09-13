package com.dasien.mekv.factory;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

public enum RelativeSide implements StringRepresentable {
    FRONT("front"),
    BACK("back"),
    LEFT("left"),
    RIGHT("right"),
    TOP("top"),
    BOTTOM("bottom");

    private final String name;

    RelativeSide(String name) {
        this.name = name;
    }

    public Direction toWorld(Direction facing) {
        return switch (this) {
            case FRONT -> facing;
            case BACK -> facing.getOpposite();
            case LEFT -> facing.getCounterClockWise();
            case RIGHT -> facing.getClockWise();
            case TOP -> Direction.UP;
            case BOTTOM -> Direction.DOWN;
        };
    }

    public static RelativeSide fromWorld(Direction facing, Direction world) {
        if (world == Direction.UP) {
            return TOP;
        }
        if (world == Direction.DOWN) {
            return BOTTOM;
        }
        if (world == facing) {
            return FRONT;
        }
        if (world == facing.getOpposite()) {
            return BACK;
        }
        if (world == facing.getCounterClockWise()) {
            return LEFT;
        }
        return RIGHT;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
