package net.ixdarklord.trophypedestals.pedestal;

import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;

/**
 * The Clear Lens: dyed by vanilla's armor dye recipe, which takes any {@link DyeableLeatherItem} (1.20.1 has no dyed color
 * component, nor a general dye recipe). Its color is kept where leather armor keeps it, {@code display.color}.
 */
public class DyeableLensItem extends LensItem implements DyeableLeatherItem {
    public DyeableLensItem(Lens lens, Properties properties) {
        super(lens, properties);
    }

    /** White until it's dyed (leather's default is brown). */
    @Override
    public int getColor(ItemStack stack) {
        return hasCustomColor(stack) ? DyeableLeatherItem.super.getColor(stack) : 0xFFFFFF;
    }
}
