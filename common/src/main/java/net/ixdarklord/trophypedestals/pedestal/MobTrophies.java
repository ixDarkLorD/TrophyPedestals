package net.ixdarklord.trophypedestals.pedestal;
import net.ixdarklord.coolcatcore.api.event.v2.common.LivingEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.ServerLifecycleEvents;
import net.ixdarklord.coolcatcore.api.event.v2.core.EventResult;
import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.api.TrophyEvents;
import net.ixdarklord.trophypedestals.feat.TrophyLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.AtlasIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
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
import java.util.function.Predicate;

/**
 * Mob trophies: a mob a player kills now and then drops a trophy pedestal showing it, if the server's config allows
 * ({@code mob_trophies.enabled}) and at its chance ({@code mob_trophies.chance}).
 */
public final class MobTrophies {
    public static final TagKey<EntityType<?>> LEGENDARY = TagKey.create(Registries.ENTITY_TYPE, TrophyPedestalsMod.id("legendary_mob_trophies"));
    public static final TagKey<EntityType<?>> EPIC = TagKey.create(Registries.ENTITY_TYPE, TrophyPedestalsMod.id("epic_mob_trophies"));
    public static final TagKey<EntityType<?>> NONE = TagKey.create(Registries.ENTITY_TYPE, TrophyPedestalsMod.id("no_mob_trophy"));
    public static final TagKey<EntityType<?>> ADDED = TagKey.create(Registries.ENTITY_TYPE, TrophyPedestalsMod.id("mob_trophies"));
    public static final ResourceKey<Enchantment> TROPHY_HUNTER = ResourceKey.create(Registries.ENCHANTMENT, TrophyPedestalsMod.id("trophy_hunter"));

    private MobTrophies() {}

    private static @Nullable Boolean allMobs;

    public static void init() {
        ServerLifecycleEvents.STARTING.register(server -> applyAllMobs());
        LivingEvents.DEATH.register((entity, source) -> {
            if (entity.level() instanceof ServerLevel level) {
                ItemStack trophy = roll(entity, source, level.getRandom());
                if (trophy != null && source.getEntity() instanceof ServerPlayer player) {
                    TrophyEvents.Drop drop = new TrophyEvents.Drop(player, entity, trophy);
                    TrophyEvents.MOB_TROPHY_DROP.invoker().onDrop(drop);
                    trophy = drop.cancelled() ? null : drop.trophy();
                }
                if (trophy != null) {
                    MobTrophyRecord record = trophy.get(Pedestals.MOB_TROPHY.get());
                    if (record != null && source.getEntity() instanceof ServerPlayer slayer) {
                        TrophyLedger.collect(slayer, record);
                        announce(slayer, trophy, record);
                    }
                    entity.spawnAtLocation(level, trophy);
                }
            }
            return EventResult.pass();
        });
    }

    private static final int ANNOUNCE_COLOR = 0xFFE08A;

    public static void announce(ServerPlayer player, ItemStack trophy, MobTrophyRecord record) {
        if (!TrophyPedestalsConfig.ANNOUNCE_RARE.get() || !(record.gilded() || PedestalRarities.LEGENDARY.id().equals(record.rarity()))) return;
        Component cup = Component.object(new AtlasSprite(AtlasIds.ITEMS, TrophyPedestalsMod.id("item/trophy_icon")));
        player.level().getServer().getPlayerList().broadcastSystemMessage(Component.empty().append(cup).append(" ")
                .append(Component.translatable("trophypedestals.announce.rare", player.getDisplayName(), trophy.getDisplayName()).withColor(ANNOUNCE_COLOR)), false);
    }

    public static @Nullable ItemStack roll(LivingEntity entity, DamageSource source, RandomSource random) {
        if (entity instanceof ServerPlayer victim) return rollPlayer(victim, source, random);
        if (!TrophyPedestalsConfig.MOB_TROPHIES.get() || !droppable(entity) || !hasTrophy(entity.getType()) || !(source.getEntity() instanceof ServerPlayer player)) return null;
        if (bossCollected(entity.getType(), player)) return null;
        ItemStack weapon = weapon(source, player);
        if (TrophyPedestalsConfig.REQUIRE_ENCHANTMENT.get() && enchantment(player, TROPHY_HUNTER, weapon) == 0) return null;
        if (random.nextDouble() >= chance(entity.getType(), enchantment(player, Enchantments.LOOTING, weapon))) return null;
        Identifier rank = rank(entity, player);
        ItemStack trophy = trophy(Pedestals.DEFAULT.get(), entity, player.getName().getString(), System.currentTimeMillis(), rank);
        return random.nextDouble() < TrophyPedestalsConfig.GILDED_CHANCE.get() ? gild(trophy) : trophy;
    }

    public static @Nullable ItemStack rollPlayer(ServerPlayer victim, DamageSource source, RandomSource random) {
        if (!TrophyPedestalsConfig.PLAYER_TROPHIES.get() || !(source.getEntity() instanceof ServerPlayer killer) || killer == victim) return null;
        if (random.nextDouble() >= TrophyPedestalsConfig.MOB_TROPHY_CHANCE.get()) return null;
        Identifier rank = rank(victim, killer);
        return trophy(Pedestals.DEFAULT.get(), Statues.FIGURE, Statues.look(victim), killer.getName().getString(), System.currentTimeMillis(), rank);
    }

    public static void creativeTrophies(FeatureFlagSet features, Consumer<ItemStack> output) {
        for (EntityType<?> type : collectable(features)) output.accept(trophy(Pedestals.DEFAULT.get(), type, "", 0L, rarity(type)));
    }

    public static List<EntityType<?>> collectable(FeatureFlagSet features) {
        return collectable(features, type -> false);
    }

    public static List<EntityType<?>> collectable(FeatureFlagSet features, Predicate<EntityType<?>> also) {
        record Ranked(EntityType<?> type, PedestalRarity rarity, String name) {}
        List<Ranked> mobs = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == EntityTypes.PLAYER || type == EntityTypes.ARMOR_STAND || !type.isEnabled(features) || !(hasTrophy(type) || also.test(type))) continue;
            PedestalRarity rarity = rarity(type);
            if (rarity != null) mobs.add(new Ranked(type, rarity, type.getDescription().getString()));
        }
        mobs.sort(Comparator.comparingInt((Ranked mob) -> mob.rarity().rank()).thenComparing(Ranked::name, String.CASE_INSENSITIVE_ORDER));
        return mobs.stream().<EntityType<?>>map(Ranked::type).toList();
    }

    public static boolean droppable(LivingEntity entity) {
        return !(entity instanceof Player) && !(entity instanceof ArmorStand) && !entity.getType().builtInRegistryHolder().is(NONE);
    }

    public static boolean hasTrophy(EntityType<?> type) {
        if (type.builtInRegistryHolder().is(NONE)) return false;
        if (allMobs() || type.builtInRegistryHolder().is(ADDED)) return true;
        if (Identifier.DEFAULT_NAMESPACE.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())) return true;
        Double override = chanceOverride(type);
        return override != null && override > 0.0;
    }

    public static boolean allMobs() {
        Boolean value = allMobs;
        if (value == null) allMobs = value = TrophyPedestalsConfig.ALL_MOBS.get();
        return value;
    }

    public static void applyAllMobs() {
        allMobs = TrophyPedestalsConfig.ALL_MOBS.get();
    }

    public static void forgetAllMobs() {
        allMobs = null;
    }

    public static double chance(EntityType<?> type, int looting) {
        return Math.min(1.0, chance(type) * (1.0 + TrophyPedestalsConfig.LOOTING_BONUS.get() * looting));
    }

    public static ItemStack weapon(DamageSource source, ServerPlayer player) {
        ItemStack weapon = source.getWeaponItem();
        return weapon != null && !weapon.isEmpty() ? weapon : player.getMainHandItem();
    }

    public static int enchantment(ServerPlayer player, ResourceKey<Enchantment> enchantment, ItemStack weapon) {
        return player.level().registryAccess().get(enchantment).map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, weapon)).orElse(0);
    }

    public static double chance(EntityType<?> type) {
        Double override = chanceOverride(type);
        if (override != null) return override;
        return tagged(type) != null ? TrophyPedestalsConfig.BOSS_TROPHY_CHANCE.get() : TrophyPedestalsConfig.MOB_TROPHY_CHANCE.get();
    }

    public static @Nullable Double chanceOverride(EntityType<?> type) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
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
                Identifier tag = Identifier.tryParse(target.substring(1));
                if (byTag == null && tag != null && type.builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE, tag))) byTag = chance;
            } else if (target.startsWith("@")) {
                if (byMod == null && id.getNamespace().equals(target.substring(1))) byMod = chance;
            } else if (id.equals(Identifier.tryParse(target))) {
                return chance;
            }
        }
        return byTag != null ? byTag : byMod;
    }

    private static @Nullable Double probability(String text) {
        try {
            double chance = Double.parseDouble(text.trim());
            return chance >= 0.0 && chance <= 1.0 ? chance : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean bossCollected(EntityType<?> type, ServerPlayer player) {
        if (!TrophyPedestalsConfig.BOSS_TROPHIES_ONCE.get() || tagged(type) == null) return false;
        return TrophyLedger.COLLECTED.get(player).contains(BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    public static Identifier rank(LivingEntity victim, ServerPlayer killer) {
        return switch (TrophyPedestalsConfig.MOB_TROPHY_RARITY.get()) {
            case NONE -> PedestalRarities.NONE;
            case STRENGTH -> rarity(victim).id();
            case KILLS -> byKills(victim.getType(), killer.getStats().getValue(Stats.ENTITY_KILLED, victim.getType())).id();
        };
    }

    public static PedestalRarity byKills(EntityType<?> type, int kills) {
        int[] counts = killCounts(type);
        if (kills >= counts[3]) return PedestalRarities.LEGENDARY;
        if (kills >= counts[2]) return PedestalRarities.EPIC;
        if (kills >= counts[1]) return PedestalRarities.RARE;
        if (kills >= counts[0]) return PedestalRarities.UNCOMMON;
        return PedestalRarities.COMMON;
    }

    public static int[] killCounts(EntityType<?> type) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
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
                Identifier tag = Identifier.tryParse(target.substring(1));
                if (byTag == null && tag != null && type.builtInRegistryHolder().is(TagKey.create(Registries.ENTITY_TYPE, tag))) byTag = counts;
            } else if (id.equals(Identifier.tryParse(target))) {
                return counts;
            }
        }
        if (byTag != null) return byTag;
        return new int[]{TrophyPedestalsConfig.KILLS_UNCOMMON.get(), TrophyPedestalsConfig.KILLS_RARE.get(), TrophyPedestalsConfig.KILLS_EPIC.get(), TrophyPedestalsConfig.KILLS_LEGENDARY.get()};
    }

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

    public static PedestalRarity rarity(LivingEntity entity) {
        PedestalRarity tagged = tagged(entity.getType());
        if (tagged != null) return tagged;
        return byStrength(strength(entity));
    }

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

    public static double strength(LivingEntity entity) {
        double attack = entity.getAttribute(Attributes.ATTACK_DAMAGE) == null ? 0.0 : entity.getAttributeValue(Attributes.ATTACK_DAMAGE);
        return entity.getMaxHealth() + 4.0 * attack;
    }

    public static PedestalRarity rarity(EntityType<?> type, ServerLevel level) {
        PedestalRarity tagged = tagged(type);
        if (tagged != null) return tagged;
        return type.create(level, EntitySpawnReason.LOAD) instanceof LivingEntity mob ? rarity(mob) : PedestalRarities.COMMON;
    }

    public static @Nullable PedestalRarity tagged(EntityType<?> type) {
        if (type.builtInRegistryHolder().is(LEGENDARY)) return PedestalRarities.LEGENDARY;
        if (type.builtInRegistryHolder().is(EPIC)) return PedestalRarities.EPIC;
        return null;
    }

    public static ItemStack trophy(Block pedestal, EntityType<?> type, String slayer, long slainAt, PedestalRarity rarity) {
        return trophy(pedestal, type, slayer, slainAt, rarity.id());
    }

    public static ItemStack trophy(Block pedestal, EntityType<?> type, String slayer, long slainAt, Identifier rarity) {
        ItemStack stack = new ItemStack(pedestal);
        record(stack, new MobTrophyRecord(BuiltInRegistries.ENTITY_TYPE.getKey(type), slayer, slainAt, rarity));
        return stack;
    }

    public static ItemStack trophy(Block pedestal, LivingEntity mob, String slayer, long slainAt, Identifier rarity) {
        return trophy(pedestal, mob.getType(), MobLooks.capture(mob), slayer, slainAt, rarity);
    }

    public static ItemStack trophy(Block pedestal, EntityType<?> type, CompoundTag look, String slayer, long slainAt, Identifier rarity) {
        ItemStack stack = new ItemStack(pedestal);
        record(stack, new MobTrophyRecord(BuiltInRegistries.ENTITY_TYPE.getKey(type), slayer, slainAt, rarity, look));
        return stack;
    }

    private static final int SOUND_COOLDOWN = 20;

    public static void playSound(Level level, BlockPos pos, PedestalBlockEntity pedestal) {
        MobTrophyRecord record = pedestal.mobTrophy();
        if (record == null || record.type() == Statues.FIGURE || level.getGameTime() - pedestal.lastSound() < SOUND_COOLDOWN) return;
        Identifier id = record.entity();
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.fromNamespaceAndPath(id.getNamespace(), "entity." + id.getPath() + ".ambient"))
                .or(() -> BuiltInRegistries.SOUND_EVENT.getOptional(Identifier.fromNamespaceAndPath(id.getNamespace(), "entity." + id.getPath() + ".hurt")))
                .orElse(null);
        if (sound == null) return;
        pedestal.markSound(level.getGameTime());
        level.playSound(null, pos, sound, SoundSource.BLOCKS, pedestal.glass() ? 0.4F : 0.8F, 0.9F + level.getRandom().nextFloat() * 0.2F);
    }

    private static final int GROW_CHECK = 200;

    public static void grow(Level level, BlockPos pos, PedestalBlockEntity pedestal) {
        if ((level.getGameTime() + pos.asLong()) % GROW_CHECK != 0 || !(level instanceof ServerLevel server)) return;
        count(server, pos, pedestal);
    }

    public static void count(ServerLevel server, BlockPos pos, PedestalBlockEntity pedestal) {
        MobTrophyRecord record = pedestal.mobTrophy();
        if (record == null || !record.slain()) return;
        ServerPlayer slayer = server.getServer().getPlayerList().getPlayerByName(record.slayer());
        if (slayer != null) count(server, pos, pedestal, slayer);
    }

    public static void count(ServerLevel server, BlockPos pos, PedestalBlockEntity pedestal, ServerPlayer slayer) {
        if (!TrophyPedestalsConfig.GROWING_TROPHIES.get()) return;
        Level level = server;
        MobTrophyRecord record = pedestal.mobTrophy();
        if (record == null || !record.slain()) return;
        EntityType<?> type = record.type();
        if (type == null) return;
        int kills = slayer.getStats().getValue(Stats.ENTITY_KILLED, type);
        if (kills == record.kills()) return;
        Identifier rank = record.rarity();
        if (TrophyPedestalsConfig.MOB_TROPHY_RARITY.get() == TrophyPedestalsConfig.MobTrophyRarity.KILLS && !PedestalRarities.isNone(rank)) {
            PedestalRarity grown = byKills(type, kills);
            if (grown.rank() > PedestalRarities.get(rank).rank()) {
                rank = grown.id();
                level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6F, 1.2F);
            }
        }
        pedestal.updateMobTrophy(record.withKills(kills, rank));
    }

    public static ItemStack gild(ItemStack trophy) {
        MobTrophyRecord record = trophy.get(Pedestals.MOB_TROPHY.get());
        if (record == null) return trophy;
        ItemStack gilded = new ItemStack(Pedestals.CUSTOM_ITEM.get());
        gilded.set(Pedestals.PEDESTAL_MATERIAL.get(), BuiltInRegistries.BLOCK.getKey(Blocks.GOLD_BLOCK));
        record(gilded, record.withGilded(true));
        return gilded;
    }

    public static void record(ItemStack stack, MobTrophyRecord record) {
        stack.set(Pedestals.MOB_TROPHY.get(), record);
        int color = PedestalRarities.isNone(record.rarity()) ? 0xFFFFFF : PedestalRarities.get(record.rarity()).color();
        stack.set(DataComponents.ITEM_NAME, Component.translatable("trophypedestals.pedestal.trophy_name", record.displayName()).withColor(color));
    }
}
