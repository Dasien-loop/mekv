package com.dasien.mekv.client;

import com.dasien.mekv.Mekv;
import com.dasien.mekv.client.render.VillagerFactoryRenderer;
import com.dasien.mekv.client.screen.OutputFactoryScreen;
import com.dasien.mekv.client.screen.TraderFactoryScreen;
import com.dasien.mekv.registry.ModBlockEntities;
import com.dasien.mekv.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Mekv.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.TRADER_FACTORY.get(), TraderFactoryScreen::new);
            MenuScreens.register(ModMenus.OUTPUT_FACTORY.get(), OutputFactoryScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.TRADER_FACTORY.get(), VillagerFactoryRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.IRON_GOLEM_FACTORY.get(), VillagerFactoryRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FARMER_FACTORY.get(), VillagerFactoryRenderer::new);
    }
}
