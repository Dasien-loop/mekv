package com.dasien.mekv.compat;

import com.dasien.mekv.factory.FactoryTier;
import com.dasien.mekv.util.FactoryInteraction;
import com.jerry.mekanism_extras.api.tier.AdvancedTier;
import com.jerry.mekanism_extras.common.item.ExtraItemTierInstaller;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Only entered after checking that Extras is installed. */
public final class ExtrasIntegration {
    private ExtrasIntegration() {
    }

    public static InteractionResult useTierInstaller(Item item, UseOnContext context) {
        if (!(item instanceof ExtraItemTierInstaller installer)) {
            return InteractionResult.PASS;
        }
        FactoryTier from = installer.getFromTier() == null ? FactoryTier.ULTIMATE : fromTier(installer.getFromTier());
        return FactoryInteraction.useTierInstaller(from, fromTier(installer.getToTier()), context);
    }

    private static FactoryTier fromTier(AdvancedTier tier) {
        if (tier == null) {
            return null;
        }
        return switch (tier) {
            case ABSOLUTE -> FactoryTier.ABSOLUTE;
            case SUPREME -> FactoryTier.SUPREME;
            case COSMIC -> FactoryTier.COSMIC;
            case INFINITE -> FactoryTier.INFINITE;
        };
    }
}
