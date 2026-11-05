package net.ixdarklord.trophypedestals.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.pedestal.MobTrophyRecord;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Lists every mob trophy in the mob trophies creative tab as its own entry (a pedestal item's trophy tells its stacks
 * apart by its mob, so one of each kind of mob shows, whoever slew it). JEI finds it by its annotation on NeoForge and
 * through the {@code jei_mod_plugin} entrypoint on Fabric; nothing else loads this class.
 */
@JeiPlugin
public final class TrophyPedestalsJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return TrophyPedestalsMod.id("jei");
    }

    // Pedestal items are told apart by the mob of the trophy they carry (none for a plain pedestal or an achievement's trophy).
    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        ISubtypeInterpreter<ItemStack> byMob = (stack, context) -> {
            MobTrophyRecord mob = stack.get(Pedestals.MOB_TROPHY.get());
            return mob == null ? null : mob.entity();
        };
        Pedestals.ITEMS_BY_STONE.values().forEach(item -> registration.registerSubtypeInterpreter(item.get(), byMob));
        registration.registerSubtypeInterpreter(Pedestals.CUSTOM_ITEM.get(), byMob);
    }
}
