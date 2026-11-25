package net.ixdarklord.trophypedestals.fabric.mixin;

import net.ixdarklord.trophypedestals.pedestal.MobTrophies;
import net.ixdarklord.trophypedestals.pedestal.TrophyHunterEnchantment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {
    @Inject(method = "getAvailableEnchantmentResults", at = @At("RETURN"))
    private static void trophypedestals$trophyHunterOnAxes(int power, ItemStack stack, boolean treasure, CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        if (EnchantmentCategory.WEAPON.canEnchant(stack.getItem()) || !TrophyHunterEnchantment.fits(stack)) return;
        Enchantment hunter = MobTrophies.TROPHY_HUNTER.get();
        if (power >= hunter.getMinCost(1) && power <= hunter.getMaxCost(1)) cir.getReturnValue().add(new EnchantmentInstance(hunter, 1));
    }
}
