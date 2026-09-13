package com.dasien.mekv.registry;

import com.dasien.mekv.Mekv;
import com.dasien.mekv.recipe.FactoryUpgradeRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Mekv.MODID);
    public static final RegistryObject<FactoryUpgradeRecipe.Serializer> FACTORY_UPGRADE =
            SERIALIZERS.register("factory_upgrade", FactoryUpgradeRecipe.Serializer::new);

    private ModRecipes() {
    }
}
