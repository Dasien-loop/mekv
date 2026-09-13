package com.dasien.mekv.mixin;

import com.dasien.mekv.util.FactoryInteraction;
import mekanism.common.item.ItemUpgrade;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Routes Mekanism upgrades to factories while the player is sneaking. */
@Mixin(value = ItemUpgrade.class, remap = false)
public abstract class ItemUpgradeMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true, remap = true)
    private void mekv$useOnFactory(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        InteractionResult result = FactoryInteraction.useUpgrade(context.getItemInHand(), context);
        if (result != InteractionResult.PASS) {
            callback.setReturnValue(result);
        }
    }
}












