package com.dasien.mekv.item;

import com.dasien.mekv.block.VillagerFactoryBlock;
import com.dasien.mekv.factory.VillagerFactoryType;
import mekanism.api.math.FloatingLong;
import mekanism.api.security.SecurityMode;
import mekanism.api.text.EnumColor;
import mekanism.api.text.TextComponentUtil;
import mekanism.common.MekanismLang;
import mekanism.common.item.block.ItemBlockTooltip;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.text.BooleanStateDisplay;
import mekanism.common.util.text.EnergyDisplay;
import mekanism.common.util.text.OwnerDisplay;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/** Uses Mekanism's standard hold-for-details/description tooltip behavior. */
public class FactoryBlockItem extends ItemBlockTooltip<VillagerFactoryBlock> {
    public FactoryBlockItem(VillagerFactoryBlock block, Item.Properties properties) {
        super(block, true, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component name = super.getName(stack);
        return getBlock().getTier().isExtra()
                ? name.copy().withStyle(style -> style.withColor(getBlock().getTier().frameColor()))
                : name;
    }

    @Override
    protected void addTypeDetails(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        VillagerFactoryBlock block = getBlock();
        Component type = Component.translatable("gui.mekv.factory_type." + block.getFactoryType().getSerializedName());
        tooltip.add(MekanismLang.FACTORY_TYPE.translateColored(EnumColor.INDIGO, EnumColor.GRAY, type));
        if (block.getFactoryType() == VillagerFactoryType.TRADER) {
            tooltip.add(TextComponentUtil.build(EnumColor.INDIGO,
                    Component.translatable("tooltip.mekv.factory.slots",
                            TextComponentUtil.build(EnumColor.GRAY, block.getTier().processes()))));
        } else {
            tooltip.add(Component.translatable("tooltip.mekv.factory.parallel", block.getTier().parallel()));
        }
        super.addTypeDetails(stack, level, tooltip, flag);
    }

    @Override
    protected void addDetails(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        // Vanilla adds this tag for Ctrl+pick-block, while an ordinary pick-block stack has no tag.
        CompoundTag data = BlockItem.getBlockEntityData(stack);
        if (data != null) {
            addSecurityDetails(data, tooltip);
        }
        addTypeDetails(stack, level, tooltip, flag);
        int stored = data == null ? 0 : data.getInt("Energy");
        tooltip.add(MekanismLang.STORED_ENERGY.translateColored(EnumColor.BRIGHT_GREEN, EnumColor.GRAY,
                EnergyDisplay.of(FloatingLong.create(stored),
                        FloatingLong.create(getBlock().getTier().energyCapacity()))));
        boolean hasInventory = data != null && (data.contains("Villager") || data.contains("Input") || data.contains("Output"));
        tooltip.add(MekanismLang.HAS_INVENTORY.translateColored(EnumColor.AQUA, EnumColor.GRAY,
                BooleanStateDisplay.YesNo.of(hasInventory)));
    }

    private static void addSecurityDetails(CompoundTag data, List<Component> tooltip) {
        boolean hasOwner = data.hasUUID("Owner") || data.contains("OwnerName", Tag.TAG_STRING);
        boolean hasSecurity = data.contains("SecurityMode", Tag.TAG_STRING);
        if (!hasOwner && !hasSecurity) {
            return;
        }

        UUID ownerUUID = data.hasUUID("Owner") ? data.getUUID("Owner") : null;
        String ownerName = data.getString("OwnerName");
        tooltip.add(OwnerDisplay.of(MekanismUtils.tryGetClientPlayer(), ownerUUID,
                ownerName.isEmpty() ? null : ownerName).getTextComponent());

        if (hasSecurity) {
            try {
                SecurityMode mode = SecurityMode.valueOf(data.getString("SecurityMode"));
                tooltip.add(MekanismLang.SECURITY.translateColored(EnumColor.GRAY, mode));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed external NBT instead of showing an unlocalized enum name.
            }
        }
    }

}
