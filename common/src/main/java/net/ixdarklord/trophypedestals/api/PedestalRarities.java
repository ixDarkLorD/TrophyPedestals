package net.ixdarklord.trophypedestals.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The trophy pedestal rarities, the ranks a trophy's looks and sounds come from. Trophy Pedestals's five grow from a plain grey
 * glow to a golden beam of light:
 * <ul>
 *   <li>{@link #COMMON} (1): a soft grey light, three outlines, a faint flare, a two-armed spiral and a rare glint; no
 *   sound of its own.</li>
 *   <li>{@link #UNCOMMON} (2): green, faster outlines, a stronger flare and more glints; a chime when placed.</li>
 *   <li>{@link #RARE} (3): blue, a fuller flare, more outlines and glints, a halo circling the icon and a spark
 *   orbiting it; a bell when placed, and a chime now and then.</li>
 *   <li>{@link #EPIC} (4): purple and pink, shimmering outlines, a pulsing double flare, two crossed halos, a
 *   three-armed spiral and two orbiting sparks; an enchanting sound when placed, and amethyst resonating now and then.</li>
 *   <li>{@link #LEGENDARY} (5): gold, a beam of light, shimmering outlines, a large pulsing double flare, a turning
 *   crown of light shards, three orbiting sparks and bursts of light; a fanfare when placed, and a beacon's hum.</li>
 * </ul>
 * A trophy's kind picks its rank ({@code TrophyKind.rarity}; Tierwise reads a tier's {@code "trophy_rarity"}); without one it's common. Other mods
 * add theirs with {@link #register} from their common setup, so servers and clients both know them, and give each a
 * name under the {@code trophypedestals.rarity.<namespace>.<path>} lang key.
 */
public final class PedestalRarities {
    private static final Map<ResourceLocation, PedestalRarity> RARITIES = Collections.synchronizedMap(new LinkedHashMap<>());

    public static final PedestalRarity COMMON = register(PedestalRarity.builder(TrophyPedestalsMod.id("common"), 1, 0xB8BEC6, 0xE8ECF0)
            .rings(3, 36.0F).flare(1.05F, 0.3F, 0.5F).spiral(2, 0.5F).glints(1 / 60.0F).build());
    public static final PedestalRarity UNCOMMON = register(PedestalRarity.builder(TrophyPedestalsMod.id("uncommon"), 2, 0x5BD46E, 0xC6F7B4)
            .rings(3, 32.0F).flare(1.3F, 0.55F, 0.7F).spiral(2, 0.67F).glints(1 / 28.0F)
            .placeSound(SoundEvents.AMETHYST_BLOCK_CHIME.getLocation(), 1.0F, 1.2F).build());
    public static final PedestalRarity RARE = register(PedestalRarity.builder(TrophyPedestalsMod.id("rare"), 3, 0x4A9DFF, 0xB4DCFF)
            .rings(4, 29.0F).flare(1.6F, 0.8F, 0.9F).spiral(2, 0.85F).glints(1 / 14.0F).orbiters(1).halos(1, 0.46F)
            .placeSound(SoundEvents.NOTE_BLOCK_CHIME.value().getLocation(), 0.8F, 1.4F)
            .ambientSound(SoundEvents.AMETHYST_BLOCK_CHIME.getLocation(), 0.4F, 1.5F, 240).build());
    public static final PedestalRarity EPIC = register(PedestalRarity.builder(TrophyPedestalsMod.id("epic"), 4, 0xB25CFF, 0xFF8BEA)
            .rings(5, 26.0F).flare(1.85F, 1.0F, 1.1F).flarePulse(0.1F).secondFlare().spiral(3, 0.75F).glints(1 / 9.0F)
            .orbiters(2).halos(2, 0.6F).shimmer().placeSound(SoundEvents.ENCHANTMENT_TABLE_USE.getLocation(), 1.0F, 1.1F)
            .ambientSound(SoundEvents.AMETHYST_BLOCK_RESONATE.getLocation(), 0.5F, 1.2F, 180).build());
    public static final PedestalRarity LEGENDARY = register(PedestalRarity.builder(TrophyPedestalsMod.id("legendary"), 5, 0xFFB52E, 0xFFF4B0)
            .rings(6, 22.0F).flare(2.15F, 1.0F, 1.3F).flarePulse(0.14F).secondFlare().spiral(3, 1.0F).glints(1 / 5.0F)
            .orbiters(3).crown(8).beam(3.0F).shimmer().burst(70).placeSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE.getLocation(), 0.7F, 1.0F)
            .ambientSound(SoundEvents.BEACON_AMBIENT.getLocation(), 0.5F, 1.3F, 90).build());

    /**
     * The id a trophy with no rank records (a mob trophy, when the server's config gives mob trophies none): it's shown
     * as a display pedestal is, without a rank's effects, frame or sounds.
     */
    public static final ResourceLocation NONE = TrophyPedestalsMod.id("none");

    /** Reads a rarity's id; a bare name ({@code "epic"}) is one of Trophy Pedestals's. Unknown ids read fine, and show as common. */
    public static final Codec<ResourceLocation> ID_CODEC = Codec.STRING.comapFlatMap(
            text -> {
                ResourceLocation id = parse(text);
                return id == null ? DataResult.error(() -> "Not a rarity id: " + text) : DataResult.success(id);
            },
            ResourceLocation::toString);

    private PedestalRarities() {}

    /**
     * Adds a rarity; call it once from common setup, on both sides.
     *
     * @throws IllegalStateException if one with its id is already there
     */
    public static PedestalRarity register(PedestalRarity rarity) {
        if (RARITIES.putIfAbsent(rarity.id(), rarity) != null) throw new IllegalStateException("Pedestal rarity " + rarity.id() + " is already registered");
        return rarity;
    }

    /** Whether this is the id of no rank ({@link #NONE}). */
    public static boolean isNone(ResourceLocation id) {
        return NONE.equals(id);
    }

    /** The rarity with this id, or null if there's none. */
    public static @Nullable PedestalRarity find(ResourceLocation id) {
        return RARITIES.get(id);
    }

    /** The rarity with this id, or {@link #COMMON} if there's none (a trophy from a removed mod's rarity, say). */
    public static PedestalRarity get(ResourceLocation id) {
        PedestalRarity rarity = RARITIES.get(id);
        return rarity == null ? COMMON : rarity;
    }

    /** Every rarity, by rank from lowest to highest (those of the same rank in the order they were added). */
    public static List<PedestalRarity> all() {
        synchronized (RARITIES) {
            return RARITIES.values().stream().sorted(Comparator.comparingInt(PedestalRarity::rank)).toList();
        }
    }

    /** A rarity id from text: {@code "epic"} is {@code trophypedestals:epic}; null if it isn't an id at all. */
    public static @Nullable ResourceLocation parse(String text) {
        return text.indexOf(':') < 0 ? ResourceLocation.tryBuild(TrophyPedestalsMod.MOD_ID, text) : ResourceLocation.tryParse(text);
    }
}
