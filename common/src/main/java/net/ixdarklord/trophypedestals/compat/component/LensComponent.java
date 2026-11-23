package net.ixdarklord.trophypedestals.compat.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The lens a pedestal's item comes with, kept where its block entity saves it, so the game hands it to the placed block:
 * {@code BlockEntityTag.Lens}, or {@code BlockEntityTag.NoLens} for a pedestal with its lens taken out. An item with
 * neither has no say, and is fitted with the Clear Lens when placed.
 */
public final class LensComponent extends DataComponentType<ItemContainerContents> {
    private static final String HOLDER = "BlockEntityTag", LENS = "Lens", NO_LENS = "NoLens";

    public LensComponent() {
        super(null);
    }

    @Override
    protected @Nullable ItemContainerContents get(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(HOLDER);
        if (tag == null) return null;
        if (tag.contains(LENS, Tag.TAG_COMPOUND)) {
            ItemStack lens = ItemStack.CODEC.parse(NbtOps.INSTANCE, tag.get(LENS)).result().orElse(ItemStack.EMPTY);
            return lens.isEmpty() ? ItemContainerContents.EMPTY : ItemContainerContents.fromItems(List.of(lens));
        }
        return tag.getBoolean(NO_LENS) ? ItemContainerContents.EMPTY : null;
    }

    @Override
    protected void set(ItemStack stack, @Nullable ItemContainerContents value) {
        remove(stack);
        if (value == null) return;
        CompoundTag tag = stack.getOrCreateTagElement(HOLDER);
        ItemStack lens = value.copyOne();
        if (lens.isEmpty()) tag.putBoolean(NO_LENS, true);
        else ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, lens).result().ifPresent(encoded -> tag.put(LENS, encoded));
    }

    @Override
    protected void remove(ItemStack stack) {
        CompoundTag tag = stack.getTagElement(HOLDER);
        if (tag == null) return;
        tag.remove(LENS);
        tag.remove(NO_LENS);
        if (tag.isEmpty()) stack.removeTagKey(HOLDER);
    }
}
