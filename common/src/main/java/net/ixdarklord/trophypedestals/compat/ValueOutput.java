package net.ixdarklord.trophypedestals.compat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

/**
 * 1.21.1's stand-in for 26.1's {@code ValueOutput}: the same calls, writing into a block entity's saved tag, so the
 * code saving pedestals is the same on every version. Values that can't be encoded are left out.
 */
public final class ValueOutput {
    private final CompoundTag tag;
    private final DynamicOps<Tag> ops;

    private ValueOutput(CompoundTag tag, DynamicOps<Tag> ops) {
        this.tag = tag;
        this.ops = ops;
    }

    public static ValueOutput of(CompoundTag tag, HolderLookup.Provider registries) {
        return new ValueOutput(tag, registries.createSerializationContext(NbtOps.INSTANCE));
    }

    /** A tag written without the game's registries (for removing keys, and values that don't need them). */
    public static ValueOutput of(CompoundTag tag) {
        return new ValueOutput(tag, NbtOps.INSTANCE);
    }

    public <T> void store(String key, Codec<T> codec, T value) {
        codec.encodeStart(ops, value).result().ifPresent(encoded -> tag.put(key, encoded));
    }

    public void putString(String key, String value) {
        tag.putString(key, value);
    }

    public void putFloat(String key, float value) {
        tag.putFloat(key, value);
    }

    public void putInt(String key, int value) {
        tag.putInt(key, value);
    }

    public void putLong(String key, long value) {
        tag.putLong(key, value);
    }

    public void putBoolean(String key, boolean value) {
        tag.putBoolean(key, value);
    }

    public void discard(String key) {
        tag.remove(key);
    }
}
