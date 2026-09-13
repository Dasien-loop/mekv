package com.dasien.mekv.recipe;

import com.dasien.mekv.item.FactoryBlockItem;
import com.dasien.mekv.registry.ModRecipes;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

public class FactoryUpgradeRecipe extends ShapedRecipe {
    private FactoryUpgradeRecipe(ShapedRecipe recipe) {
        super(recipe.getId(), recipe.getGroup(), recipe.category(), recipe.getWidth(), recipe.getHeight(),
                recipe.getIngredients(), recipe.getResultItem(RegistryAccess.EMPTY), recipe.showNotification());
    }

    @Override
    public ItemStack assemble(CraftingContainer inventory, RegistryAccess registries) {
        ItemStack result = super.assemble(inventory, registries);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack input = inventory.getItem(slot);
            if (input.getItem() instanceof FactoryBlockItem && input.hasTag()) {
                result.setTag(input.getTag().copy());
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
        public FactoryUpgradeRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new FactoryUpgradeRecipe(RecipeSerializer.SHAPED_RECIPE.fromJson(id, json));
        }

        @Override
        public FactoryUpgradeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new FactoryUpgradeRecipe(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, FactoryUpgradeRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, recipe);
        }
    }
}
