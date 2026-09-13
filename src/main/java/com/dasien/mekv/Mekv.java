package com.dasien.mekv;

import com.dasien.mekv.factory.FactoryTier;
import com.dasien.mekv.factory.VillagerFactoryType;
import com.dasien.mekv.blockentity.VillagerFactoryBlockEntity;
import mekanism.common.util.SecurityUtils;
import com.dasien.mekv.registry.ModBlockEntities;
import com.dasien.mekv.registry.ModBlocks;
import com.dasien.mekv.registry.ModItems;
import com.dasien.mekv.registry.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(Mekv.MODID)
public class Mekv {
    public static final String MODID = "mekv";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("mekv", () ->
            CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.get(VillagerFactoryType.TRADER, FactoryTier.BASIC).get()))
                    .title(Component.translatable("itemGroup.mekv"))
                    .displayItems((parameters, output) -> {
                        for (VillagerFactoryType type : VillagerFactoryType.values()) {
                            for (FactoryTier tier : FactoryTier.availableValues()) {
                                output.accept(ModBlocks.get(type, tier).get());
                            }
                        }
                        output.accept(ModItems.INFINITE_TRADE_UPGRADE.get());
                    })
                    .build());

    public Mekv() {
        com.dasien.mekv.network.FactoryNetwork.register();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        com.dasien.mekv.registry.ModRecipes.SERIALIZERS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().getBlockEntity(event.getPos()) instanceof VillagerFactoryBlockEntity factory
                && !factory.canAccess(event.getPlayer())) {
            event.setCanceled(true);
            SecurityUtils.get().displayNoAccess(event.getPlayer());
        }
    }
}
