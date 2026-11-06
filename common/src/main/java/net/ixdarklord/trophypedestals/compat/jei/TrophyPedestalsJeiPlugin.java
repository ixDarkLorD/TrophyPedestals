package net.ixdarklord.trophypedestals.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.pedestal.MobTrophyRecord;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Lists every mob trophy in the mob trophies creative tab as its own entry (a pedestal item's trophy tells its stacks
 * apart by its mob, so one of each kind of mob shows, whoever slew it), and shows what isn't crafted on pages of its own:
 * the trophy each mob drops ({@link MobTrophyCategory}) and the trims of cabinets and shelves ({@link TrimCategory}).
 * JEI finds it by its annotation on NeoForge and
 * through the {@code jei_mod_plugin} entrypoint on Fabric; nothing else loads this class.
 */
@JeiPlugin
public final class TrophyPedestalsJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return TrophyPedestalsMod.id("jei");
    }

    // Pedestal items are told apart by the mob of the trophy they carry (none for a plain pedestal or an achievement's
    // trophy), and the custom pedestal by its material too (one entry for each a data pack makes).
    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        Pedestals.ITEMS_BY_STONE.values().forEach(item -> registration.registerSubtypeInterpreter(item.get(), interpreter(TrophyPedestalsJeiPlugin::mob)));
        registration.registerSubtypeInterpreter(Pedestals.CUSTOM_ITEM.get(), interpreter(TrophyPedestalsJeiPlugin::materialAndMob));
    }

    private static @Nullable String mob(ItemStack stack) {
        MobTrophyRecord mob = stack.get(Pedestals.MOB_TROPHY.get());
        return mob == null ? null : mob.entity().toString();
    }

    private static @Nullable String materialAndMob(ItemStack stack) {
        Identifier material = stack.get(Pedestals.PEDESTAL_MATERIAL.get());
        String mob = mob(stack);
        return material == null && mob == null ? null : material + "/" + mob;
    }

    // A subtype interpreter from a subtype's name (null for none).
    private static ISubtypeInterpreter<ItemStack> interpreter(Function<ItemStack, @Nullable String> subtype) {
        return (stack, context) -> subtype.apply(stack);
    }

    // What isn't crafted: the trophy each mob can drop, and the trims of cabinets and shelves.
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

    // The default pedestal (mob trophies drop on it) and the gold ingot (the gold trim) lead to their pages.
    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(MobTrophyCategory.TYPE, Pedestals.DEFAULT.get());
        registration.addCraftingStation(TrimCategory.TYPE, Items.GOLD_INGOT);
    }
}
