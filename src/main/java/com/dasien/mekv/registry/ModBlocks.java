package com.dasien.mekv.registry;

import com.dasien.mekv.Mekv;
import com.dasien.mekv.block.VillagerFactoryBlock;
import com.dasien.mekv.factory.FactoryTier;
import com.dasien.mekv.factory.VillagerFactoryType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Mekv.MODID);

    private static final Map<VillagerFactoryType, Map<FactoryTier, RegistryObject<Block>>> FACTORIES = new EnumMap<>(VillagerFactoryType.class);

    static {
        for (VillagerFactoryType type : VillagerFactoryType.values()) {
            Map<FactoryTier, RegistryObject<Block>> tiers = new EnumMap<>(FactoryTier.class);
            for (FactoryTier tier : FactoryTier.availableValues()) {
                String id = tier.getSerializedName() + "_" + type.getSerializedName() + "_factory";
                tiers.put(tier, BLOCKS.register(id, () -> new VillagerFactoryBlock(type, tier)));
            }
            FACTORIES.put(type, tiers);
        }
    }

    public static RegistryObject<Block> get(VillagerFactoryType type, FactoryTier tier) {
        return FACTORIES.get(type).get(tier);
    }

    public static Block[] blocksOf(VillagerFactoryType type) {
        return FACTORIES.get(type).values().stream().map(RegistryObject::get).toArray(Block[]::new);
    }

    public static List<RegistryObject<Block>> all() {
        List<RegistryObject<Block>> list = new ArrayList<>();
        FACTORIES.values().forEach(map -> list.addAll(map.values()));
        return list;
    }
}
