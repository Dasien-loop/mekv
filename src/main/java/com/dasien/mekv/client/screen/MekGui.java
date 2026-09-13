package com.dasien.mekv.client.screen;

import net.minecraft.resources.ResourceLocation;

public final class MekGui {
    public static final ResourceLocation HOLDER_RIGHT = mek("gui/holder_right.png");
    public static final ResourceLocation CONFIG = mek("gui/configuration.png");
    public static final ResourceLocation UPGRADE = mek("gui/upgrade.png");
    public static final ResourceLocation TRANSPORTER = mek("gui/transporter_config.png");
    public static final ResourceLocation AUTO_EJECT = mek("gui/button/auto_eject.png");
    public static final ResourceLocation SORTING = mek("gui/sorting.png");
    public static final ResourceLocation CLEAR_SIDES = mek("gui/button/clear_sides.png");
    public static final ResourceLocation EXCLAMATION = mek("gui/button/exclamation.png");
    public static final ResourceLocation ITEM_CONFIG = mek("gui/items.png");
    public static final ResourceLocation ENERGY_CONFIG = mek("gui/energy.png");
    public static final ResourceLocation UPGRADE_SELECTION = mek("gui/upgrade_selection.png");
    public static final ResourceLocation REDSTONE_DISABLED = mek("gui/redstone_control_disabled.png");
    public static final ResourceLocation REDSTONE_HIGH = mek("gui/redstone_control_high.png");
    public static final ResourceLocation REDSTONE_LOW = mek("gui/redstone_control_low.png");
    public static final ResourceLocation SECURITY_PUBLIC = mek("gui/public.png");
    public static final ResourceLocation SECURITY_PRIVATE = mek("gui/private.png");
    public static final ResourceLocation SECURITY_TRUSTED = mek("gui/protected.png");
    public static final ResourceLocation BUTTON_LEFT = mek("gui/button/left.png");
    public static final ResourceLocation BUTTON_RIGHT = mek("gui/button/right.png");
    public static final ResourceLocation BUTTON_TOGGLE = mek("gui/button/toggle.png");
    public static final ResourceLocation BUTTON_TOGGLE_FLIPPED = mek("gui/button/toggle_flipped.png");
    public static final ResourceLocation BUTTON_RESET = mek("gui/button/reset.png");

    private MekGui() {
    }

    public static ResourceLocation mek(String path) {
        return ResourceLocation.fromNamespaceAndPath("mekanism", path);
    }
}













