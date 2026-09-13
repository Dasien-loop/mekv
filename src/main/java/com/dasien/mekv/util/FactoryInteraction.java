package com.dasien.mekv.util;

import com.dasien.mekv.blockentity.VillagerFactoryBlockEntity;
import com.dasien.mekv.factory.FactoryTier;
import mekanism.common.item.ItemTierInstaller;
import mekanism.common.lib.security.SecurityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Shared item-use handlers for Mekanism-style factory interactions.
 *
 * <p>Vanilla skips {@code Block#use} for a sneaking player unless the held
 * items opt into sneak-bypass interaction. Mekanism therefore implements its
 * direct interactions in the items' {@code useOn} methods. Keeping the
 * server-side mutation here lets the block path and the item paths use the
 * same validation and inventory rules.</p>
 */
public final class FactoryInteraction {
    private FactoryInteraction() {
    }

    public static InteractionResult useTierInstaller(ItemTierInstaller installer, UseOnContext context) {
        return useTierInstaller(FactoryTier.fromBaseTier(installer.getFromTier()),
                FactoryTier.fromBaseTier(installer.getToTier()), context);
    }

    public static InteractionResult useTierInstaller(FactoryTier from, FactoryTier target, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) {
            return InteractionResult.PASS;
        }

        VillagerFactoryBlockEntity factory = getFactory(level, context.getClickedPos());
        if (factory == null) {
            return InteractionResult.PASS;
        }

        FactoryTier current = factory.getTier();
        if (from != current || target == null || target != current.next()) {
            return InteractionResult.PASS;
        }
        if (!factory.canAccess(player)) {
            displayNoAccess(level, player);
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!factory.upgradeTier(target)) {
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        level.playSound(null, context.getClickedPos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 1.2f);
        return InteractionResult.sidedSuccess(false);
    }

    public static InteractionResult useUpgrade(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        VillagerFactoryBlockEntity factory = getFactory(level, context.getClickedPos());
        if (factory == null || !factory.isSupportedUpgrade(stack)) {
            return InteractionResult.PASS;
        }
        if (!factory.canAccess(player)) {
            displayNoAccess(level, player);
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            int accepted = factory.installUpgrade(stack);
            if (accepted > 0 && !player.getAbilities().instabuild) {
                stack.shrink(accepted);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static VillagerFactoryBlockEntity getFactory(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof VillagerFactoryBlockEntity factory ? factory : null;
    }

    private static void displayNoAccess(Level level, Player player) {
        if (!level.isClientSide) {
            SecurityUtils.get().displayNoAccess(player);
        }
    }
}












