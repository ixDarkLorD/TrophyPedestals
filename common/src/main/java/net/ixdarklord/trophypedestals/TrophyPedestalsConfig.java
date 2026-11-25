package net.ixdarklord.trophypedestals;

import net.ixdarklord.coolcatcore.api.config.Config;
import net.ixdarklord.coolcatcore.api.config.ConfigBuilder;
import net.ixdarklord.coolcatcore.api.config.ConfigScope;
import net.ixdarklord.coolcatcore.api.config.ConfigValue;
import net.ixdarklord.coolcatcore.api.config.type.EnumType;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

public final class TrophyPedestalsConfig {
    private static final ConfigBuilder BUILDER = Config.builder(TrophyPedestalsMod.MOD_ID, ConfigScope.SERVER);

    static {
        BUILDER.push("mob_trophies", "Trophies that mobs drop when a player kills them.");
    }

    public static final ConfigValue<Boolean> MOB_TROPHIES = BUILDER.bool("enabled", true)
            .comment("Whether mobs a player kills can drop a trophy pedestal showing them.")
            .build();
    public static final ConfigValue<Double> MOB_TROPHY_CHANCE = BUILDER.doubleValue("chance", 0.015)
            .range(0.0, 1.0)
            .comment("The chance a mob a player kills drops its trophy, from 0 (never) to 1 (always). 0.015 is about one in sixty-seven.")
            .build();
    public static final ConfigValue<Double> BOSS_TROPHY_CHANCE = BUILDER.doubleValue("boss_chance", 0.15)
            .range(0.0, 1.0)
            .comment("The chance a boss (a mob in #trophypedestals:epic_mob_trophies or #trophypedestals:legendary_mob_trophies) a player kills drops its trophy, from 0 to 1. 0.15 is about one in seven.")
            .build();
    public static final ConfigValue<Double> LOOTING_BONUS = BUILDER.doubleValue("looting_bonus", 0.5)
            .range(0.0, 10.0)
            .comment("How much each level of Looting on the weapon a mob is killed with raises its trophy chance, as a share of that chance: 0.5 makes Looting III two and a half times as likely to drop one. 0 turns it off.")
            .build();
    public static final ConfigValue<Boolean> REQUIRE_ENCHANTMENT = BUILDER.bool("require_enchantment", false)
            .comment("Whether mob trophies drop only when the mob is killed with a weapon enchanted with Trophy Hunter (found in enchanting tables, loot and trades, for swords and axes, and any item a pack adds to the #trophypedestals:enchantable/trophy_hunter item tag, such as #minecraft:pickaxes). Off by default.")
            .build();
    public static final ConfigValue<Boolean> ALL_MOBS = BUILDER.bool("all_mobs", true)
            .comment("Whether every mob in the game, every mod's included, drops a trophy and is listed in the Mobs creative tab and the Mob Trophies Ledger. On by default. Turned off, only Minecraft's own mobs do, and the mobs a pack adds, either to the #trophypedestals:mob_trophies entity type tag or by naming them (by id, #tag or @mod id) in chance_overrides with a chance above 0. A change applies when the world (or server) next starts. WARNING: in large packs this can behave unexpectedly (very long lists, mobs never meant to be collected, some shown as spawn eggs); turning it off and adding chosen mods' mobs is safer.")
            .requiresWorldRestart()
            .build();
    public static final ConfigValue<List<String>> MOB_TROPHY_CHANCE_OVERRIDES = BUILDER.stringList("chance_overrides", List.of())
            .comment("Other drop chances for some mobs, groups of mobs or whole mods, each \"<mob id, #entity type tag or @mod id> = chance\" (0 to 1), e.g. \"minecraft:zombie = 0.001\", \"#minecraft:raiders = 0.05\" or \"@alexsmobs = 0.005\". A mob named by its id wins over one of its tags, and a tag over its mod.")
            .build();
    public static final ConfigValue<Boolean> BOSS_TROPHIES_ONCE = BUILDER.bool("boss_trophies_once", false)
            .comment("Whether a boss (a mob in #trophypedestals:epic_mob_trophies or #trophypedestals:legendary_mob_trophies) drops its trophy only for a player who hasn't collected it yet. Lost ones can still be bought back from the Mob Trophies Ledger. Off by default.")
            .build();
    public static final ConfigValue<Boolean> ANNOUNCE_RARE = BUILDER.bool("announce_rare", true)
            .comment("Whether the whole server is told in chat when someone gets a legendary or gilded mob trophy.")
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
    public static final ConfigValue<List<String>> KILLS_OVERRIDES = BUILDER.stringList("overrides", List.of(
                    "#trophypedestals:legendary_mob_trophies = 1, 2, 3, 5",
                    "#trophypedestals:epic_mob_trophies = 2, 5, 10, 20"))
            .comment("Other kill counts for some mobs or groups of mobs, each \"<mob id or #entity type tag> = uncommon, rare, epic, legendary\" (bosses need fewer by default). A mob named by its id wins over one of its tags; the first matching entry is used.")
            .build();

    static {
        BUILDER.pop().pop();
    }

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
        BUILDER.pop().push("display_pedestal_settings", "Which of a display pedestal's adjust screen settings players can change. Its Display tab shows greyed out while it's off, and its Text tab shows only when its text shows.");
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

    public static void init() {}

    public static boolean displaySettings(boolean trophy) {
        return (trophy ? TROPHY_DISPLAY_SETTINGS : DISPLAY_PEDESTAL_DISPLAY_SETTINGS).get();
    }

    public static boolean textSettings(boolean trophy) {
        return (trophy ? TROPHY_TEXT_SETTINGS : DISPLAY_PEDESTAL_TEXT_SETTINGS).get();
    }

    public static boolean textModeSettings(boolean trophy) {
        return !trophy || TROPHY_TEXT_SETTINGS.get();
    }

    public static boolean poseSettings(boolean trophy) {
        return trophy && TROPHY_POSE_SETTINGS.get();
    }

    public enum MobTrophyRarity implements EnumType.Displayable {
        STRENGTH,
        KILLS,
        NONE;

        @Override
        public Component displayName() {
            return Component.translatable("config.trophypedestals.server.mob_trophies.rank." + name().toLowerCase(Locale.ROOT));
        }

        public Component description() {
            return Component.translatable("config.trophypedestals.server.mob_trophies.rank." + name().toLowerCase(Locale.ROOT) + ".desc");
        }
    }

    public enum AdvancementTrophies implements EnumType.Displayable {
        NONE,
        CHALLENGES,
        GOALS,
        ALL;

        @Override
        public Component displayName() {
            return Component.translatable("config.trophypedestals.server.earned_trophies.advancements." + name().toLowerCase(Locale.ROOT));
        }

        public Component description() {
            return Component.translatable("config.trophypedestals.server.earned_trophies.advancements." + name().toLowerCase(Locale.ROOT) + ".desc");
        }
    }

    public enum PedestalAccess implements EnumType.Displayable {
        OWNER,
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
