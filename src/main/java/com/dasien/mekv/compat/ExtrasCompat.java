package com.dasien.mekv.compat;

import com.dasien.mekv.factory.FactoryTier;
import com.dasien.mekv.util.FactoryInteraction;
import com.jerry.mekextras.common.item.ItemExtraTierInstaller;
import com.jerry.mekextras.common.tier.ExtraFactoryTier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Loaded only after the caller has confirmed Mekanism Extras is installed. */
public final class ExtrasCompat {
    private ExtrasCompat() {}

    public static int processes(int index) {
        return ExtraFactoryTier.values()[index].processes;
    }

    public static int color(int index) {
        return ExtraFactoryTier.values()[index].getAdvanceTier().getColor().getValue();
    }

    public static boolean isInstaller(Item item) {
        return item instanceof ItemExtraTierInstaller;
    }

    public static InteractionResult useInstaller(Item item, UseOnContext context) {
        if (!(item instanceof ItemExtraTierInstaller installer)) return InteractionResult.PASS;
        FactoryTier from = installer.getFromTier() == null ? FactoryTier.ULTIMATE
                : FactoryTier.valueOf(installer.getFromTier().name());
        return FactoryInteraction.useTierInstaller(from, FactoryTier.valueOf(installer.getToTier().name()), context);
    }
}
