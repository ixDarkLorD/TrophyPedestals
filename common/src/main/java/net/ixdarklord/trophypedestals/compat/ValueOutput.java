package net.ixdarklord.trophypedestals.compat;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

/**
 * 1.20.1's stand-in for 26.1's {@code ValueOutput}: the same calls, writing into a saved tag, so the code saving
 * pedestals is the same on every version. Values that can't be encoded are left out.
 */
public final class ValueOutput {
    private final CompoundTag tag;

    private ValueOutput(CompoundTag tag) {
        this.tag = tag;
    }

    public static ValueOutput of(CompoundTag tag) {
        return new ValueOutput(tag);
    }

    public <T> void store(String key, Codec<T> codec, T value) {
        codec.encodeStart(NbtOps.INSTANCE, value).result().ifPresent(encoded -> tag.put(key, encoded));
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
