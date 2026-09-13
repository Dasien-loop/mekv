package com.dasien.mekv.registry;

import com.dasien.mekv.Mekv;
import com.dasien.mekv.menu.OutputFactoryMenu;
import com.dasien.mekv.menu.TraderFactoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Mekv.MODID);

    public static final RegistryObject<MenuType<TraderFactoryMenu>> TRADER_FACTORY =
            MENUS.register("trader_factory", () -> IForgeMenuType.create(TraderFactoryMenu::new));

    public static final RegistryObject<MenuType<OutputFactoryMenu>> OUTPUT_FACTORY =
            MENUS.register("output_factory", () -> IForgeMenuType.create(OutputFactoryMenu::new));
}
