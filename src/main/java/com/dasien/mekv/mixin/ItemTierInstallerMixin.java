package com.dasien.mekv.mixin;

import com.dasien.mekv.util.FactoryInteraction;
import mekanism.common.item.ItemTierInstaller;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Routes Mekanism tier installers to factories while the player is sneaking. */
@Mixin(value = ItemTierInstaller.class, remap = false)
public abstract class ItemTierInstallerMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true, remap = true)
    private void mekv$useOnFactory(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        InteractionResult result = FactoryInteraction.useTierInstaller((ItemTierInstaller) (Object) this, context);
        if (result != InteractionResult.PASS) {
            callback.setReturnValue(result);
        }
    }
}












