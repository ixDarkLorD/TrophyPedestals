package net.ixdarklord.trophypedestals.api;

import net.ixdarklord.trophypedestals.pedestal.PedestalRecord;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The kinds of trophy other mods add ({@link TrophyKind}), by id: Tierwise's tier trophies are {@code tierwise:tier}.
 * Register one from a mod's constructor, on both sides. A trophy whose kind isn't registered (its mod was removed)
 * still places and keeps its record; it shows its subject's id and no icon.
 */
public final class TrophyKinds {
    private static final Map<ResourceLocation, TrophyKind> KINDS = new ConcurrentHashMap<>();
    // A record's own icon, made once per item (drawn every frame).
    private static final Map<ResourceLocation, ItemStack> ICONS = new ConcurrentHashMap<>();

    private TrophyKinds() {}

    /** Adds a kind of trophy; registering the same id again replaces it. */
    public static void register(ResourceLocation id, TrophyKind kind) {
        KINDS.put(id, kind);
    }

    /** The kind with this id, or null if no mod registered it. */
    public static @Nullable TrophyKind get(ResourceLocation id) {
        return KINDS.get(id);
    }

    /** The name of what this trophy records: its own title, else its kind's name, or its id if its kind is unknown. */
    public static Component name(PedestalRecord record, boolean client) {
        if (record.title().isPresent()) return record.title().get();
        TrophyKind kind = get(record.kind());
        return kind == null ? Component.literal(record.subject().toString()) : kind.name(record.subject(), client);
    }

    /** The color of that name (RGB). */
    public static int color(PedestalRecord record, boolean client) {
        TrophyKind kind = get(record.kind());
        return kind == null ? 0xFFFFFF : kind.color(record.subject(), client);
    }

    /** What the trophy shows turning over it: its own icon, else its kind's (empty if its kind is unknown). */
    public static ItemStack icon(PedestalRecord record, boolean client) {
        if (record.icon().isPresent()) return ICONS.computeIfAbsent(record.icon().get(), id -> BuiltInRegistries.ITEM.getOptional(id).map(ItemStack::new).orElse(ItemStack.EMPTY));
        TrophyKind kind = get(record.kind());
        return kind == null ? ItemStack.EMPTY : kind.icon(record.subject(), client);
    }

    /** The rank a trophy of this kind and subject gets unless one is named: the kind's, or common. */
    public static PedestalRarity rarity(ResourceLocation kind, ResourceLocation subject) {
        TrophyKind found = get(kind);
        PedestalRarity rarity = found == null ? null : found.rarity(subject);
        return rarity == null ? PedestalRarities.COMMON : rarity;
    }
}
