package net.ixdarklord.trophypedestals.compat;

import com.mojang.serialization.Codec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 1.21.1's stand-in for 26.1's {@code ValueInput}: the same calls, reading a saved tag, so the code loading pedestals
 * is the same on every version. A value that's missing, of the wrong type, or can't be decoded reads as its default.
 */
public final class ValueInput {
    private final CompoundTag tag;
    private final @Nullable HolderLookup.Provider registries;

    private ValueInput(CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        this.tag = tag;
        this.registries = registries;
    }

    public static ValueInput of(CompoundTag tag, HolderLookup.Provider registries) {
        return new ValueInput(tag, registries);
    }

    /** A tag read without the game's registries (for values that don't need them: names, ids, components' text). */
    public static ValueInput of(CompoundTag tag) {
        return new ValueInput(tag, null);
    }

    public <T> Optional<T> read(String key, Codec<T> codec) {
        Tag value = tag.get(key);
        if (value == null) return Optional.empty();
        return codec.parse(registries == null ? NbtOps.INSTANCE : registries.createSerializationContext(NbtOps.INSTANCE), value).result();
    }

    /**
     * A name saved as an entity saves one here (the text's JSON in a string): 26.1 reads it with
     * {@code ComponentSerialization.CODEC}. A plain string that isn't JSON reads as that text.
     */
    public Optional<Component> readName(String key) {
        if (!tag.contains(key, Tag.TAG_STRING)) return Optional.empty();
        String saved = tag.getString(key);
        try {
            Component name = Component.Serializer.fromJson(saved, registries == null ? RegistryAccess.EMPTY : registries);
            if (name != null) return Optional.of(name);
        } catch (RuntimeException ignored) {
            // (Not JSON: the text itself.)
        }
        return Optional.of(Component.literal(saved));
    }

    public Optional<String> getString(String key) {
        return tag.contains(key, Tag.TAG_STRING) ? Optional.of(tag.getString(key)) : Optional.empty();
    }

    public String getStringOr(String key, String fallback) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : fallback;
    }

    public float getFloatOr(String key, float fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getFloat(key) : fallback;
    }

    public int getIntOr(String key, int fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
    }

    public long getLongOr(String key, long fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getLong(key) : fallback;
    }

    public boolean getBooleanOr(String key, boolean fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getBoolean(key) : fallback;
    }
}
