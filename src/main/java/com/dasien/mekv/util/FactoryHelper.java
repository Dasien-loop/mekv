package com.dasien.mekv.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

public final class FactoryHelper {
    private FactoryHelper() {
    }

    public static void giveToPlayer(Level level, BlockPos pos, BlockState state, Player player, InteractionHand hand, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (player.getItemInHand(hand).isEmpty()) {
            player.setItemInHand(hand, stack);
            return;
        }
        if (!player.getInventory().add(stack)) {
            Direction facing = state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    ? state.getValue(BlockStateProperties.HORIZONTAL_FACING)
                    : Direction.NORTH;
            Containers.dropItemStack(level,
                    pos.getX() + facing.getStepX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + facing.getStepZ() + 0.5,
                    stack);
        }
    }

    public static void play(Level level, BlockPos pos, SoundEvent sound) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    public static ItemStack insertAll(IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            remaining = handler.insertItem(slot, remaining, simulate);
            if (remaining.isEmpty()) {
                break;
            }
        }
        return remaining;
    }

    public static boolean canFullyInsert(IItemHandler handler, ItemStack stack) {
        return insertAll(handler, stack.copy(), true).isEmpty();
    }

    public static int extractMatching(IItemHandler handler, ItemStack toMatch, boolean simulate) {
        if (toMatch.isEmpty()) {
            return 0;
        }
        int remaining = toMatch.getCount();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack inSlot = handler.getStackInSlot(slot);
            if (inSlot.isEmpty() || !ItemStack.isSameItemSameTags(inSlot, toMatch)) {
                continue;
            }
            ItemStack extracted = handler.extractItem(slot, remaining, simulate);
            remaining -= extracted.getCount();
            if (remaining <= 0) {
                return toMatch.getCount();
            }
        }
        return toMatch.getCount() - remaining;
    }

    public static boolean extractExactly(IItemHandler handler, ItemStack toMatch, boolean simulate) {
        if (toMatch.isEmpty()) {
            return true;
        }
        if (extractMatching(handler, toMatch, true) < toMatch.getCount()) {
            return false;
        }
        if (!simulate) {
            extractMatching(handler, toMatch, false);
        }
        return true;
    }

    public static void dropHandler(Level level, BlockPos pos, IItemHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            }
        }
    }

    public static ItemStack insertIntoNeighbor(IItemHandler target, ItemStack stack) {
        return ItemHandlerHelper.insertItemStacked(target, stack, false);
    }

    public static boolean canFullyInsertAll(IItemHandler handler, List<ItemStack> stacks) {
        ItemStackHandler copy = copy(handler);
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            if (!insertAll(copy, stack.copy(), false).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static ItemStackHandler copy(IItemHandler handler) {
        ItemStackHandler copy = new ItemStackHandler(handler.getSlots());
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            copy.setStackInSlot(slot, handler.getStackInSlot(slot).copy());
        }
        return copy;
    }
}
