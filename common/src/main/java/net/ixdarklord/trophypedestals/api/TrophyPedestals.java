package net.ixdarklord.trophypedestals.api;

import net.ixdarklord.trophypedestals.pedestal.MobTrophies;
import net.ixdarklord.trophypedestals.pedestal.MobTrophyRecord;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlock;
import net.ixdarklord.trophypedestals.pedestal.PedestalRecord;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Trophy pedestals for other mods: make one recording an achievement, give one to a player, or read the record on one.
 * An achievement is a {@code subject} of a {@code kind} the mod registers with {@link TrophyKinds}, which names it and
 * gives its icon (Tierwise's tier trophies are {@code tierwise:tier}, their subject the tier). Placed, a trophy shows its
 * icon, hologram and text, with its rank's looks and sounds ({@link PedestalRarities}, which mods can add to). Mob
 * trophies too: a pedestal showing a mob, as it looked, with who slew it and when, the same item a mob drops now and
 * then. Data packs do the same with the {@code trophypedestals:set_trophy} and {@code trophypedestals:set_mob_trophy}
 * loot functions, or {@code /trophypedestals mob_trophy}.
 */
public final class TrophyPedestals {
    private TrophyPedestals() {}

    /** The pedestal trophies are on unless another is named (deepslate). */
    public static PedestalBlock defaultPedestal() {
        return Pedestals.DEFAULT.get();
    }

    /** The pedestal block with this id ({@code trophypedestals:quartz_pedestal}), or null if it isn't one. */
    public static @Nullable PedestalBlock pedestal(ResourceLocation id) {
        return Pedestals.pedestal(id);
    }

    /**
     * A trophy on the default pedestal recording {@code subject} of {@code kind} as achieved now, of the kind's own rank
     * for it. {@code name} is the player's or team's name; it's ignored for {@link PedestalRecord.Unlocker#SERVER}.
     */
    public static ItemStack create(ResourceLocation kind, ResourceLocation subject, PedestalRecord.Unlocker unlocker, String name) {
        return create(defaultPedestal(), kind, subject, unlocker, name, System.currentTimeMillis(), TrophyKinds.rarity(kind, subject));
    }

    /** A trophy on this pedestal and of this rarity, recording {@code subject} of {@code kind} as achieved at {@code unlockedAt} (milliseconds since the epoch). */
    public static ItemStack create(PedestalBlock pedestal, ResourceLocation kind, ResourceLocation subject, PedestalRecord.Unlocker unlocker, String name,
                                   long unlockedAt, PedestalRarity rarity) {
        return Pedestals.trophy(pedestal, kind, subject, unlocker, name, unlockedAt, rarity);
    }

    /** A trophy's rarity, an achievement's or a mob's, or null for anything but a trophy (or a mob trophy with no rank). */
    public static @Nullable PedestalRarity rarity(ItemStack stack) {
        PedestalRecord record = record(stack);
        if (record != null) return PedestalRarities.get(record.rarity());
        MobTrophyRecord mob = mobRecord(stack);
        return mob == null || PedestalRarities.isNone(mob.rarity()) ? null : PedestalRarities.get(mob.rarity());
    }

    /** Gives the player these trophies, dropping any that don't fit in their inventory. */
    public static void give(ServerPlayer player, List<ItemStack> trophies) {
        Pedestals.give(player, trophies);
    }

    /**
     * A mob trophy on the default (deepslate) pedestal showing this mob as it looks (its variant, gear, name and the
     * rest), slain by {@code slayer} now, ranked by how tough it is.
     */
    public static ItemStack createMob(LivingEntity mob, String slayer) {
        return createMob(Pedestals.DEFAULT.get(), mob, slayer, System.currentTimeMillis(), MobTrophies.rarity(mob));
    }

    /** A mob trophy on this pedestal and of this rarity, showing this mob as it looks, slain by {@code slayer} at {@code slainAt}. */
    public static ItemStack createMob(PedestalBlock pedestal, LivingEntity mob, String slayer, long slainAt, PedestalRarity rarity) {
        return MobTrophies.trophy(pedestal, mob, slayer, slainAt, rarity.id());
    }

    /**
     * A mob trophy on this pedestal and of this rarity, showing this kind of mob with this look (in the mob's own
     * saved-data format, as {@code /summon} takes it; empty to show it as it spawns).
     */
    public static ItemStack createMob(PedestalBlock pedestal, EntityType<?> type, CompoundTag look, String slayer, long slainAt, PedestalRarity rarity) {
        return MobTrophies.trophy(pedestal, type, look, slayer, slainAt, rarity.id());
    }

    /** What a mob trophy records (the mob, its look, who slew it and when, its rank), or null for anything else. */
    public static @Nullable MobTrophyRecord mobRecord(ItemStack stack) {
        return stack.get(Pedestals.MOB_TROPHY.get());
    }

    /** The achievement a trophy records, or null for anything else (an empty pedestal included). */
    public static @Nullable PedestalRecord record(ItemStack stack) {
        return stack.get(Pedestals.RECORD.get());
    }
}
