package com.dasien.mekv.mixin;

import com.dasien.mekv.util.FactoryInteraction;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Pseudo;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.jerry.mekanism_extras.common.item.ExtraItemTierInstaller", remap = false)
public abstract class ExtraItemTierInstallerMixin {
    // Pseudo targets are not remapped by the annotation processor. Cover both
    // the development name and Forge 1.20.1's production name explicitly.
    @Inject(method = {"useOn", "m_6225_"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void mekv$useOnFactory(UseOnContext context, CallbackInfoReturnable<InteractionResult> callback) {
        InteractionResult result = FactoryInteraction.useExtraTierInstaller((Item) (Object) this, context);
        if (result != InteractionResult.PASS) {
            callback.setReturnValue(result);
        }
    }
}
