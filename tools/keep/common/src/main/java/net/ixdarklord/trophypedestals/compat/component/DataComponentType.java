package net.ixdarklord.trophypedestals.compat.component;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 1.20.1's stand-in for 26.1's {@code DataComponentType} (1.20.1 items have no components, only a tag): where on an
 * item's tag a value is kept, and how. Read and written through {@link Components}.
 */
public class DataComponentType<T> {
    // Where it's kept: under these keys, one inside the other ("BlockEntityTag", "Record"), in this codec.
    private @Nullable String[] path;
    private final @Nullable Codec<T> codec;

    protected DataComponentType(@Nullable Codec<T> codec) {
        this.codec = codec;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /** Places it on an item's tag (named by its registration, see {@link DataComponentRegister}). */
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

    // The tag holding its key: made if asked to (and it's missing), else null if it's missing.
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

        /** (Items are sent whole with their tag here: nothing more to say how.) */
        public Builder<T> networkSynchronized(Object streamCodec) {
            return this;
        }

        public DataComponentType<T> build() {
            return new DataComponentType<>(codec);
        }
    }
}
