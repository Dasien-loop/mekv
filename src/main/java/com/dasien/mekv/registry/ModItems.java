package com.dasien.mekv.registry;

import com.dasien.mekv.Mekv;
import com.dasien.mekv.block.VillagerFactoryBlock;
import com.dasien.mekv.factory.FactoryTier;
import com.dasien.mekv.factory.VillagerFactoryType;
import com.dasien.mekv.item.InfiniteTradeUpgradeItem;
import com.dasien.mekv.item.FactoryBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;

import java.util.ArrayList;
import java.util.List;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, Mekv.MODID);

    public static final Supplier<Item> INFINITE_TRADE_UPGRADE = ITEMS.register("upgrade_infinite_trade",
            InfiniteTradeUpgradeItem::new);

    static {
        for (VillagerFactoryType type : VillagerFactoryType.values()) {
            for (FactoryTier tier : FactoryTier.values()) {
                if (!tier.isAvailable()) continue;
                Supplier<net.minecraft.world.level.block.Block> block = ModBlocks.get(type, tier);
                ITEMS.register(tier.getSerializedName() + "_" + type.getSerializedName() + "_factory",
                        () -> new FactoryBlockItem((com.dasien.mekv.block.VillagerFactoryBlock) block.get(),
                                new Item.Properties().rarity(tier.rarity())));
            }
        }
    }

    public static List<Item> allFactoryItems() {
        List<Item> items = new ArrayList<>();
        net.minecraft.core.registries.BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof BlockItem blockItem && blockItem.getBlock() instanceof VillagerFactoryBlock)
                .forEach(items::add);
        return items;
    }
}













