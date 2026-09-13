package com.dasien.mekv.item;

import com.dasien.mekv.util.FactoryInteraction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InfiniteTradeUpgradeItem extends Item {
    public InfiniteTradeUpgradeItem() {
        super(new Item.Properties().stacksTo(4).rarity(Rarity.RARE));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return FactoryInteraction.useUpgrade(context.getItemInHand(), context);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mekv.upgrade_infinite_trade"));
    }
}
