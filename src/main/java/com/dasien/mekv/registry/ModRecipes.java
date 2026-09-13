package com.dasien.mekv.registry;

import com.dasien.mekv.Mekv;
import com.dasien.mekv.recipe.FactoryUpgradeRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Mekv.MODID);
    public static final Supplier<FactoryUpgradeRecipe.Serializer> FACTORY_UPGRADE =
            SERIALIZERS.register("factory_upgrade", FactoryUpgradeRecipe.Serializer::new);

    private ModRecipes() {
    }
}













