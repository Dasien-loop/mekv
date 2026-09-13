package com.dasien.mekv.registry;

import com.dasien.mekv.Mekv;
import com.dasien.mekv.menu.OutputFactoryMenu;
import com.dasien.mekv.menu.TraderFactoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Mekv.MODID);

    public static final Supplier<MenuType<TraderFactoryMenu>> TRADER_FACTORY =
            MENUS.register("trader_factory", () -> IMenuTypeExtension.create(TraderFactoryMenu::new));

    public static final Supplier<MenuType<OutputFactoryMenu>> OUTPUT_FACTORY =
            MENUS.register("output_factory", () -> IMenuTypeExtension.create(OutputFactoryMenu::new));
}













