package com.dasien.mekv.mixin;

import com.dasien.mekv.blockentity.VillagerFactoryBlockEntity;
import mekanism.api.text.EnumColor;
import mekanism.common.util.TransporterUtils;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TransporterUtils.class, remap = false)
public abstract class TransporterUtilsMixin {
    // Both public overloads reach this overload, which already has the tile.
    @Inject(method = "canInsert(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lmekanism/api/text/EnumColor;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;Z)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void mekv$checkFactoryInputColor(Level level, BlockPos pos, BlockEntity tile, EnumColor color, ItemStack stack,
                                                     Direction side, boolean force,
                                                     CallbackInfoReturnable<Boolean> callback) {
        if (!force && tile instanceof VillagerFactoryBlockEntity factory
                && !factory.canTransporterInsert(color, side)) {
            callback.setReturnValue(false);
        }
    }
}












