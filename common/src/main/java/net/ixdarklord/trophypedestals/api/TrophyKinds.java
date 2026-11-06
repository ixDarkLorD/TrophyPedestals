package net.ixdarklord.trophypedestals.api;

import net.ixdarklord.trophypedestals.pedestal.PedestalRecord;
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

    private TrophyKinds() {}

    /** Adds a kind of trophy; registering the same id again replaces it. */
    public static void register(ResourceLocation id, TrophyKind kind) {
        KINDS.put(id, kind);
    }

    /** The kind with this id, or null if no mod registered it. */
    public static @Nullable TrophyKind get(ResourceLocation id) {
        return KINDS.get(id);
    }

    /** The name of what this trophy records, or its id if its kind is unknown. */
    public static Component name(PedestalRecord record, boolean client) {
        TrophyKind kind = get(record.kind());
        return kind == null ? Component.literal(record.subject().toString()) : kind.name(record.subject(), client);
    }

    /** The color of that name (RGB). */
    public static int color(PedestalRecord record, boolean client) {
        TrophyKind kind = get(record.kind());
        return kind == null ? 0xFFFFFF : kind.color(record.subject(), client);
    }

    /** What the trophy shows turning over it (empty if its kind is unknown). */
    public static ItemStack icon(PedestalRecord record, boolean client) {
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
