package com.dasien.mekv.registry;

import com.dasien.mekv.Mekv;
import com.dasien.mekv.block.VillagerFactoryBlock;
import com.dasien.mekv.factory.FactoryTier;
import com.dasien.mekv.factory.VillagerFactoryType;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, Mekv.MODID);

    private static final Map<VillagerFactoryType, Map<FactoryTier, Supplier<Block>>> FACTORIES = new EnumMap<>(VillagerFactoryType.class);

    static {
        for (VillagerFactoryType type : VillagerFactoryType.values()) {
            Map<FactoryTier, Supplier<Block>> tiers = new EnumMap<>(FactoryTier.class);
            for (FactoryTier tier : FactoryTier.values()) {
                if (!tier.isAvailable()) continue;
                String id = tier.getSerializedName() + "_" + type.getSerializedName() + "_factory";
                tiers.put(tier, BLOCKS.register(id, () -> new VillagerFactoryBlock(type, tier)));
            }
            FACTORIES.put(type, tiers);
        }
    }

    public static Supplier<Block> get(VillagerFactoryType type, FactoryTier tier) {
        return FACTORIES.get(type).get(tier);
    }

    public static Block[] blocksOf(VillagerFactoryType type) {
        return FACTORIES.get(type).values().stream().map(Supplier::get).toArray(Block[]::new);
    }

    public static List<Supplier<Block>> all() {
        List<Supplier<Block>> list = new ArrayList<>();
        FACTORIES.values().forEach(map -> list.addAll(map.values()));
        return list;
    }
}













