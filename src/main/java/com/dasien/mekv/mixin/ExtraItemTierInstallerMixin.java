package com.dasien.mekv.mixin;

import com.dasien.mekv.compat.ExtrasCompat;
import com.jerry.mekextras.common.item.ItemExtraTierInstaller;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@org.spongepowered.asm.mixin.Pseudo
@Mixin(targets = "com.jerry.mekextras.common.item.ItemExtraTierInstaller", remap = false)
public abstract class ExtraItemTierInstallerMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true, remap = true)
    private void mekv$useOnFactory(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        InteractionResult result = ExtrasCompat.useInstaller((ItemExtraTierInstaller) (Object) this, context);
        if (result != InteractionResult.PASS) {
            callback.setReturnValue(result);
        }
    }
}












