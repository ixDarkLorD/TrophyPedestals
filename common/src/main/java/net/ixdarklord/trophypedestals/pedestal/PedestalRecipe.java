package net.ixdarklord.trophypedestals.pedestal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/** A pedestal's shaped recipe: the lens crafted into it comes with it, fitted when it's placed. */
public class PedestalRecipe extends ShapedRecipe {
    public static final MapCodec<PedestalRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(PedestalRecipe::getGroup),
                    CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(PedestalRecipe::category),
                    ShapedRecipePattern.MAP_CODEC.forGetter(o -> o.shape),
                    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(o -> o.output),
                    Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(PedestalRecipe::showNotification))
            .apply(i, PedestalRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, PedestalRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PedestalRecipe::getGroup,
            CraftingBookCategory.STREAM_CODEC, PedestalRecipe::category,
            ShapedRecipePattern.STREAM_CODEC, o -> o.shape,
            ItemStack.STREAM_CODEC, o -> o.output,
            ByteBufCodecs.BOOL, PedestalRecipe::showNotification,
            PedestalRecipe::new);
    public static final RecipeSerializer<PedestalRecipe> SERIALIZER = new RecipeSerializer<>() {
        @Override
        public MapCodec<PedestalRecipe> codec() {
            return MAP_CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PedestalRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    };

    private final ShapedRecipePattern shape;
    private final ItemStack output;

    public PedestalRecipe(String group, CraftingBookCategory category, ShapedRecipePattern pattern, ItemStack result, boolean showNotification) {
        super(group, category, pattern, result, showNotification);
        this.shape = pattern;
        this.output = result;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack pedestal = super.assemble(input, registries);
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (Lens.of(stack) != null) {
                pedestal.set(Pedestals.LENS.get(), Pedestals.lens(stack));
                break;
            }
        }
        return pedestal;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
