package com.dasien.mekv;

import com.dasien.mekv.factory.FactoryTier;
import com.dasien.mekv.factory.VillagerFactoryType;
import com.dasien.mekv.blockentity.VillagerFactoryBlockEntity;
import mekanism.common.lib.security.SecurityUtils;
import com.dasien.mekv.registry.ModBlockEntities;
import com.dasien.mekv.registry.ModBlocks;
import com.dasien.mekv.registry.ModItems;
import com.dasien.mekv.registry.ModMenus;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
import org.slf4j.Logger;

@Mod(Mekv.MODID)
public class Mekv {
    public static final String MODID = "mekv";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final Supplier<CreativeModeTab> TAB = CREATIVE_MODE_TABS.register("mekv", () ->
            CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModBlocks.get(VillagerFactoryType.TRADER, FactoryTier.BASIC).get()))
                    .title(Component.translatable("itemGroup.mekv"))
                    .displayItems((parameters, output) -> {
                        for (VillagerFactoryType type : VillagerFactoryType.values()) {
                            for (FactoryTier tier : FactoryTier.values()) {
                                if (!tier.isAvailable()) continue;
                                output.accept(ModBlocks.get(type, tier).get());
                            }
                        }
                        output.accept(ModItems.INFINITE_TRADE_UPGRADE.get());
                    })
                    .build());

    public Mekv(net.neoforged.bus.api.IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(com.dasien.mekv.network.FactoryNetwork::register);
        modEventBus.addListener(Mekv::registerCapabilities);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        com.dasien.mekv.registry.ModRecipes.SERIALIZERS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(Config::onLoad);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        if (Boolean.getBoolean("mekv.verifyMixins")) {
            modEventBus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) ->
                    event.enqueueWork(com.dasien.mekv.compat.MixinVerification::verify));
        }
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(com.dasien.mekv.client.ClientSetup::registerScreens);
            modEventBus.addListener(com.dasien.mekv.client.ClientSetup::registerRenderers);
        }
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.TRADER_FACTORY.get(), (be, side) -> be.getItemCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.IRON_GOLEM_FACTORY.get(), (be, side) -> be.getItemCapability(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.FARMER_FACTORY.get(), (be, side) -> be.getItemCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.TRADER_FACTORY.get(), (be, side) -> be.getEnergyCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.IRON_GOLEM_FACTORY.get(), (be, side) -> be.getEnergyCapability(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,
                ModBlockEntities.FARMER_FACTORY.get(), (be, side) -> be.getEnergyCapability(side));
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












