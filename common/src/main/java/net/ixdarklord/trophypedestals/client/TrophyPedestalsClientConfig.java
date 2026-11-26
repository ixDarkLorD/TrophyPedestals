package net.ixdarklord.trophypedestals.client;

import net.ixdarklord.coolcatcore.api.config.Config;
import net.ixdarklord.coolcatcore.api.config.ConfigBuilder;
import net.ixdarklord.coolcatcore.api.config.ConfigScope;
import net.ixdarklord.coolcatcore.api.config.ConfigValue;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;

public final class TrophyPedestalsClientConfig {
    private static final ConfigBuilder BUILDER = Config.builder(TrophyPedestalsMod.MOD_ID, ConfigScope.CLIENT);

    public static final ConfigValue<Boolean> FLOURISHES = BUILDER.bool("flourishes", true)
            .comment("Whether ranked trophies show their flourishes: beams, halos, flares, crowns and particles. Their showcase light and outlines stay.")
            .build();
    public static final ConfigValue<Boolean> LENS_EFFECTS = BUILDER.bool("lens_effects", true)
            .comment("Whether the lenses fitted to display pedestals show their effects: ripples, runes, embers, snowflakes, scanlines and the item's motion. Off, a lens only colours the light.")
            .build();
    public static final ConfigValue<Boolean> PEDESTAL_LIGHTING = BUILDER.bool("pedestal_lighting", false)
            .comment("Whether lit pedestals cast their lens's or rank's colour onto the blocks around them, the Prism its rainbow. Works with shader packs. Off, only their own light shows.")
            .build();
    public static final ConfigValue<Boolean> LIGHTING_PROMPT = BUILDER.bool("lighting_prompt", true)
            .comment("Whether joining a world asks to turn on the coloured lighting, while it's off. Its \"Don't show this again\" turns this off.")
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

    public static final ConfigValue<Boolean> HIDE_DISABLED_TABS = BUILDER.bool("hide_disabled_tabs", true)
            .comment("Whether the pedestal screen hides the tabs the server has turned off. Off, they stay in the row, greyed out, and say why.")
            .build();

    public static final Config CONFIG = BUILDER.build();

    private TrophyPedestalsClientConfig() {}

    public static void init() {}
}
