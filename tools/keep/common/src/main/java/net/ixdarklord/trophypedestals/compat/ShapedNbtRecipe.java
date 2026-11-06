package net.ixdarklord.trophypedestals.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.ixdarklord.coolcatcore.api.registry.DeferredRegister;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * {@code trophypedestals:crafting_shaped}, on 1.20.1: a shaped crafting recipe whose result can carry data
 * ({@code "result": {"item": ..., "nbt": {...}}}), as 26.1's results carry components, which vanilla 1.20.1's shaped recipes
 * can't (Forge's can; Fabric's can't). A data pack's custom pedestal of a material is one (the port turns 26.1's
 * {@code minecraft:crafting_shaped} with result components into this). The recipe it makes is a vanilla shaped one.
 */
public final class ShapedNbtRecipe {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(TrophyPedestalsMod.MOD_ID, Registries.RECIPE_SERIALIZER);

    static {
        SERIALIZERS.register("crafting_shaped", Serializer::new);
    }

    private ShapedNbtRecipe() {}

    public static void init() {
        SERIALIZERS.register();
    }

    private static final class Serializer implements RecipeSerializer<ShapedRecipe> {
        @Override
        public ShapedRecipe fromJson(ResourceLocation id, JsonObject json) {
            JsonObject plain = json.deepCopy();
            JsonObject result = plain.getAsJsonObject("result");
            CompoundTag data = null;
            if (result != null && result.has("nbt")) {
                try {
                    data = result.get("nbt").isJsonObject() ? TagParser.parseTag(result.get("nbt").toString()) : TagParser.parseTag(result.get("nbt").getAsString());
                } catch (CommandSyntaxException e) {
                    throw new JsonParseException("Invalid result nbt in " + id + ": " + e.getMessage());
                }
                result.remove("nbt");
            }
            ShapedRecipe read = RecipeSerializer.SHAPED_RECIPE.fromJson(id, plain);
            if (data == null) return read;
            ItemStack made = read.getResultItem(RegistryAccess.EMPTY).copy();
            made.getOrCreateTag().merge(data);
            return new ShapedRecipe(id, read.getGroup(), read.category(), read.getWidth(), read.getHeight(), read.getIngredients(), made, read.showNotification());
        }

        @Override
        public ShapedRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buffer);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ShapedRecipe recipe) {
            RecipeSerializer.SHAPED_RECIPE.toNetwork(buffer, recipe);
        }
    }
}
