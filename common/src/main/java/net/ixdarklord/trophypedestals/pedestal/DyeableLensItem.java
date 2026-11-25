package net.ixdarklord.trophypedestals.pedestal;

import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;

public class DyeableLensItem extends LensItem implements DyeableLeatherItem {
    public DyeableLensItem(Lens lens, Properties properties) {
        super(lens, properties);
    }

    @Override
    public int getColor(ItemStack stack) {
        return hasCustomColor(stack) ? DyeableLeatherItem.super.getColor(stack) : 0xFFFFFF;
    }
}
