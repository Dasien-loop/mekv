package com.dasien.mekv.registry;

import com.dasien.mekv.Mekv;
import com.dasien.mekv.blockentity.FarmerFactoryBlockEntity;
import com.dasien.mekv.blockentity.IronGolemFactoryBlockEntity;
import com.dasien.mekv.blockentity.TraderFactoryBlockEntity;
import com.dasien.mekv.factory.VillagerFactoryType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Mekv.MODID);

    public static final Supplier<BlockEntityType<TraderFactoryBlockEntity>> TRADER_FACTORY =
            BLOCK_ENTITIES.register("trader_factory", () ->
                    BlockEntityType.Builder.of(TraderFactoryBlockEntity::new, ModBlocks.blocksOf(VillagerFactoryType.TRADER)).build(null));

    public static final Supplier<BlockEntityType<IronGolemFactoryBlockEntity>> IRON_GOLEM_FACTORY =
            BLOCK_ENTITIES.register("iron_golem_factory", () ->
                    BlockEntityType.Builder.of(IronGolemFactoryBlockEntity::new, ModBlocks.blocksOf(VillagerFactoryType.IRON_GOLEM)).build(null));

    public static final Supplier<BlockEntityType<FarmerFactoryBlockEntity>> FARMER_FACTORY =
            BLOCK_ENTITIES.register("farmer_factory", () ->
                    BlockEntityType.Builder.of(FarmerFactoryBlockEntity::new, ModBlocks.blocksOf(VillagerFactoryType.FARMER)).build(null));
}













