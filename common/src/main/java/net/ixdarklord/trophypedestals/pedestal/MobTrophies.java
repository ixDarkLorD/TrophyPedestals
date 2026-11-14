package net.ixdarklord.trophypedestals.pedestal;
import net.ixdarklord.coolcatcore.api.event.v2.common.LivingEvents;
import net.ixdarklord.coolcatcore.api.event.v2.core.EventResult;
import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.api.TrophyEvents;
import net.ixdarklord.trophypedestals.compat.component.Components;
import net.ixdarklord.trophypedestals.compat.component.DataComponents;
import net.ixdarklord.trophypedestals.feat.TrophyLedger;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Mob trophies: a mob a player kills now and then drops a trophy pedestal showing it, if the server's config allows
 * ({@code mob_trophies.enabled}) and at its chance ({@code mob_trophies.chance}). Its rank ({@code mob_trophies.rank}) is by how
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
                // (Mods and scripts can change it or stop it: TrophyEvents.MOB_TROPHY_DROP.)
                if (trophy != null && source.getEntity() instanceof ServerPlayer player) {
                    TrophyEvents.Drop drop = new TrophyEvents.Drop(player, entity, trophy);
                    TrophyEvents.MOB_TROPHY_DROP.invoker().onDrop(drop);
                    trophy = drop.cancelled() ? null : drop.trophy();
                }
                if (trophy != null) {
                    MobTrophyRecord record = Components.get(trophy, Pedestals.MOB_TROPHY.get());
                    if (record != null && source.getEntity() instanceof ServerPlayer slayer) {
                        TrophyLedger.collect(slayer, record);
                        announce(slayer, trophy, record);
                    }
                    entity.spawnAtLocation(trophy);
                }
            }
            return EventResult.pass();
        });
    }

    /**
     * Tells the whole server, while {@code mob_trophies.announce_rare} is on, that a player got a legendary or gilded
     * mob trophy: a gold trophy cup, then the news in pale gold, the trophy's name in its rank's colour, its tooltip
     * shown on hover.
     */
    // The announcement's own words: the mod's pale gold (the player's name and the trophy's keep their colours).
    private static final int ANNOUNCE_COLOR = 0xFFE08A;
    // The trophy cup's font and its one character (a private use one), for the announcement's prefix.
    private static final ResourceLocation ICON_FONT = TrophyPedestalsMod.id("icons");
    private static final String TROPHY_GLYPH = "";

    public static void announce(ServerPlayer player, ItemStack trophy, MobTrophyRecord record) {
        if (!TrophyPedestalsConfig.ANNOUNCE_RARE.get() || !(record.gilded() || PedestalRarities.LEGENDARY.id().equals(record.rarity()))) return;
        // (The cup: one character of the mod's icon font, assets/trophypedestals/font/icons.json, drawn untinted.)
        Component cup = Component.literal(TROPHY_GLYPH).withStyle(style -> style.withFont(ICON_FONT).withColor(ChatFormatting.WHITE));
        player.level().getServer().getPlayerList().broadcastSystemMessage(Component.empty().append(cup).append(" ")
                .append(Component.translatable("trophypedestals.announce.rare", player.getDisplayName(), trophy.getDisplayName()).withStyle(style -> style.withColor(ANNOUNCE_COLOR))), false);
    }

    /**
     * The trophy this death drops, or null: only a mob killed by a player, while they're on, and at their chance. Any
     * mod's mobs count as vanilla's do: any living thing but a player or an armor stand, unless it's in
     * {@code trophypedestals:no_mob_trophy}.
     */
    public static @Nullable ItemStack roll(LivingEntity entity, DamageSource source, RandomSource random) {
        if (entity instanceof ServerPlayer victim) return rollPlayer(victim, source, random);
        if (!TrophyPedestalsConfig.MOB_TROPHIES.get() || !droppable(entity) || !(source.getEntity() instanceof ServerPlayer player)) return null;
        if (bossCollected(entity.getType(), player)) return null;
        if (random.nextDouble() >= chance(entity.getType())) return null;
        ResourceLocation rank = rank(entity, player);
        ItemStack trophy = trophy(Pedestals.DEFAULT.get(), entity, player.getName().getString(), System.currentTimeMillis(), rank);
        return random.nextDouble() < TrophyPedestalsConfig.GILDED_CHANCE.get() ? gild(trophy) : trophy;
    }

    /**
     * The trophy a player killed by another player drops, or null: only while the server's config turns player trophies
     * on ({@code mob_trophies.player_trophies}, off by default), at the mob trophy chance. It shows the player's statue in their skin
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
        for (EntityType<?> type : collectable(features)) output.accept(trophy(Pedestals.DEFAULT.get(), type, "", 0L, rarity(type)));
    }

    /**
     * Every mob that can be a trophy (mods' included) but a player or an armor stand, or any tagged to drop none, by rank
     * (common first, legendary last) and then by name: the creative tab's order, and the Mob Trophies Ledger's.
     */
    public static List<EntityType<?>> collectable(FeatureFlagSet features) {
        record Ranked(EntityType<?> type, PedestalRarity rarity, String name) {}
        List<Ranked> mobs = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == EntityType.PLAYER || type == EntityType.ARMOR_STAND || type.builtInRegistryHolder().is(NONE) || !type.isEnabled(features)) continue;
            PedestalRarity rarity = rarity(type);
            if (rarity != null) mobs.add(new Ranked(type, rarity, type.getDescription().getString()));
        }
        mobs.sort(Comparator.comparingInt((Ranked mob) -> mob.rarity().rank()).thenComparing(Ranked::name, String.CASE_INSENSITIVE_ORDER));
        return mobs.stream().<EntityType<?>>map(Ranked::type).toList();
    }

    /** Whether this can drop a trophy at all: any living thing but a player or an armor stand, unless tagged not to. */
    public static boolean droppable(LivingEntity entity) {
        return !(entity instanceof Player) && !(entity instanceof ArmorStand) && !entity.getType().builtInRegistryHolder().is(NONE);
    }

    /**
     * The chance this mob drops its trophy: the first {@code mob_trophies.chance_overrides} entry naming it by id, else the
     * first naming one of its tags, else the first naming its mod ({@code @mod id}), else {@code mob_trophies.chance}. An
     * entry that can't be read is logged and skipped.
     */
    public static double chance(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        Double byTag = null, byMod = null;
        for (String entry : TrophyPedestalsConfig.MOB_TROPHY_CHANCE_OVERRIDES.get()) {
            int split = entry.indexOf('=');
            Double chance = split < 0 ? null : probability(entry.substring(split + 1));
            String target = split < 0 ? "" : entry.substring(0, split).trim();
            if (chance == null || target.length() < 2) {
                if (WARNED_OVERRIDES.add(entry)) TrophyPedestalsMod.LOGGER.warn("Can't read the mob_trophies.chance_overrides entry \"{}\"; it needs \"<mob id, #tag or @mod id> = chance\" with a chance from 0 to 1", entry);
                continue;
            }
            if (target.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(target.substring(1));
                if (byTag == null && tag != null && type.builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE, tag))) byTag = chance;
            } else if (target.startsWith("@")) {
                if (byMod == null && id.getNamespace().equals(target.substring(1))) byMod = chance;
            } else if (id.equals(ResourceLocation.tryParse(target))) {
                return chance;
            }
        }
        if (byTag != null) return byTag;
        if (byMod != null) return byMod;
        return TrophyPedestalsConfig.MOB_TROPHY_CHANCE.get();
    }

    // "0.05" as a chance from 0 to 1; null if it isn't a number in that range.
    private static @Nullable Double probability(String text) {
        try {
            double chance = Double.parseDouble(text.trim());
            return chance >= 0.0 && chance <= 1.0 ? chance : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Whether this is a boss whose trophy the player has already collected, while {@code mob_trophies.boss_trophies_once}
     * is on: then it drops no more for them.
     */
    public static boolean bossCollected(EntityType<?> type, ServerPlayer player) {
        if (!TrophyPedestalsConfig.BOSS_TROPHIES_ONCE.get() || tagged(type) == null) return false;
        return TrophyLedger.COLLECTED.get(player).contains(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    /** The rank a trophy of this mob dropped for this player has, by the server's {@code mob_trophies.rank}. */
    public static ResourceLocation rank(LivingEntity victim, ServerPlayer killer) {
        return switch (TrophyPedestalsConfig.MOB_TROPHY_RARITY.get()) {
            case NONE -> PedestalRarities.NONE;
            case STRENGTH -> rarity(victim).id();
            case KILLS -> byKills(victim.getType(), killer.getStats().getValue(Stats.ENTITY_KILLED, victim.getType())).id();
        };
    }

    /** The rank for this many kills of a mob, by the server's {@code mob_trophies.kills} counts (the mob's own, if it has some). */
    public static PedestalRarity byKills(EntityType<?> type, int kills) {
        int[] counts = killCounts(type);
        if (kills >= counts[3]) return PedestalRarities.LEGENDARY;
        if (kills >= counts[2]) return PedestalRarities.EPIC;
        if (kills >= counts[1]) return PedestalRarities.RARE;
        if (kills >= counts[0]) return PedestalRarities.UNCOMMON;
        return PedestalRarities.COMMON;
    }

    /**
     * The kills a mob's trophy needs to be uncommon, rare, epic and legendary: the first {@code mob_trophies.kills.overrides}
     * entry naming it by id, else the first naming one of its tags, else the {@code mob_trophies.kills} counts. An entry
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
                if (WARNED_OVERRIDES.add(entry)) TrophyPedestalsMod.LOGGER.warn("Can't read the mob_trophies.kills.overrides entry \"{}\"; it needs \"<mob id or #tag> = uncommon, rare, epic, legendary\"", entry);
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

    // The least time between a mob trophy's sounds, in ticks.
    private static final int SOUND_COOLDOWN = 20;

    /**
     * A mob trophy's mob's sound, as it makes it: its ambient sound ({@code entity.<mob>.ambient}, as vanilla's and most
     * mods' mobs name theirs), else its hurt sound, at the pedestal; not more than once a second. Nothing for a mob with
     * neither, or a statue.
     */
    public static void playSound(Level level, BlockPos pos, PedestalBlockEntity pedestal) {
        MobTrophyRecord record = pedestal.mobTrophy();
        if (record == null || record.type() == Statues.FIGURE || level.getGameTime() - pedestal.lastSound() < SOUND_COOLDOWN) return;
        ResourceLocation id = record.entity();
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(new ResourceLocation(id.getNamespace(), "entity." + id.getPath() + ".ambient"))
                .or(() -> BuiltInRegistries.SOUND_EVENT.getOptional(new ResourceLocation(id.getNamespace(), "entity." + id.getPath() + ".hurt")))
                .orElse(null);
        if (sound == null) return;
        pedestal.markSound(level.getGameTime());
        // (Softer under glass.)
        level.playSound(null, pos, sound, SoundSource.BLOCKS, pedestal.glass() ? 0.4F : 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);
    }

    // How often a placed mob trophy counts its slayer's kills, in ticks (each pedestal on its own beat).
    private static final int GROW_CHECK = 200;

    /**
     * A placed mob trophy counting its slayer's kills of its mob (the game's own kill statistic, while they're online),
     * shown on it as "152 slain"; with the server's {@code mob_trophies.rank} {@code KILLS}, a ranked one goes up a rank as
     * the count reaches the next ({@link #byKills}), never down. Each pedestal counts every {@link #GROW_CHECK} ticks.
     */
    public static void grow(Level level, BlockPos pos, PedestalBlockEntity pedestal) {
        if ((level.getGameTime() + pos.asLong()) % GROW_CHECK != 0 || !(level instanceof ServerLevel server)) return;
        count(server, pos, pedestal);
    }

    /** A placed mob trophy counting its slayer's kills now, and going up a rank if it should (see {@link #grow}). */
    public static void count(ServerLevel server, BlockPos pos, PedestalBlockEntity pedestal) {
        MobTrophyRecord record = pedestal.mobTrophy();
        if (record == null || !record.slain()) return;
        ServerPlayer slayer = server.getServer().getPlayerList().getPlayerByName(record.slayer());
        if (slayer != null) count(server, pos, pedestal, slayer);
    }

    /** The same, counting this player's kills (its slayer's). */
    public static void count(ServerLevel server, BlockPos pos, PedestalBlockEntity pedestal, ServerPlayer slayer) {
        if (!TrophyPedestalsConfig.GROWING_TROPHIES.get()) return;
        Level level = server;
        MobTrophyRecord record = pedestal.mobTrophy();
        if (record == null || !record.slain()) return;
        EntityType<?> type = record.type();
        if (type == null) return;
        int kills = slayer.getStats().getValue(Stats.ENTITY_KILLED, type);
        if (kills == record.kills()) return;
        ResourceLocation rank = record.rarity();
        if (TrophyPedestalsConfig.MOB_TROPHY_RARITY.get() == TrophyPedestalsConfig.MobTrophyRarity.KILLS && !PedestalRarities.isNone(rank)) {
            PedestalRarity grown = byKills(type, kills);
            if (grown.rank() > PedestalRarities.get(rank).rank()) {
                rank = grown.id();
                level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.2F);
            }
        }
        pedestal.updateMobTrophy(record.withKills(kills, rank));
    }

    /**
     * A gilded mob trophy: the same, on a gold pedestal (a custom one, of gold blocks), named "Gilded Zombie". A rare
     * drop, at the server's {@code mob_trophies.gilded_chance}.
     */
    public static ItemStack gild(ItemStack trophy) {
        MobTrophyRecord record = Components.get(trophy, Pedestals.MOB_TROPHY.get());
        if (record == null) return trophy;
        ItemStack gilded = new ItemStack(Pedestals.CUSTOM_ITEM.get());
        Components.set(gilded, Pedestals.PEDESTAL_MATERIAL.get(), BuiltInRegistries.BLOCK.getKey(Blocks.GOLD_BLOCK));
        record(gilded, record.withGilded(true));
        return gilded;
    }

    /** Makes a pedestal item a mob trophy with this record, named after the mob ("Zombie Trophy") in its rank's color. */
    public static void record(ItemStack stack, MobTrophyRecord record) {
        Components.set(stack, Pedestals.MOB_TROPHY.get(), record);
        int color = PedestalRarities.isNone(record.rarity()) ? 0xFFFFFF : PedestalRarities.get(record.rarity()).color();
        Components.set(stack, DataComponents.ITEM_NAME, Component.translatable("trophypedestals.pedestal.trophy_name", record.displayName()).withStyle(style -> style.withColor(color)));
    }
}
