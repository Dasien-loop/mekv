package com.dasien.mekv.compat;

import com.dasien.mekv.Mekv;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.resource.PathPackResources;

@Mod.EventBusSubscriber(modid = Mekv.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ExtrasSupport {
    private ExtrasSupport() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("mekanism_extras");
    }

    @SubscribeEvent
    public static void addDataPack(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA || !isLoaded()) {
            return;
        }
        // Keep recipes, loot and tag entries out of the base data pack when
        // their corresponding blocks and items have not been registered.
        var path = ModList.get().getModFileById(Mekv.MODID).getFile().findResource("extras");
        event.addRepositorySource(consumer -> {
            Pack pack = Pack.readMetaAndCreate("mekv:extras", Component.literal("Mekv Extras"), true,
                    id -> new PathPackResources(id, true, path), PackType.SERVER_DATA,
                    Pack.Position.BOTTOM, PackSource.BUILT_IN);
            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }
}
