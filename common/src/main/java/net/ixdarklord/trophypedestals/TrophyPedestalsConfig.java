package net.ixdarklord.trophypedestals;

import net.ixdarklord.coolcatcore.api.config.Config;
import net.ixdarklord.coolcatcore.api.config.ConfigBuilder;
import net.ixdarklord.coolcatcore.api.config.ConfigScope;
import net.ixdarklord.coolcatcore.api.config.ConfigValue;
import net.ixdarklord.coolcatcore.api.config.type.EnumType;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/** The server's settings ({@code config/trophypedestals-server}, per world), editable in Glazed Menu or Configured. */
public final class TrophyPedestalsConfig {
    private static final ConfigBuilder BUILDER = Config.builder(TrophyPedestalsMod.MOD_ID, ConfigScope.SERVER);

    public static final ConfigValue<Boolean> MOB_TROPHIES = BUILDER.bool("mob_trophies", true)
            .comment("Whether mobs a player kills can drop a trophy pedestal showing them.")
            .build();
    public static final ConfigValue<Double> MOB_TROPHY_CHANCE = BUILDER.doubleValue("mob_trophy_chance", 0.01)
            .range(0.0, 1.0)
            .comment("The chance a mob a player kills drops its trophy, from 0 (never) to 1 (always). 0.01 is one in a hundred.")
            .build();

    public static final ConfigValue<Boolean> PLAYER_TROPHIES = BUILDER.bool("player_trophies", false)
            .comment("Whether a player another player kills can drop a trophy pedestal showing their statue, at the mob trophy chance. Off by default.")
            .build();

    public static final ConfigValue<MobTrophyRarity> MOB_TROPHY_RARITY = BUILDER.enumValue("mob_trophy_rarity", MobTrophyRarity.NONE)
            .comment("How mob trophies get their rank: NONE (the default), no rank, shown as a display pedestal is; STRENGTH, by how tough the mob is (the tougher, the grander); or KILLS, by how many of that mob the player has killed (the mob_trophy_kills_* counts below).")
            .build();

    /** With KILLS: how many of a mob a player must have killed for its trophy to be uncommon, rare, epic, legendary. */
    public static final ConfigValue<Integer> KILLS_UNCOMMON = BUILDER.intValue("mob_trophy_kills_uncommon", 10)
            .range(1, 1_000_000)
            .comment("With mob_trophy_rarity KILLS: how many of a mob a player must have killed for its trophy to be uncommon.")
            .build();
    public static final ConfigValue<Integer> KILLS_RARE = BUILDER.intValue("mob_trophy_kills_rare", 25)
            .range(1, 1_000_000)
            .comment("With mob_trophy_rarity KILLS: kills for a rare trophy.")
            .build();
    public static final ConfigValue<Integer> KILLS_EPIC = BUILDER.intValue("mob_trophy_kills_epic", 50)
            .range(1, 1_000_000)
            .comment("With mob_trophy_rarity KILLS: kills for an epic trophy.")
            .build();
    public static final ConfigValue<Integer> KILLS_LEGENDARY = BUILDER.intValue("mob_trophy_kills_legendary", 100)
            .range(1, 1_000_000)
            .comment("With mob_trophy_rarity KILLS: kills for a legendary trophy.")
            .build();

    /**
     * With KILLS: other counts for some mobs, as {@code "<mob id or #tag> = uncommon, rare, epic, legendary"}. The first
     * entry naming the mob by id wins, then the first naming one of its tags; other mobs use the counts above.
     */
    public static final ConfigValue<List<String>> KILLS_OVERRIDES = BUILDER.stringList("mob_trophy_kills_overrides", List.of(
                    "#trophypedestals:legendary_mob_trophies = 1, 2, 3, 5",
                    "#trophypedestals:epic_mob_trophies = 2, 5, 10, 20"))
            .comment("With mob_trophy_rarity KILLS: other kill counts for some mobs or groups of mobs, each \"<mob id or #entity type tag> = uncommon, rare, epic, legendary\" (bosses need fewer kills by default). A mob named by its id wins over one of its tags; the first matching entry is used.")
            .build();

    public static final ConfigValue<PedestalAccess> PEDESTAL_ACCESS = BUILDER.enumValue("pedestal_access", PedestalAccess.OWNER)
            .comment("Who can adjust a pedestal (its text and display size): OWNER, the player who placed it, operators, and whoever its owner allows on its screen (Private, Team or Everyone); or ANYONE.")
            .build();
    public static final ConfigValue<Boolean> PEDESTAL_TAKE_ITEMS = BUILDER.bool("pedestal_take_items", true)
            .comment("Whether a player can take an item off a display pedestal by using it with an empty hand. When off, the item stays until the pedestal is broken.")
            .build();

    /**
     * Which of a pedestal's screen's settings players can change (its Behavior settings always can): turned off, a tab
     * leaves the screen and the server ignores its settings.
     */
    public static final ConfigValue<Boolean> PEDESTAL_DISPLAY_SETTINGS = BUILDER.bool("pedestal_display_settings", true)
            .comment("Whether players can change a pedestal's Display settings on its screen (its size, height, yaw, pitch, roll and crown). Off, the tab is hidden and they stay as they are.")
            .build();
    public static final ConfigValue<Boolean> PEDESTAL_TEXT_SETTINGS = BUILDER.bool("pedestal_text_settings", true)
            .comment("Whether players can change a pedestal's Text settings on its screen (when its text shows, how high and how large). Off, the tab is hidden and they stay as they are.")
            .build();
    public static final ConfigValue<Boolean> PEDESTAL_POSE_SETTINGS = BUILDER.bool("pedestal_pose_settings", true)
            .comment("Whether players can change a pedestal's Pose settings on its screen (a mob's or statue's pose and animation, and showing a statue). Off, the tab is hidden and they stay as they are.")
            .build();

    public static final Config CONFIG = BUILDER.build();

    private TrophyPedestalsConfig() {}

    /** Loads the config; called from the mod constructor. */
    public static void init() {}

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
            return Component.translatable("config.trophypedestals.server.mob_trophy_rarity." + name().toLowerCase(Locale.ROOT));
        }

        @Override
        public Component description() {
            return Component.translatable("config.trophypedestals.server.mob_trophy_rarity." + name().toLowerCase(Locale.ROOT) + ".desc");
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
            return Component.translatable("config.trophypedestals.server.pedestal_access." + name().toLowerCase(Locale.ROOT));
        }

        @Override
        public Component description() {
            return Component.translatable("config.trophypedestals.server.pedestal_access." + name().toLowerCase(Locale.ROOT) + ".desc");
        }
    }
}
