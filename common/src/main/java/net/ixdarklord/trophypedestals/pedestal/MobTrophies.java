package net.ixdarklord.trophypedestals.pedestal;
import net.ixdarklord.coolcatcore.api.event.v2.common.LivingEvents;
import net.ixdarklord.coolcatcore.api.event.v2.core.EventResult;
import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Mob trophies: a mob a player kills now and then drops a trophy pedestal showing it, if the server's config allows
 * ({@code mob_trophies}) and at its chance ({@code mob_trophy_chance}). Its rank ({@code mob_trophy_rarity}) is by how
 * tough the mob is, or none at all (shown as a display pedestal is). By toughness: a mob in the
 * {@code trophypedestals:legendary_mob_trophies} entity type tag (the ender dragon and the wither) gives a legendary one, in
 * {@code trophypedestals:epic_mob_trophies} (the warden and the elder guardian) an epic one, and any other one by its strength,
 * its health plus four times its attack: under 25 common (a chicken, a creeper), under 50 uncommon (a zombie, a blaze),
 * under 120 rare (an enderman, a piglin brute), under 250 epic (a ravager, an iron golem), and from there legendary.
 * Data packs can add to either tag. Mods' mobs work as vanilla's do (opt one out with {@code trophypedestals:no_mob_trophy}).
 * A trophy keeps how its mob looked ({@link MobLooks}): its variant, whether it was a baby, its gear, its name.
 */
public final class MobTrophies {
    public static final TagKey<EntityType<?>> LEGENDARY = TagKey.create(Registries.ENTITY_TYPE, TrophyPedestalsMod.id("legendary_mob_trophies"));
    public static final TagKey<EntityType<?>> EPIC = TagKey.create(Registries.ENTITY_TYPE, TrophyPedestalsMod.id("epic_mob_trophies"));
    /** Mobs that never drop a trophy (for a mod's mob that shouldn't, say). */
    public static final TagKey<EntityType<?>> NONE = TagKey.create(Registries.ENTITY_TYPE, TrophyPedestalsMod.id("no_mob_trophy"));

    private MobTrophies() {}

    public static void init() {
        LivingEvents.DEATH.register((entity, source) -> {
            if (entity.level() instanceof ServerLevel level) {
                ItemStack trophy = roll(entity, source, level.getRandom());
                if (trophy != null) entity.spawnAtLocation(trophy);
            }
            return EventResult.pass();
        });
    }

    /**
     * The trophy this death drops, or null: only a mob killed by a player, while they're on, and at their chance. Any
     * mod's mobs count as vanilla's do: any living thing but a player or an armor stand, unless it's in
     * {@code trophypedestals:no_mob_trophy}.
     */
    public static @Nullable ItemStack roll(LivingEntity entity, DamageSource source, RandomSource random) {
        if (entity instanceof ServerPlayer victim) return rollPlayer(victim, source, random);
        if (!TrophyPedestalsConfig.MOB_TROPHIES.get() || !droppable(entity) || !(source.getEntity() instanceof ServerPlayer player)) return null;
        if (random.nextDouble() >= TrophyPedestalsConfig.MOB_TROPHY_CHANCE.get()) return null;
        ResourceLocation rank = rank(entity, player);
        return trophy(Pedestals.DEFAULT.get(), entity, player.getName().getString(), System.currentTimeMillis(), rank);
    }

    /**
     * The trophy a player killed by another player drops, or null: only while the server's config turns player trophies
     * on ({@code player_trophies}, off by default), at the mob trophy chance. It shows the player's statue in their skin
     * ({@link Statues}), slain by their killer, ranked as mob trophies are.
     */
    public static @Nullable ItemStack rollPlayer(ServerPlayer victim, DamageSource source, RandomSource random) {
        if (!TrophyPedestalsConfig.PLAYER_TROPHIES.get() || !(source.getEntity() instanceof ServerPlayer killer) || killer == victim) return null;
        if (random.nextDouble() >= TrophyPedestalsConfig.MOB_TROPHY_CHANCE.get()) return null;
        ResourceLocation rank = rank(victim, killer);
        return trophy(Pedestals.DEFAULT.get(), Statues.FIGURE, Statues.look(victim), killer.getName().getString(), System.currentTimeMillis(), rank);
    }

    /**
     * A trophy of every living mob in the game (mods' included) but a player or an armor stand, or any tagged to drop
     * none, by rank (common first, legendary last) and then by name: for the creative tab. Each is on the default
     * pedestal, ranked by its toughness, and slain by no one.
     */
    public static void creativeTrophies(FeatureFlagSet features, Consumer<ItemStack> output) {
        record Ranked(EntityType<?> type, PedestalRarity rarity, String name) {}
        List<Ranked> mobs = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == EntityType.PLAYER || type == EntityType.ARMOR_STAND || type.builtInRegistryHolder().is(NONE) || !type.isEnabled(features)) continue;
            PedestalRarity rarity = rarity(type);
            if (rarity != null) mobs.add(new Ranked(type, rarity, type.getDescription().getString()));
        }
        mobs.sort(Comparator.comparingInt((Ranked mob) -> mob.rarity().rank()).thenComparing(Ranked::name, String.CASE_INSENSITIVE_ORDER));
        for (Ranked mob : mobs) output.accept(trophy(Pedestals.DEFAULT.get(), mob.type(), "", 0L, mob.rarity()));
    }

    /** Whether this can drop a trophy at all: any living thing but a player or an armor stand, unless tagged not to. */
    public static boolean droppable(LivingEntity entity) {
        return !(entity instanceof Player) && !(entity instanceof ArmorStand) && !entity.getType().builtInRegistryHolder().is(NONE);
    }

    /** The rank a trophy of this mob dropped for this player has, by the server's {@code mob_trophy_rarity}. */
    public static ResourceLocation rank(LivingEntity victim, ServerPlayer killer) {
        return switch (TrophyPedestalsConfig.MOB_TROPHY_RARITY.get()) {
            case NONE -> PedestalRarities.NONE;
            case STRENGTH -> rarity(victim).id();
            case KILLS -> byKills(victim.getType(), killer.getStats().getValue(Stats.ENTITY_KILLED, victim.getType())).id();
        };
    }

    /** The rank for this many kills of a mob, by the server's {@code mob_trophy_kills_*} counts (the mob's own, if it has some). */
    public static PedestalRarity byKills(EntityType<?> type, int kills) {
        int[] counts = killCounts(type);
        if (kills >= counts[3]) return PedestalRarities.LEGENDARY;
        if (kills >= counts[2]) return PedestalRarities.EPIC;
        if (kills >= counts[1]) return PedestalRarities.RARE;
        if (kills >= counts[0]) return PedestalRarities.UNCOMMON;
        return PedestalRarities.COMMON;
    }

    /**
     * The kills a mob's trophy needs to be uncommon, rare, epic and legendary: the first {@code mob_trophy_kills_overrides}
     * entry naming it by id, else the first naming one of its tags, else the {@code mob_trophy_kills_*} counts. An entry
     * that can't be read is logged and skipped.
     */
    public static int[] killCounts(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        int[] byTag = null;
        for (String entry : TrophyPedestalsConfig.KILLS_OVERRIDES.get()) {
            int split = entry.indexOf('=');
            int[] counts = split < 0 ? null : counts(entry.substring(split + 1));
            String target = split < 0 ? "" : entry.substring(0, split).trim();
            if (counts == null || target.isEmpty()) {
                if (WARNED_OVERRIDES.add(entry)) TrophyPedestalsMod.LOGGER.warn("Can't read the mob_trophy_kills_overrides entry \"{}\"; it needs \"<mob id or #tag> = uncommon, rare, epic, legendary\"", entry);
                continue;
            }
            if (target.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(target.substring(1));
                if (byTag == null && tag != null && type.builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE, tag))) byTag = counts;
            } else if (id.equals(ResourceLocation.tryParse(target))) {
                return counts;
            }
        }
        if (byTag != null) return byTag;
        return new int[]{TrophyPedestalsConfig.KILLS_UNCOMMON.get(), TrophyPedestalsConfig.KILLS_RARE.get(), TrophyPedestalsConfig.KILLS_EPIC.get(), TrophyPedestalsConfig.KILLS_LEGENDARY.get()};
    }

    // "1, 2, 3, 5" as four counts, each at least 1; null if it isn't four whole numbers.
    private static int @Nullable [] counts(String text) {
        String[] parts = text.split(",");
        if (parts.length != 4) return null;
        int[] counts = new int[4];
        try {
            for (int i = 0; i < 4; i++) counts[i] = Math.max(1, Integer.parseInt(parts[i].trim()));
        } catch (NumberFormatException e) {
            return null;
        }
        return counts;
    }

    private static final Set<String> WARNED_OVERRIDES = ConcurrentHashMap.newKeySet();

    /** The rank a mob's trophy has by its toughness: from the boss tags, or its strength. */
    public static PedestalRarity rarity(LivingEntity entity) {
        PedestalRarity tagged = tagged(entity.getType());
        if (tagged != null) return tagged;
        return byStrength(strength(entity));
    }

    /**
     * The rank a kind of mob's trophy has by its toughness, from its default attributes (for where there's no mob or
     * level, as in the creative tab): from the boss tags, or its base strength; null if it isn't a living mob.
     */
    public static @Nullable PedestalRarity rarity(EntityType<?> type) {
        if (!DefaultAttributes.hasSupplier(type)) return null;
        PedestalRarity tagged = tagged(type);
        if (tagged != null) return tagged;
        @SuppressWarnings("unchecked")
        AttributeSupplier attributes = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
        double attack = attributes.hasAttribute(Attributes.ATTACK_DAMAGE) ? attributes.getValue(Attributes.ATTACK_DAMAGE) : 0.0;
        return byStrength(attributes.getValue(Attributes.MAX_HEALTH) + 4.0 * attack);
    }

    private static PedestalRarity byStrength(double strength) {
        if (strength < 25.0) return PedestalRarities.COMMON;
        if (strength < 50.0) return PedestalRarities.UNCOMMON;
        if (strength < 120.0) return PedestalRarities.RARE;
        if (strength < 250.0) return PedestalRarities.EPIC;
        return PedestalRarities.LEGENDARY;
    }

    /** A mob's strength, for its trophy's rank: its health, plus four times its attack if it has one. */
    public static double strength(LivingEntity entity) {
        double attack = entity.getAttribute(Attributes.ATTACK_DAMAGE) == null ? 0.0 : entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
        return entity.getMaxHealth() + 4.0 * attack;
    }

    /**
     * The rank a kind of mob's trophy has by its toughness, for where there's no mob to measure (a pack's or a command's
     * trophy): from the boss tags, or a freshly made one's strength; common for one that can't be made, or isn't alive.
     */
    public static PedestalRarity rarity(EntityType<?> type, ServerLevel level) {
        PedestalRarity tagged = tagged(type);
        if (tagged != null) return tagged;
        return type.create(level) instanceof LivingEntity mob ? rarity(mob) : PedestalRarities.COMMON;
    }

    /** The rank a boss tag gives a mob's trophy, or null if it's in neither. */
    public static @Nullable PedestalRarity tagged(EntityType<?> type) {
        if (type.builtInRegistryHolder().is(LEGENDARY)) return PedestalRarities.LEGENDARY;
        if (type.builtInRegistryHolder().is(EPIC)) return PedestalRarities.EPIC;
        return null;
    }

    /** A pedestal of this block showing this mob as it spawns, slain by {@code slayer} at {@code slainAt}. */
    public static ItemStack trophy(Block pedestal, EntityType<?> type, String slayer, long slainAt, PedestalRarity rarity) {
        return trophy(pedestal, type, slayer, slainAt, rarity.id());
    }

    /** The same, by its rank's id ({@link PedestalRarities#NONE} for none). */
    public static ItemStack trophy(Block pedestal, EntityType<?> type, String slayer, long slainAt, ResourceLocation rarity) {
        ItemStack stack = new ItemStack(pedestal);
        record(stack, new MobTrophyRecord(BuiltInRegistries.ENTITY_TYPE.getKey(type), slayer, slainAt, rarity));
        return stack;
    }

    /**
     * A pedestal of this block showing this mob as it looks ({@link MobLooks}: its variant, gear, name and the rest),
     * slain by {@code slayer} at {@code slainAt}, of this rank ({@link PedestalRarities#NONE} for none).
     */
    public static ItemStack trophy(Block pedestal, LivingEntity mob, String slayer, long slainAt, ResourceLocation rarity) {
        return trophy(pedestal, mob.getType(), MobLooks.capture(mob), slayer, slainAt, rarity);
    }

    /** A pedestal of this block showing this mob with this look (in the mob's own saved-data format; empty for none). */
    public static ItemStack trophy(Block pedestal, EntityType<?> type, CompoundTag look, String slayer, long slainAt, ResourceLocation rarity) {
        ItemStack stack = new ItemStack(pedestal);
        record(stack, new MobTrophyRecord(BuiltInRegistries.ENTITY_TYPE.getKey(type), slayer, slainAt, rarity, look));
        return stack;
    }

    /** Makes a pedestal item a mob trophy with this record, named after the mob ("Zombie Trophy") in its rank's color. */
    public static void record(ItemStack stack, MobTrophyRecord record) {
        stack.set(Pedestals.MOB_TROPHY.get(), record);
        int color = PedestalRarities.isNone(record.rarity()) ? 0xFFFFFF : PedestalRarities.get(record.rarity()).color();
        stack.set(DataComponents.ITEM_NAME, Component.translatable("trophypedestals.pedestal.trophy_name", record.mobName()).withColor(color));
    }
}
