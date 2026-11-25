package net.ixdarklord.trophypedestals.compat.component;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class DataComponentType<T> {
    private @Nullable String[] path;
    private final @Nullable Codec<T> codec;

    protected DataComponentType(@Nullable Codec<T> codec) {
        this.codec = codec;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    void at(String... path) {
        this.path = path;
    }

    protected @Nullable T get(ItemStack stack) {
        CompoundTag holder = holder(stack, false);
        Tag value = holder == null ? null : holder.get(last());
        return value == null || codec == null ? null : codec.parse(NbtOps.INSTANCE, value).result().orElse(null);
    }

    protected void set(ItemStack stack, @Nullable T value) {
        if (value == null || codec == null) {
            remove(stack);
            return;
        }
        codec.encodeStart(NbtOps.INSTANCE, value).result().ifPresent(encoded -> holder(stack, true).put(last(), encoded));
    }

    protected void remove(ItemStack stack) {
        CompoundTag holder = holder(stack, false);
        if (holder != null) holder.remove(last());
    }

    private String last() {
        if (path == null) throw new IllegalStateException("A component used before it was registered");
        return path[path.length - 1];
    }

    private @Nullable CompoundTag holder(ItemStack stack, boolean make) {
        if (path == null) throw new IllegalStateException("A component used before it was registered");
        CompoundTag tag = make ? stack.getOrCreateTag() : stack.getTag();
        for (int i = 0; tag != null && i < path.length - 1; i++) {
            if (tag.contains(path[i], Tag.TAG_COMPOUND)) tag = tag.getCompound(path[i]);
            else if (make) {
                CompoundTag inner = new CompoundTag();
                tag.put(path[i], inner);
                tag = inner;
            } else tag = null;
        }
        return tag;
    }

    public static final class Builder<T> {
        private @Nullable Codec<T> codec;

        public Builder<T> persistent(Codec<T> codec) {
            this.codec = codec;
            return this;
        }

        public Builder<T> networkSynchronized(Object streamCodec) {
            return this;
        }

        public DataComponentType<T> build() {
            return new DataComponentType<>(codec);
        }
    }
}
