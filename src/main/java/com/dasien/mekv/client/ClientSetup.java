package com.dasien.mekv.client;

import com.dasien.mekv.Mekv;
import com.dasien.mekv.client.render.VillagerFactoryRenderer;
import com.dasien.mekv.client.screen.OutputFactoryScreen;
import com.dasien.mekv.client.screen.TraderFactoryScreen;
import com.dasien.mekv.registry.ModBlockEntities;
import com.dasien.mekv.registry.ModMenus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class ClientSetup {
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.TRADER_FACTORY.get(), TraderFactoryScreen::new);
        event.register(ModMenus.OUTPUT_FACTORY.get(), OutputFactoryScreen::new);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.TRADER_FACTORY.get(), VillagerFactoryRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.IRON_GOLEM_FACTORY.get(), VillagerFactoryRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.FARMER_FACTORY.get(), VillagerFactoryRenderer::new);
    }
}













