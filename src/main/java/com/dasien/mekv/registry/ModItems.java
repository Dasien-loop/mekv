package com.dasien.mekv.registry;

import com.dasien.mekv.Mekv;
import com.dasien.mekv.block.VillagerFactoryBlock;
import com.dasien.mekv.factory.FactoryTier;
import com.dasien.mekv.factory.VillagerFactoryType;
import com.dasien.mekv.item.InfiniteTradeUpgradeItem;
import com.dasien.mekv.item.FactoryBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Mekv.MODID);

    public static final RegistryObject<Item> INFINITE_TRADE_UPGRADE = ITEMS.register("upgrade_infinite_trade",
            InfiniteTradeUpgradeItem::new);

    static {
        for (VillagerFactoryType type : VillagerFactoryType.values()) {
            for (FactoryTier tier : FactoryTier.availableValues()) {
                RegistryObject<net.minecraft.world.level.block.Block> block = ModBlocks.get(type, tier);
                ITEMS.register(tier.getSerializedName() + "_" + type.getSerializedName() + "_factory",
                        () -> new FactoryBlockItem((com.dasien.mekv.block.VillagerFactoryBlock) block.get(),
                                new Item.Properties().rarity(tier.rarity())));
            }
        }
    }

    public static List<Item> allFactoryItems() {
        List<Item> items = new ArrayList<>();
        ForgeRegistries.ITEMS.getValues().stream()
                .filter(item -> item instanceof BlockItem blockItem && blockItem.getBlock() instanceof VillagerFactoryBlock)
                .forEach(items::add);
        return items;
    }
}
