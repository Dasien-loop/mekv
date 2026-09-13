package com.dasien.mekv.item;

import com.dasien.mekv.block.VillagerFactoryBlock;
import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.text.OwnerDisplay;
import mekanism.common.util.text.EnergyDisplay;
import mekanism.common.util.text.BooleanStateDisplay;
import mekanism.api.security.SecurityMode;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Uses Mekanism's standard hold-for-details/description tooltip behavior. */
public class FactoryBlockItem extends ItemBlockTooltip<VillagerFactoryBlock> {
    public FactoryBlockItem(VillagerFactoryBlock block, Item.Properties properties) {
        super(block, true, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = super.getName(stack);
        VillagerFactoryBlock factoryBlock = (VillagerFactoryBlock) getBlock();
        var tier = factoryBlock.getTier();
        var color = tier.isExtra()
                ? net.minecraft.network.chat.TextColor.fromRgb(com.dasien.mekv.compat.ExtrasCompat.color(tier.ordinal() - 4))
                : tier.baseTier().getColor();
        return mekanism.api.text.TextComponentUtil.build(color, name);
    }

    @Override
    protected void addTypeDetails(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Let Mekanism's common ItemBlockTooltip implementation add all
        // standard machine details first (stored energy when an attachment is
        // present, then our factory-specific type line).
        super.addTypeDetails(stack, context, tooltip, flag);
        VillagerFactoryBlock block = (VillagerFactoryBlock) getBlock();
        Component type = Component.translatable("gui.mekv.factory_type." + block.getFactoryType().getSerializedName());
        tooltip.add(MekanismLang.FACTORY_TYPE.translateColored(EnumColor.INDIGO, EnumColor.GRAY, type));
    }

    @Override
    protected void addDetails(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        // Keep ItemBlockTooltip as the lifecycle/formatting entry point, then
        // supply the factory's legacy block-entity state using Mekanism's
        // native text components. The factory is not a Mekanism BlockType, so
        // its owner and energy cannot be discovered through Mekanism
        // attachments automatically.
        CustomData customData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag data = customData == null ? null : customData.copyTag();
        if (data != null && data.hasUUID("Owner")) {
            tooltip.add(OwnerDisplay.of(MekanismUtils.tryGetClientPlayer(), data.getUUID("Owner"),
                    data.getString("OwnerName").isEmpty() ? null : data.getString("OwnerName")).getTextComponent());
        } else {
            tooltip.add(Component.translatable("owner.mekanism.none"));
        }
        SecurityMode mode = SecurityMode.PUBLIC;
        if (data != null && data.contains("SecurityMode", Tag.TAG_STRING)) {
            try {
                mode = SecurityMode.valueOf(data.getString("SecurityMode"));
            } catch (IllegalArgumentException ignored) {
                // Keep Mekanism's public default for malformed/legacy data.
            }
        }
        tooltip.add(MekanismLang.SECURITY.translateColored(EnumColor.GRAY, mode));
        addTypeDetails(stack, context, tooltip, flag);
        long stored = data != null && data.contains("Energy", Tag.TAG_INT) ? data.getInt("Energy") : 0L;
        tooltip.add(MekanismLang.STORED_ENERGY.translateColored(EnumColor.BRIGHT_GREEN,
                EnergyDisplay.of(stored, ((VillagerFactoryBlock) getBlock()).getTier().energyCapacity()).getTextComponent()));
        tooltip.add(MekanismLang.HAS_INVENTORY.translateColored(EnumColor.AQUA, EnumColor.GRAY,
                BooleanStateDisplay.YesNo.hasInventory(stack)));
    }

}














