package net.ixdarklord.trophypedestals.pedestal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.Optional;

/**
 * How a trophy on a trophy display (a cabinet or a shelf) is shown: the pedestal settings that can be changed there, for
 * the spot it's on. Everything that would change its size or place (its display's size and height, its text's height and
 * size, its crown's size and place) stays as a new pedestal has it, so it fits the display. They're the spot's, not the
 * trophy's: taken off, the trophy keeps its own, and the spot goes back to {@link #DEFAULT}.
 */
public record DisplayedTrophySettings(PedestalBlockEntity.TextMode textMode, boolean spins, float yaw, float pitch, float roll,
                                      boolean effects,
                                      PedestalBlockEntity.RedstoneMode redstone, boolean sounds, boolean animates, boolean statue,
                                      PedestalBlockEntity.StatuePose statuePose, String animation, boolean crown, boolean light) {
    /** As a new pedestal has them. */
    public static final DisplayedTrophySettings DEFAULT = new DisplayedTrophySettings(PedestalBlockEntity.TextMode.LOOKING, true, 0.0F, 0.0F, 0.0F, true,
            PedestalBlockEntity.RedstoneMode.IGNORE, true, true, false, PedestalBlockEntity.StatuePose.STANDING, PedestalBlockEntity.IDLE, true, true);

    public static final Codec<DisplayedTrophySettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            PedestalBlockEntity.TextMode.CODEC.optionalFieldOf("text", DEFAULT.textMode).forGetter(DisplayedTrophySettings::textMode),
            Codec.BOOL.optionalFieldOf("spins", true).forGetter(DisplayedTrophySettings::spins),
            Codec.FLOAT.optionalFieldOf("yaw").forGetter(settings -> settings.yaw == 0.0F ? Optional.empty() : Optional.of(settings.yaw)),
            // (What yaw was saved as before pitch and roll: read, never written.)
            Codec.FLOAT.optionalFieldOf("facing").forGetter(settings -> Optional.empty()),
            Codec.FLOAT.optionalFieldOf("pitch", 0.0F).forGetter(DisplayedTrophySettings::pitch),
            Codec.FLOAT.optionalFieldOf("roll", 0.0F).forGetter(DisplayedTrophySettings::roll),
            Codec.BOOL.optionalFieldOf("effects", true).forGetter(DisplayedTrophySettings::effects),
            PedestalBlockEntity.RedstoneMode.CODEC.optionalFieldOf("redstone", DEFAULT.redstone).forGetter(DisplayedTrophySettings::redstone),
            Codec.BOOL.optionalFieldOf("sounds", true).forGetter(DisplayedTrophySettings::sounds),
            Codec.BOOL.optionalFieldOf("animates", true).forGetter(DisplayedTrophySettings::animates),
            Codec.BOOL.optionalFieldOf("statue", false).forGetter(DisplayedTrophySettings::statue),
            PedestalBlockEntity.StatuePose.CODEC.optionalFieldOf("pose", DEFAULT.statuePose).forGetter(DisplayedTrophySettings::statuePose),
            Codec.STRING.optionalFieldOf("animation", DEFAULT.animation).forGetter(DisplayedTrophySettings::animation),
            Codec.BOOL.optionalFieldOf("crown", true).forGetter(DisplayedTrophySettings::crown),
            Codec.BOOL.optionalFieldOf("light", true).forGetter(DisplayedTrophySettings::light)
    ).apply(instance, (text, spins, yaw, facing, pitch, roll, effects, redstone, sounds, animates, statue, pose, animation, crown, light) ->
            new DisplayedTrophySettings(text, spins, yaw.or(() -> facing).orElse(0.0F), pitch, roll, effects, redstone, sounds, animates,
                    statue, pose, animation, crown, light)));

    /** Shows a trophy's pedestal (a stand-in for drawing it) with these settings, and as powered or not. */
    public void applyTo(PedestalBlockEntity pedestal, boolean powered) {
        pedestal.setTextMode(textMode);
        pedestal.setSpins(spins);
        pedestal.setRotation(yaw, pitch, roll);
        pedestal.setEffects(effects);
        pedestal.setRedstone(redstone);
        pedestal.setSounds(sounds);
        pedestal.setAnimates(animates);
        pedestal.setStatue(statue);
        pedestal.setStatuePose(statuePose);
        pedestal.setAnimation(animation);
        pedestal.setCrown(crown);
        pedestal.setLight(light);
        pedestal.setPowered(powered);
    }
}
