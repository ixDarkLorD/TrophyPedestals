package net.ixdarklord.trophypedestals.compat.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.20.1's stand-in for 26.1's item component calls ({@code stack.get(type)}, {@code set}, {@code has}, {@code remove}),
 * over an item's tag (see {@link DataComponentType}).
 */
public final class Components {
    private static final Map<String, DataComponentType<?>> BY_ID = new ConcurrentHashMap<>();

    private Components() {}

    static void register(String id, DataComponentType<?> type) {
        BY_ID.put(id, type);
    }

    /** A component by its id ({@code trophypedestals:pedestal}), as an item definition names it; null for one unknown. */
    public static @Nullable DataComponentType<?> byId(String id) {
        return BY_ID.get(id);
    }

    public static <T> @Nullable T get(ItemStack stack, DataComponentType<T> type) {
        return type.get(stack);
    }

    public static <T> void set(ItemStack stack, DataComponentType<T> type, @Nullable T value) {
        type.set(stack, value);
    }

    public static boolean has(ItemStack stack, DataComponentType<?> type) {
        return type.get(stack) != null;
    }

    public static void remove(ItemStack stack, DataComponentType<?> type) {
        type.remove(stack);
    }

    /**
     * Gives a block entity what an item carries for it (26.1's {@code applyComponentsFromItemStack}): its block entity
     * data, over what the block entity has.
     */
    public static void applyToBlockEntity(BlockEntity blockEntity, ItemStack stack) {
        CompoundTag carried = stack.getTag() != null && stack.getTag().contains("BlockEntityTag", Tag.TAG_COMPOUND)
                ? stack.getTag().getCompound("BlockEntityTag") : null;
        if (carried == null) return;
        CompoundTag merged = blockEntity.saveWithoutMetadata();
        merged.merge(carried);
        blockEntity.load(merged);
    }
}
