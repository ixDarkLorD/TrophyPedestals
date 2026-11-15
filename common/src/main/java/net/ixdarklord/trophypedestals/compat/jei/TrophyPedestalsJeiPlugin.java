package net.ixdarklord.trophypedestals.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.compat.component.Components;
import net.ixdarklord.trophypedestals.pedestal.MobTrophyRecord;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Lists every mob trophy in the mob trophies creative tab as its own entry (a pedestal item's trophy tells its stacks
 * apart by its mob, so one of each kind of mob shows, whoever slew it), and shows what isn't crafted on pages of its
 * own: the trophy each mob drops ({@link MobTrophyCategory}) and the trims of cabinets and shelves
 * ({@link TrimCategory}).
 */
@JeiPlugin
public final class TrophyPedestalsJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return TrophyPedestalsMod.id("jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        Pedestals.ITEMS_BY_STONE.values().forEach(item -> registration.registerSubtypeInterpreter(item.get(), interpreter(TrophyPedestalsJeiPlugin::mob)));
        registration.registerSubtypeInterpreter(Pedestals.CUSTOM_ITEM.get(), interpreter(TrophyPedestalsJeiPlugin::materialAndMob));
    }

    private static @Nullable String mob(ItemStack stack) {
        MobTrophyRecord mob = Components.get(stack, Pedestals.MOB_TROPHY.get());
        return mob == null ? null : mob.entity().toString();
    }

    private static @Nullable String materialAndMob(ItemStack stack) {
        ResourceLocation material = Components.get(stack, Pedestals.PEDESTAL_MATERIAL.get());
        String mob = mob(stack);
        return material == null && mob == null ? null : material + "/" + mob;
    }

    private static IIngredientSubtypeInterpreter<ItemStack> interpreter(Function<ItemStack, @Nullable String> subtype) {
        return (stack, context) -> {
            String data = subtype.apply(stack);
            return data == null ? IIngredientSubtypeInterpreter.NONE : data;
        };
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new MobTrophyCategory(gui), new TrimCategory(gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(MobTrophyCategory.TYPE, MobTrophyCategory.drops());
        registration.addRecipes(TrimCategory.TYPE, TrimCategory.trims());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(Pedestals.DEFAULT.get(), MobTrophyCategory.TYPE);
        registration.addRecipeCatalyst(Items.GOLD_INGOT, TrimCategory.TYPE);
    }
}
