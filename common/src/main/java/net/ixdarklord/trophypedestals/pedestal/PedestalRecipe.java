package net.ixdarklord.trophypedestals.pedestal;

import com.google.gson.JsonObject;
import net.ixdarklord.trophypedestals.compat.ShapedNbtRecipe;
import net.ixdarklord.trophypedestals.compat.component.Components;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/** A pedestal's shaped recipe: the lens crafted into it comes with it, fitted when it's placed. */
public class PedestalRecipe extends ShapedRecipe {
    public static final RecipeSerializer<PedestalRecipe> SERIALIZER = new Serializer();

    public PedestalRecipe(ShapedRecipe recipe) {
        super(recipe.getId(), recipe.getGroup(), recipe.category(), recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(),
                recipe.getResultItem(RegistryAccess.EMPTY), recipe.showNotification());
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        ItemStack pedestal = super.assemble(input, registries);
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (Lens.of(stack) != null) {
                Components.set(pedestal, Pedestals.LENS.get(), Pedestals.lens(stack));
                break;
            }
        }
        return pedestal;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    private static final class Serializer implements RecipeSerializer<PedestalRecipe> {
        @Override
        public PedestalRecipe fromJson(ResourceLocation id, JsonObject json) {
            return new PedestalRecipe(ShapedNbtRecipe.read(id, json));
        }

        @Override
        public PedestalRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new PedestalRecipe(RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, PedestalRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, recipe);
        }
    }
}
