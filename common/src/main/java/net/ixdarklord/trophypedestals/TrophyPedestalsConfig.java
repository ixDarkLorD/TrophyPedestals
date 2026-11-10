package net.ixdarklord.trophypedestals;

import net.ixdarklord.coolcatcore.api.config.Config;
import net.ixdarklord.coolcatcore.api.config.ConfigBuilder;
import net.ixdarklord.coolcatcore.api.config.ConfigScope;
import net.ixdarklord.coolcatcore.api.config.ConfigValue;
import net.ixdarklord.coolcatcore.api.config.type.EnumType;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * The server's settings ({@code config/trophypedestals-server}, per world), in two groups: {@code mob_trophies} (whether
 * and how often mobs drop trophies, and their rank, with the kill counts in {@code mob_trophies.kills}) and
 * {@code pedestals} (who may adjust them, taking items off, and which of the adjust screen's settings players can change:
 * a trophy's in {@code pedestals.trophy_settings}, a display pedestal's in {@code pedestals.display_pedestal_settings}).
 */
public final class TrophyPedestalsConfig {
    private static final ConfigBuilder BUILDER = Config.builder(TrophyPedestalsMod.MOD_ID, ConfigScope.SERVER);

    // --- Mob trophies ---

    static {
        BUILDER.push("mob_trophies", "Trophies that mobs drop when a player kills them.");
    }

    public static final ConfigValue<Boolean> MOB_TROPHIES = BUILDER.bool("enabled", true)
            .comment("Whether mobs a player kills can drop a trophy pedestal showing them.")
            .build();
    public static final ConfigValue<Double> MOB_TROPHY_CHANCE = BUILDER.doubleValue("chance", 0.01)
            .range(0.0, 1.0)
            .comment("The chance a mob a player kills drops its trophy, from 0 (never) to 1 (always). 0.01 is one in a hundred.")
            .build();
    public static final ConfigValue<Boolean> PLAYER_TROPHIES = BUILDER.bool("player_trophies", false)
            .comment("Whether a player another player kills can drop a trophy pedestal showing their statue, at the same chance. Off by default.")
            .build();
    public static final ConfigValue<Double> GILDED_CHANCE = BUILDER.doubleValue("gilded_chance", 0.02)
            .range(0.0, 1.0)
            .comment("The chance a mob trophy that drops is gilded: on a gold pedestal, named \"Gilded ...\". 0.02 is one in fifty.")
            .build();
    public static final ConfigValue<Boolean> GROWING_TROPHIES = BUILDER.bool("growing", true)
            .comment("Whether a placed mob trophy counts its slayer's kills of its mob and shows them, and with rank KILLS goes up a rank as they reach the counts under kills.")
            .build();
    public static final ConfigValue<MobTrophyRarity> MOB_TROPHY_RARITY = BUILDER.enumValue("rank", MobTrophyRarity.NONE)
            .comment("How mob trophies get their rank: NONE (the default), no rank, shown as a display pedestal is; STRENGTH, by how tough the mob is (the tougher, the grander); or KILLS, by how many of that mob the player has killed (the counts under kills).")
            .build();

    static {
        BUILDER.push("kills", "With rank KILLS: how many of a mob a player must have killed for its trophy to reach each rank (common before that).");
    }

    public static final ConfigValue<Integer> KILLS_UNCOMMON = BUILDER.intValue("uncommon", 10)
            .range(1, 1_000_000)
            .comment("Kills for an uncommon trophy.")
            .build();
    public static final ConfigValue<Integer> KILLS_RARE = BUILDER.intValue("rare", 25)
            .range(1, 1_000_000)
            .comment("Kills for a rare trophy.")
            .build();
    public static final ConfigValue<Integer> KILLS_EPIC = BUILDER.intValue("epic", 50)
            .range(1, 1_000_000)
            .comment("Kills for an epic trophy.")
            .build();
    public static final ConfigValue<Integer> KILLS_LEGENDARY = BUILDER.intValue("legendary", 100)
            .range(1, 1_000_000)
            .comment("Kills for a legendary trophy.")
            .build();
    /**
     * Other counts for some mobs, as {@code "<mob id or #tag> = uncommon, rare, epic, legendary"}. The first entry naming
     * the mob by id wins, then the first naming one of its tags; other mobs use the counts above.
     */
    public static final ConfigValue<List<String>> KILLS_OVERRIDES = BUILDER.stringList("overrides", List.of(
                    "#trophypedestals:legendary_mob_trophies = 1, 2, 3, 5",
                    "#trophypedestals:epic_mob_trophies = 2, 5, 10, 20"))
            .comment("Other kill counts for some mobs or groups of mobs, each \"<mob id or #entity type tag> = uncommon, rare, epic, legendary\" (bosses need fewer by default). A mob named by its id wins over one of its tags; the first matching entry is used.")
            .build();

    static {
        BUILDER.pop().pop();
    }

    // --- Earned trophies ---

    static {
        BUILDER.push("earned_trophies", "Trophies players earn for what they do: advancements, and data packs' feats (milestones and first kills).");
    }

    public static final ConfigValue<AdvancementTrophies> ADVANCEMENT_TROPHIES = BUILDER.enumValue("advancements", AdvancementTrophies.NONE)
            .comment("Which advancements give a trophy, titled and shown as the advancement is: NONE (the default); CHALLENGES, the purple-framed ones; GOALS, goals and challenges; or ALL. Their rank goes by their frame: tasks uncommon, goals rare, challenges epic.")
            .build();
    public static final ConfigValue<Boolean> FEAT_TROPHIES = BUILDER.bool("feats", true)
            .comment("Whether data packs' trophies (data/<namespace>/trophypedestals/feats) are given, the mod's own included: first kills of bosses and milestones.")
            .build();
    public static final ConfigValue<Boolean> ANNOUNCE_TROPHIES = BUILDER.bool("announce", true)
            .comment("Whether a player is told in chat when they earn a trophy.")
            .build();

    static {
        BUILDER.pop();
    }

    // --- The Mob Trophies Ledger ---

    static {
        BUILDER.push("ledger", "The Mob Trophies Ledger: buying back mob trophies a player has collected, for when one's lost.");
    }

    public static final ConfigValue<Boolean> LEDGER_BUY_BACK = BUILDER.bool("buy_back", true)
            .comment("Whether a player can buy back a mob trophy they've collected from their Mob Trophies Ledger, for experience levels. One they carry already can't be bought again.")
            .build();
    public static final ConfigValue<Integer> LEDGER_BUY_BACK_LEVELS = BUILDER.intValue("levels", 5)
            .range(0, 100)
            .comment("The experience levels a mob trophy bought back costs (a gilded one twice as many). 0 makes it free.")
            .build();

    static {
        BUILDER.pop();
    }

    // --- Pedestals ---

    static {
        BUILDER.push("pedestals", "Who may adjust pedestals, and what they can change.");
    }

    public static final ConfigValue<PedestalAccess> PEDESTAL_ACCESS = BUILDER.enumValue("access", PedestalAccess.OWNER)
            .comment("Who can adjust a pedestal: OWNER, the player who placed it, operators, and whoever its owner allows on its screen (Private, Team or Everyone); or ANYONE.")
            .build();
    public static final ConfigValue<Boolean> PEDESTAL_TAKE_ITEMS = BUILDER.bool("take_items", true)
            .comment("Whether a player can take an item off a display pedestal by using it with an empty hand. When off, the item stays until the pedestal is broken.")
            .build();

    static {
        BUILDER.push("trophy_settings", "Which tabs of a trophy's adjust screen players can use (Behavior always). A tab turned off is hidden, and the server ignores its settings.");
    }

    public static final ConfigValue<Boolean> TROPHY_DISPLAY_SETTINGS = BUILDER.bool("display", true)
            .comment("The Display tab: size, height, yaw, pitch, roll and crown.")
            .build();
    public static final ConfigValue<Boolean> TROPHY_TEXT_SETTINGS = BUILDER.bool("text", true)
            .comment("The Text tab: when its text shows, how high and how large.")
            .build();
    public static final ConfigValue<Boolean> TROPHY_POSE_SETTINGS = BUILDER.bool("pose", true)
            .comment("The Pose tab: a mob's or statue's pose and animation, and showing a statue.")
            .build();

    static {
        BUILDER.pop().push("display_pedestal_settings", "Which extra tabs a display pedestal's adjust screen has. With both off (the default) it keeps its one-page screen: when its text shows, redstone, sounds and its showcase light.");
    }

    public static final ConfigValue<Boolean> DISPLAY_PEDESTAL_DISPLAY_SETTINGS = BUILDER.bool("display", false)
            .comment("A Display tab: its item's size, height, yaw, pitch and roll. Off by default.")
            .build();
    public static final ConfigValue<Boolean> DISPLAY_PEDESTAL_TEXT_SETTINGS = BUILDER.bool("text", false)
            .comment("Its text's height and size, on a Text tab (when its text shows can always be changed). Off by default.")
            .build();

    static {
        BUILDER.pop().pop();
    }

    public static final Config CONFIG = BUILDER.build();

    private TrophyPedestalsConfig() {}

    /** Loads the config; called from the mod constructor. */
    public static void init() {}

    /** Whether players can change a pedestal's Display settings: a trophy's, or a display pedestal's. */
    public static boolean displaySettings(boolean trophy) {
        return (trophy ? TROPHY_DISPLAY_SETTINGS : DISPLAY_PEDESTAL_DISPLAY_SETTINGS).get();
    }

    /** Whether players can change a pedestal's text height and size: a trophy's, or a display pedestal's. */
    public static boolean textSettings(boolean trophy) {
        return (trophy ? TROPHY_TEXT_SETTINGS : DISPLAY_PEDESTAL_TEXT_SETTINGS).get();
    }

    /** Whether players can change when a pedestal's text shows: a trophy's with its Text tab, a display pedestal's always. */
    public static boolean textModeSettings(boolean trophy) {
        return !trophy || TROPHY_TEXT_SETTINGS.get();
    }

    /** Whether players can change a trophy's Pose settings (a display pedestal has none). */
    public static boolean poseSettings(boolean trophy) {
        return trophy && TROPHY_POSE_SETTINGS.get();
    }

    /** Whether a display pedestal's screen has tabs (Display or Text), rather than its one page. */
    public static boolean tabbedDisplayPedestals() {
        return DISPLAY_PEDESTAL_DISPLAY_SETTINGS.get() || DISPLAY_PEDESTAL_TEXT_SETTINGS.get();
    }

    /** How mob trophies get their rank. */
    public enum MobTrophyRarity implements EnumType.Displayable {
        /** By how tough the mob is: its health and attack, or the boss tags. */
        STRENGTH,
        /** By how many of that mob the player has killed (the game's own kill statistic). */
        KILLS,
        /** None: shown as a display pedestal is. */
        NONE;

        @Override
        public Component displayName() {
            return Component.translatable("config.trophypedestals.server.mob_trophies.rank." + name().toLowerCase(Locale.ROOT));
        }

        public Component description() {
            return Component.translatable("config.trophypedestals.server.mob_trophies.rank." + name().toLowerCase(Locale.ROOT) + ".desc");
        }
    }

    /** Which advancements give a trophy. */
    public enum AdvancementTrophies implements EnumType.Displayable {
        /** None. */
        NONE,
        /** Challenges only. */
        CHALLENGES,
        /** Goals and challenges. */
        GOALS,
        /** Every advancement shown in the advancements screen. */
        ALL;

        @Override
        public Component displayName() {
            return Component.translatable("config.trophypedestals.server.earned_trophies.advancements." + name().toLowerCase(Locale.ROOT));
        }

        public Component description() {
            return Component.translatable("config.trophypedestals.server.earned_trophies.advancements." + name().toLowerCase(Locale.ROOT) + ".desc");
        }
    }

    /** Who can adjust a pedestal. */
    public enum PedestalAccess implements EnumType.Displayable {
        /** The player who placed it, and operators; anyone, for a pedestal placed before pedestals had owners. */
        OWNER,
        /** Anyone. */
        ANYONE;

        @Override
        public Component displayName() {
            return Component.translatable("config.trophypedestals.server.pedestals.access." + name().toLowerCase(Locale.ROOT));
        }

        public Component description() {
            return Component.translatable("config.trophypedestals.server.pedestals.access." + name().toLowerCase(Locale.ROOT) + ".desc");
        }
    }
}
