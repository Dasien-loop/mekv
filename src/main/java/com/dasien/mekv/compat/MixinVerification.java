package com.dasien.mekv.compat;

import com.dasien.mekv.Mekv;
import com.dasien.mekv.factory.FactoryTier;

/** Development-run check: force lazy targets through the real Mixin transformer. */
public final class MixinVerification {
    private MixinVerification() {}

    public static void verify() {
        verifyTarget("mekanism.common.util.TransporterUtils", "mekv$checkFactoryInputColor");
        verifyTarget("mekanism.common.item.ItemTierInstaller", "mekv$useOnFactory");
        verifyTarget("mekanism.common.item.ItemUpgrade", "mekv$useOnFactory");
        if (FactoryTier.extrasLoaded()) {
            verifyTarget("com.jerry.mekextras.common.item.ItemExtraTierInstaller", "mekv$useOnFactory");
        }
        Mekv.LOGGER.info("MEKV mixin verification passed (Extras: {})", FactoryTier.extrasLoaded());
    }

    private static void verifyTarget(String name, String handler) {
        try {
            Class<?> target = Class.forName(name, true, MixinVerification.class.getClassLoader());
            for (var method : target.getDeclaredMethods()) {
                if (method.getName().contains(handler)) {
                    return;
                }
            }
            throw new IllegalStateException("Missing injected handler on " + name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Missing mixin target " + name, e);
        }
    }
}
