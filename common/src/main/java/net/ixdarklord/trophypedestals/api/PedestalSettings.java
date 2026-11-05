package net.ixdarklord.trophypedestals.api;

import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * A pedestal's settings for other mods (everything its adjustment screen sets): read them, set them, and lock them so
 * no player can change them, its owner and operators included. A player who tries is refused with a message of the
 * mod's own (best a translatable one), or silently. Call these on the server; the change is saved and sent to clients.
 * <pre>{@code
 * PedestalSettings.force(pedestal, PedestalSettings.Settings.builder().scale(2.0F).spins(false).facing(90.0F).build(),
 *         Component.translatable("mymod.pedestal.locked"));   // set and lock, telling players why
 * PedestalSettings.lock(pedestal);                             // lock as it is, with Trophy Pedestals's own message
 * PedestalSettings.lock(pedestal, null);                       // lock as it is, refusing silently
 * PedestalSettings.unlock(pedestal);
 * PedestalBlocks.pedestalAt(level, pos).ifPresent(PedestalSettings::lock);     // from a position
 * }</pre>
 */
public final class PedestalSettings {
    /** The message a player trying to change locked settings is told unless the mod gives its own. */
    public static final Component DEFAULT_REFUSAL = Component.translatable("trophypedestals.pedestal.locked");

    private PedestalSettings() {}

    /** Its settings as they are now. */
    public static Settings get(PedestalBlockEntity pedestal) {
        return new Settings(pedestal.textMode(), pedestal.scale(), pedestal.height(), pedestal.facing(), pedestal.spins(), pedestal.animates(),
                pedestal.effects(), pedestal.sounds(), pedestal.textHeight(), pedestal.redstone(), pedestal.statue(), pedestal.statuePose(),
                pedestal.animation(), pedestal.textScale(), pedestal.crown(), pedestal.light());
    }

    /** Sets all its settings (each kept within its limits), whether it's locked or not. */
    public static void set(PedestalBlockEntity pedestal, Settings settings) {
        pedestal.setTextMode(settings.textMode());
        pedestal.setScale(settings.scale());
        pedestal.setHeight(settings.height());
        pedestal.setFacing(settings.facing());
        pedestal.setSpins(settings.spins());
        pedestal.setAnimates(settings.animates());
        pedestal.setEffects(settings.effects());
        pedestal.setSounds(settings.sounds());
        pedestal.setTextHeight(settings.textHeight());
        pedestal.setRedstone(settings.redstone());
        pedestal.setStatue(settings.statue());
        pedestal.setStatuePose(settings.statuePose());
        pedestal.setAnimation(settings.animation());
        pedestal.setTextScale(settings.textScale());
        pedestal.setCrown(settings.crown());
        pedestal.setLight(settings.light());
    }

    /**
     * Locks its settings as they are: no player can open its adjustment screen. One who tries is told {@code refusal}
     * in their action bar, or, with null, is refused silently.
     */
    public static void lock(PedestalBlockEntity pedestal, @Nullable Component refusal) {
        pedestal.lock(refusal);
    }

    /** Locks its settings as they are, telling a player who tries the {@link #DEFAULT_REFUSAL}. */
    public static void lock(PedestalBlockEntity pedestal) {
        lock(pedestal, DEFAULT_REFUSAL);
    }

    /** Sets its settings and locks them, telling a player who tries the {@link #DEFAULT_REFUSAL}. */
    public static void force(PedestalBlockEntity pedestal, Settings settings) {
        force(pedestal, settings, DEFAULT_REFUSAL);
    }

    /** Sets its settings and locks them ({@link #set}, then {@link #lock}). */
    public static void force(PedestalBlockEntity pedestal, Settings settings, @Nullable Component refusal) {
        set(pedestal, settings);
        lock(pedestal, refusal);
    }

    /** Lets players with access (by the server's {@code pedestal_access}) change its settings again. */
    public static void unlock(PedestalBlockEntity pedestal) {
        pedestal.unlock();
    }

    /** Whether its settings are locked. */
    public static boolean isLocked(PedestalBlockEntity pedestal) {
        return pedestal.locked();
    }

    /** What a player trying to change its locked settings is told; empty if it isn't locked, or refuses silently. */
    public static Optional<Component> refusal(PedestalBlockEntity pedestal) {
        return Optional.ofNullable(pedestal.refusal());
    }

    /**
     * A pedestal's settings, as its adjustment screen sets them.
     *
     * @param textMode   when its text shows: while looked at, always, or never
     * @param scale      how much larger or smaller than usual its item or mob is shown (half to twenty times)
     * @param height     how far it's moved up (or down), in blocks
     * @param facing     which way it faces (where a spinning one starts from), in degrees
     * @param spins      whether it turns
     * @param animates   whether a mob trophy's mob moves, or holds one pose
     * @param effects    whether its flourishes show (its rank's beam, halos, crowns, flares and particles)
     * @param sounds     whether it makes its sounds
     * @param textHeight how far its text is moved up (or down), in blocks
     * @param redstone   how it answers redstone
     * @param statue     whether a tier's trophy unlocked by a player shows their statue in place of its icon
     * @param statuePose the pose a statue it shows takes
     * @param animation  the animation its mob or statue plays on loop while it animates: {@code idle}, {@code walk},
     *                   {@code run}, {@code attack}, or one of the mob's own (one it doesn't have plays as idle)
     * @param textScale  how large its text and frame are shown (half to three times)
     * @param crown      whether a legendary trophy wears its crown over what it shows
     * @param light      whether its showcase light shows (the lit lens, the cone of light out of the well, and its frame)
     */
    public record Settings(PedestalBlockEntity.TextMode textMode, float scale, float height, float facing, boolean spins, boolean animates,
                           boolean effects, boolean sounds, float textHeight, PedestalBlockEntity.RedstoneMode redstone, boolean statue,
                           PedestalBlockEntity.StatuePose statuePose, String animation,
                           float textScale, boolean crown, boolean light) {
        /** A new pedestal's settings. */
        public static final Settings DEFAULT = new Settings(PedestalBlockEntity.TextMode.LOOKING, 1.0F, 0.0F, 0.0F, true, true, true, true, 0.0F,
                PedestalBlockEntity.RedstoneMode.IGNORE, false, PedestalBlockEntity.StatuePose.STANDING, PedestalBlockEntity.IDLE, 1.0F, true, true);

        /** Settings starting from a new pedestal's. */
        public static Builder builder() {
            return new Builder(DEFAULT);
        }

        /** Settings starting from these. */
        public Builder toBuilder() {
            return new Builder(this);
        }
    }

    /** Builds {@link Settings}, each left as it started unless set. */
    public static final class Builder {
        private PedestalBlockEntity.TextMode textMode;
        private float scale, height, facing, textHeight;
        private boolean spins, animates, effects, sounds, statue;
        private PedestalBlockEntity.RedstoneMode redstone;
        private PedestalBlockEntity.StatuePose statuePose;
        private String animation;
        private float textScale;
        private boolean crown, light;

        private Builder(Settings from) {
            textMode = from.textMode();
            scale = from.scale();
            height = from.height();
            facing = from.facing();
            spins = from.spins();
            animates = from.animates();
            effects = from.effects();
            sounds = from.sounds();
            textHeight = from.textHeight();
            redstone = from.redstone();
            statue = from.statue();
            statuePose = from.statuePose();
            animation = from.animation();
            textScale = from.textScale();
            crown = from.crown();
            light = from.light();
        }

        public Builder textMode(PedestalBlockEntity.TextMode textMode) {
            this.textMode = textMode;
            return this;
        }

        public Builder scale(float scale) {
            this.scale = scale;
            return this;
        }

        public Builder height(float height) {
            this.height = height;
            return this;
        }

        public Builder facing(float facing) {
            this.facing = facing;
            return this;
        }

        public Builder spins(boolean spins) {
            this.spins = spins;
            return this;
        }

        public Builder animates(boolean animates) {
            this.animates = animates;
            return this;
        }

        public Builder effects(boolean effects) {
            this.effects = effects;
            return this;
        }

        public Builder sounds(boolean sounds) {
            this.sounds = sounds;
            return this;
        }

        public Builder textHeight(float textHeight) {
            this.textHeight = textHeight;
            return this;
        }

        public Builder redstone(PedestalBlockEntity.RedstoneMode redstone) {
            this.redstone = redstone;
            return this;
        }

        public Builder statue(boolean statue) {
            this.statue = statue;
            return this;
        }

        public Builder statuePose(PedestalBlockEntity.StatuePose statuePose) {
            this.statuePose = statuePose;
            return this;
        }

        public Builder animation(String animation) {
            this.animation = animation;
            return this;
        }

        public Builder textScale(float textScale) {
            this.textScale = textScale;
            return this;
        }

        public Builder crown(boolean crown) {
            this.crown = crown;
            return this;
        }

        public Builder light(boolean light) {
            this.light = light;
            return this;
        }

        public Settings build() {
            return new Settings(textMode, scale, height, facing, spins, animates, effects, sounds, textHeight, redstone, statue, statuePose, animation, textScale, crown, light);
        }
    }
}
