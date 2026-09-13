package com.dasien.mekv.recipe;

import com.dasien.mekv.item.FactoryBlockItem;
import com.dasien.mekv.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/** Shaped recipe that carries the source factory's data components forward. */
public class FactoryUpgradeRecipe extends ShapedRecipe {
    private FactoryUpgradeRecipe(ShapedRecipe recipe) {
        super(recipe.getGroup(), recipe.category(), recipe.pattern,
                recipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY), recipe.showNotification());
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        for (ItemStack stack : input.items()) {
            if (stack.getItem() instanceof FactoryBlockItem && !stack.isComponentsPatchEmpty()) {
                result.applyComponents(stack.getComponentsPatch());
                break;
            }
        }
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.FACTORY_UPGRADE.get();
    }

    public static class Serializer implements RecipeSerializer<FactoryUpgradeRecipe> {
        @Override
        public MapCodec<FactoryUpgradeRecipe> codec() {
            return RecipeSerializer.SHAPED_RECIPE.codec().xmap(
                    FactoryUpgradeRecipe::new,
                    recipe -> recipe);
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, FactoryUpgradeRecipe> streamCodec() {
            return RecipeSerializer.SHAPED_RECIPE.streamCodec().map(
                    FactoryUpgradeRecipe::new,
                    recipe -> recipe);
        }
    }
}
