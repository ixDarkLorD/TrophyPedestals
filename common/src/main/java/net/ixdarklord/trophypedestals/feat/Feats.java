package net.ixdarklord.trophypedestals.feat;

import net.ixdarklord.coolcatcore.api.attachment.Attachment;
import net.ixdarklord.coolcatcore.api.attachment.AttachmentRegistry;
import net.ixdarklord.coolcatcore.api.event.v2.common.LivingEvents;
import net.ixdarklord.coolcatcore.api.event.v2.common.PlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.core.EventResult;
import net.ixdarklord.coolcatcore.api.registry.ReloadListeners;
import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.api.TrophyKind;
import net.ixdarklord.trophypedestals.api.TrophyKinds;
import net.ixdarklord.trophypedestals.pedestal.MobTrophies;
import net.ixdarklord.trophypedestals.pedestal.MobTrophyRecord;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlock;
import net.ixdarklord.trophypedestals.pedestal.PedestalRecord;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.StatType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Trophies players earn: data packs' feats ({@link Feat}, read by {@link FeatLoader}) and, if the server turns them on
 * ({@code earned_trophies.advancements}, off by default), a trophy for each advancement of the kinds it names. Each is given straight to the
 * player and, unless the server turns it off, announced to them.
 * A feat given once per player is remembered on the player ({@link #GRANTED}); a first kill of everyone on the server, on
 * the server ({@link #FIRST_KILLS}).
 */
public final class Feats {
    /** The kind of a data pack's trophy; its subject is the feat's id, and it carries its own title and icon. */
    public static final Identifier FEAT = TrophyPedestalsMod.id("feat");
    /** The kind of an advancement's trophy; its subject is the advancement's id, and it carries its title and icon. */
    public static final Identifier ADVANCEMENT = TrophyPedestalsMod.id("advancement");

    static final AttachmentRegistry ATTACHMENTS = AttachmentRegistry.create(TrophyPedestalsMod.MOD_ID);
    /** The feats a player has had the trophy of (those given once), and the advancements whose trophy they have. */
    public static final Attachment<List<Identifier>> GRANTED = ATTACHMENTS.builder("earned_trophies", Identifier.CODEC.listOf(), List.<Identifier>of())
            .copyOnDeath()
            .build();
    public static final Attachment<List<Identifier>> GRANTED_ADVANCEMENTS = ATTACHMENTS.builder("advancement_trophies", Identifier.CODEC.listOf(), List.<Identifier>of())
            .copyOnDeath()
            .build();
    /** The first kills of everyone on the server already taken. */
    public static final Attachment<List<Identifier>> FIRST_KILLS = ATTACHMENTS.builder("first_kills", Identifier.CODEC.listOf(), List.<Identifier>of()).build();

    // How often, in ticks, each player's statistics are checked against the milestones.
    private static final int STAT_CHECK = 100;

    private static volatile Map<Identifier, Feat> feats = Map.of();

    private Feats() {}

    public static void init() {
        TrophyLedger.init();
        ATTACHMENTS.register();
        ReloadListeners.registerServer(FeatLoader.ID, new FeatLoader());
        TrophyKinds.register(FEAT, new TitledKind());
        TrophyKinds.register(ADVANCEMENT, new TitledKind());
        PlayerEvents.ADVANCEMENT_EARNED.register(Feats::onAdvancement);
        PlayerEvents.JOIN.register(Feats::onJoin);
        PlayerEvents.END_TICK.register(player -> {
            if (player instanceof ServerPlayer server && server.tickCount % STAT_CHECK == 0) checkStats(server);
        });
        LivingEvents.DEATH.register((entity, source) -> {
            if (source.getEntity() instanceof ServerPlayer player) onKill(player, entity);
            return EventResult.pass();
        });
    }

    /** The feats read last. */
    public static Map<Identifier, Feat> all() {
        return feats;
    }

    /** Replaces the feats read last, for the game tests. */
    public static void loadForTests(Map<Identifier, Feat> loaded) {
        load(loaded);
    }

    static void load(Map<Identifier, Feat> loaded) {
        feats = Map.copyOf(loaded);
        TrophyPedestalsMod.LOGGER.info("Loaded {} earned trophies", loaded.size());
    }

    // --- Triggers ---

    /** An advancement earned: the feats for it, else its own trophy if the server gives one for its kind. */
    public static void onAdvancement(ServerPlayer player, AdvancementHolder advancement) {
        boolean featFor = false;
        if (TrophyPedestalsConfig.FEAT_TROPHIES.get()) {
            for (Map.Entry<Identifier, Feat> entry : feats.entrySet()) {
                if (entry.getValue().trigger() instanceof Feat.AdvancementTrigger trigger && trigger.advancement().equals(advancement.id())) {
                    featFor = true;
                    grant(player, entry.getKey(), entry.getValue(), advancement);
                }
            }
        }
        if (!featFor) grantAdvancement(player, advancement);
    }

    /** A player's statistics against the milestones they haven't reached. */
    public static void checkStats(ServerPlayer player) {
        if (!TrophyPedestalsConfig.FEAT_TROPHIES.get() || feats.isEmpty()) return;
        List<Identifier> granted = GRANTED.get(player);
        for (Map.Entry<Identifier, Feat> entry : feats.entrySet()) {
            if (!(entry.getValue().trigger() instanceof Feat.StatTrigger trigger) || granted.contains(entry.getKey())) continue;
            Stat<?> stat = stat(trigger);
            if (stat != null && player.getStats().getValue(stat) >= trigger.atLeast()) grant(player, entry.getKey(), entry.getValue(), null);
        }
    }

    /** A mob killed by a player: the first kills it's first for. */
    public static void onKill(ServerPlayer player, LivingEntity victim) {
        if (!TrophyPedestalsConfig.FEAT_TROPHIES.get() || !MobTrophies.droppable(victim)) return;
        MinecraftServer server = player.level().getServer();
        for (Map.Entry<Identifier, Feat> entry : feats.entrySet()) {
            if (!(entry.getValue().trigger() instanceof Feat.FirstKillTrigger trigger) || !trigger.entity().matches(victim.getType())) continue;
            Feat feat = entry.getValue();
            if (trigger.scope() == Feat.Scope.SERVER) {
                if (FIRST_KILLS.get(server).contains(entry.getKey())) continue;
                FIRST_KILLS.update(server, taken -> append(taken, entry.getKey()));
            } else if (GRANTED.get(player).contains(entry.getKey())) {
                // (A player's own first kill: once each.)
                continue;
            }
            Component title = feat.title().orElseGet(() -> Component.translatable("trophypedestals.feat.first_kill", victim.getType().getDescription()));
            ItemStack trophy = MobTrophies.trophy(pedestal(feat), victim, player.getName().getString(), System.currentTimeMillis(), feat.rank());
            MobTrophyRecord record = trophy.get(Pedestals.MOB_TROPHY.get());
            if (record != null) {
                MobTrophies.record(trophy, record.withTitle(Optional.of(title)));
                TrophyLedger.collect(player, record);
            }
            give(player, entry.getKey(), feat, trophy);
        }
    }

    // A player joining: the advancement feats they've already earned (added since, or before the mod was installed).
    private static void onJoin(ServerPlayer player) {
        if (!TrophyPedestalsConfig.FEAT_TROPHIES.get()) return;
        MinecraftServer server = player.level().getServer();
        List<Identifier> granted = GRANTED.get(player);
        for (Map.Entry<Identifier, Feat> entry : feats.entrySet()) {
            if (!(entry.getValue().trigger() instanceof Feat.AdvancementTrigger trigger) || granted.contains(entry.getKey())) continue;
            AdvancementHolder advancement = server.getAdvancements().get(trigger.advancement());
            if (advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone()) grant(player, entry.getKey(), entry.getValue(), advancement);
        }
        checkStats(player);
    }

    // --- Giving ---

    // A feat's trophy for this player (an advancement feat's titled and shown as the advancement, unless it says otherwise).
    private static void grant(ServerPlayer player, Identifier id, Feat feat, @Nullable AdvancementHolder advancement) {
        if (feat.once() && GRANTED.get(player).contains(id)) return;
        DisplayInfo display = advancement == null ? null : advancement.value().display().orElse(null);
        Component title = feat.title().orElseGet(() -> display != null ? display.getTitle()
                : Component.translatableWithFallback("trophypedestals.feat." + id.getNamespace() + "." + id.getPath().replace('/', '.'), id.getPath()));
        Identifier icon = feat.icon().orElseGet(() -> display != null ? display.getIcon().item().unwrapKey().map(key -> key.identifier()).orElse(DEFAULT_ICON) : DEFAULT_ICON);
        give(player, id, feat, record(player, FEAT, id, feat, title, icon));
    }

    private static final Identifier DEFAULT_ICON = BuiltInRegistries.ITEM.getKey(Items.GOLD_INGOT);

    // An advancement's own trophy, if the server gives one for its kind and the player hasn't had it.
    private static void grantAdvancement(ServerPlayer player, AdvancementHolder advancement) {
        DisplayInfo display = advancement.value().display().orElse(null);
        if (display == null || !gives(display.getType()) || GRANTED_ADVANCEMENTS.get(player).contains(advancement.id())) return;
        PedestalRarity rank = switch (display.getType()) {
            case TASK -> PedestalRarities.UNCOMMON;
            case GOAL -> PedestalRarities.RARE;
            case CHALLENGE -> PedestalRarities.EPIC;
        };
        Identifier icon = display.getIcon().item().unwrapKey().map(key -> key.identifier()).orElse(DEFAULT_ICON);
        ItemStack trophy = new ItemStack(Pedestals.DEFAULT.get());
        Pedestals.record(trophy, new PedestalRecord(ADVANCEMENT, advancement.id(), PedestalRecord.Unlocker.PLAYER, player.getName().getString(),
                System.currentTimeMillis(), rank.id(), Optional.of(display.getTitle()), Optional.of(icon)));
        GRANTED_ADVANCEMENTS.update(player, list -> append(list, advancement.id()));
        giveAndAnnounce(player, trophy);
    }

    // Whether the server gives a trophy for advancements of this frame.
    private static boolean gives(AdvancementType type) {
        return switch (TrophyPedestalsConfig.ADVANCEMENT_TROPHIES.get()) {
            case NONE -> false;
            case CHALLENGES -> type == AdvancementType.CHALLENGE;
            case GOALS -> type != AdvancementType.TASK;
            case ALL -> true;
        };
    }

    // A feat's trophy recording its title and icon, for this player, now.
    private static ItemStack record(ServerPlayer player, Identifier kind, Identifier id, Feat feat, Component title, Identifier icon) {
        ItemStack trophy = new ItemStack(pedestal(feat));
        Pedestals.record(trophy, new PedestalRecord(kind, id, PedestalRecord.Unlocker.PLAYER, player.getName().getString(),
                System.currentTimeMillis(), feat.rank(), Optional.of(title), Optional.of(icon)));
        return trophy;
    }

    // Gives the player a feat's trophy, remembers it if it's given once, and tells them.
    private static void give(ServerPlayer player, Identifier id, Feat feat, ItemStack trophy) {
        if (feat.once()) GRANTED.update(player, list -> append(list, id));
        giveAndAnnounce(player, trophy);
    }

    // (Named before it's given: going into the inventory can leave the stack given empty.)
    private static void giveAndAnnounce(ServerPlayer player, ItemStack trophy) {
        Component name = trophy.getHoverName();
        Pedestals.give(player, List.of(trophy));
        if (TrophyPedestalsConfig.ANNOUNCE_TROPHIES.get()) player.sendSystemMessage(Component.translatable("trophypedestals.feat.earned", name));
    }

    private static PedestalBlock pedestal(Feat feat) {
        PedestalBlock named = feat.pedestal().map(Pedestals::pedestal).orElse(null);
        return named != null ? named : Pedestals.DEFAULT.get();
    }

    // The statistic a milestone counts, or null if its type or value isn't in the game.
    private static @Nullable Stat<?> stat(Feat.StatTrigger trigger) {
        StatType<?> type = BuiltInRegistries.STAT_TYPE.getOptional(trigger.stat()).orElse(null);
        return type == null ? null : stat(type, trigger.value());
    }

    private static <T> @Nullable Stat<T> stat(StatType<T> type, Identifier value) {
        T argument = type.getRegistry().getOptional(value).orElse(null);
        return argument == null ? null : type.get(argument);
    }

    private static List<Identifier> append(List<Identifier> list, Identifier id) {
        if (list.contains(id)) return list;
        List<Identifier> added = new ArrayList<>(list);
        added.add(id);
        return List.copyOf(added);
    }

    /** A kind whose trophies carry their own title and icon; without them, its subject's id and nothing turning over it. */
    private static final class TitledKind implements TrophyKind {
        @Override
        public Component name(Identifier subject, boolean client) {
            return Component.literal(subject.toString());
        }

        @Override
        public ItemStack icon(Identifier subject, boolean client) {
            return ItemStack.EMPTY;
        }
    }
}
