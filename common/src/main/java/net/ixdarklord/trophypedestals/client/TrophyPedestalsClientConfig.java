package net.ixdarklord.trophypedestals.client;

import net.ixdarklord.coolcatcore.api.config.Config;
import net.ixdarklord.coolcatcore.api.config.ConfigBuilder;
import net.ixdarklord.coolcatcore.api.config.ConfigScope;
import net.ixdarklord.coolcatcore.api.config.ConfigValue;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;

/**
 * Each player's own settings ({@code config/trophypedestals-client}): how much of pedestals' show their game draws and
 * plays, for a calmer or lighter trophy room, whatever the server or each pedestal's owner has set. They only take
 * away: a pedestal whose owner turned its flourishes or sounds off stays without them.
 */
public final class TrophyPedestalsClientConfig {
    private static final ConfigBuilder BUILDER = Config.builder(TrophyPedestalsMod.MOD_ID, ConfigScope.CLIENT);

    public static final ConfigValue<Boolean> FLOURISHES = BUILDER.bool("flourishes", true)
            .comment("Whether ranked trophies show their flourishes: beams, halos, flares, crowns and particles. Their showcase light and outlines stay.")
            .build();
    public static final ConfigValue<Boolean> AMBIENT_SOUNDS = BUILDER.bool("ambient_sounds", true)
            .comment("Whether ranked trophies play their ambient sounds and chimes now and then.")
            .build();
    public static final ConfigValue<Boolean> MOVING_MOBS = BUILDER.bool("moving_mobs", true)
            .comment("Whether mobs and statues on trophies move and play their animations. Off, they all hold still.")
            .build();
    public static final ConfigValue<Integer> DISPLAY_DISTANCE = BUILDER.intValue("display_distance", 64)
            .range(16, 256)
            .comment("How far away, in blocks, what pedestals show is drawn (farther for one shown larger or higher). The pedestals themselves are drawn as far as other blocks.")
            .build();

    public static final Config CONFIG = BUILDER.build();

    private TrophyPedestalsClientConfig() {}

    /** Loads the config; called from the client mod constructor. */
    public static void init() {}
}
