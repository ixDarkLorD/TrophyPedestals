package net.ixdarklord.trophypedestals.compat.component;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 1.20.1's stand-in for 26.1's container component, as far as a pedestal's single lens needs it. */
public final class ItemContainerContents {
    public static final ItemContainerContents EMPTY = new ItemContainerContents(ItemStack.EMPTY);
    private final ItemStack item;

    private ItemContainerContents(ItemStack item) {
        this.item = item;
    }

    public static ItemContainerContents fromItems(List<ItemStack> items) {
        return items.isEmpty() || items.get(0).isEmpty() ? EMPTY : new ItemContainerContents(items.get(0).copy());
    }

    public ItemStack copyOne() {
        return item.copy();
    }
}
