package net.ixdarklord.trophypedestals.compat.component;

import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Components {
    private static final Map<String, DataComponentType<?>> BY_ID = new ConcurrentHashMap<>();

    private Components() {}

    static void register(String id, DataComponentType<?> type) {
        BY_ID.put(id, type);
    }

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

    public static void applyToBlockEntity(BlockEntity blockEntity, ItemStack stack) {
        CompoundTag carried = stack.getTag() != null && stack.getTag().contains("BlockEntityTag", Tag.TAG_COMPOUND)
                ? stack.getTag().getCompound("BlockEntityTag") : null;
        if (carried != null) {
            CompoundTag merged = blockEntity.saveWithoutMetadata();
            merged.merge(carried);
            blockEntity.load(merged);
        }
        if (blockEntity instanceof PedestalBlockEntity pedestal) pedestal.applyItemLens(stack);
    }
}
