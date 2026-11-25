package net.ixdarklord.trophypedestals.pedestal;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class TrophyHunterEnchantment extends Enchantment {
    public TrophyHunterEnchantment() {
        super(Rarity.RARE, EnchantmentCategory.WEAPON, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
    }

    public static final TagKey<Item> ENCHANTABLE = TagKey.create(Registries.ITEM, TrophyPedestalsMod.id("enchantable/trophy_hunter"));

    public static boolean fits(ItemStack stack) {
        return stack.getItem() instanceof SwordItem || stack.is(ENCHANTABLE);
    }

    @Override
    public int getMinCost(int level) {
        return 15;
    }

    @Override
    public int getMaxCost(int level) {
        return 65;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return fits(stack);
    }

    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return fits(stack);
    }
}
