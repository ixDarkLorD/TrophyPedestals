package net.ixdarklord.trophypedestals.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

/**
 * A trophy pedestal's rank: how grand it looks and sounds, its colors and how strong each of its effects is. Trophy Pedestals
 * has five, from {@link PedestalRarities#COMMON} (rank 1) to {@link PedestalRarities#LEGENDARY} (rank 5); other mods add
 * their own with {@link PedestalRarities#register}, and draw anything more with {@code api/client/PedestalEffects}.
 *
 * @param id           its id, which trophies record and data names it by
 * @param rank         where it stands among the others, higher being grander; they're listed by it
 * @param color        its main color (RGB): the well's light, the rising outlines and the flare
 * @param accent       its second color (RGB): glints, the orbiting sparks, the second flare and the shimmer
 * @param rings        how many outlines rise at once (0 for none)
 * @param ringTicks    how long each takes to rise, in ticks
 * @param flareSize    the flare's width in blocks (0 for none)
 * @param flareAlpha   how strong the flare is, 0 to 1
 * @param flareSpin    how fast the flare turns, as a share of the icon's own turning
 * @param flarePulse   how much the flare swells and shrinks, as a share of its size (0 for steady)
 * @param secondFlare  whether a second flare in the accent color turns the other way inside the first
 * @param spiralArms   how many arms the rising spiral of sparkles has (0 for none)
 * @param spiralRate   how many ticks out of each one an arm gives a sparkle, 0 to 1
 * @param glints       the chance each tick of a glint drifting up, 0 to 1
 * @param orbiters     how many sparks circle the icon, leaving trails
 * @param halos        how many tilted rings of light circle the icon, each with a bright arc sweeping round it (0 for none)
 * @param haloRadius   the halos' radius in blocks
 * @param crown        how many shards of light stand in a turning crown round the icon (0 for none)
 * @param beamHeight   how high a beam of light rises from the well, in blocks (0 for none)
 * @param shimmer      whether its outlines alternate between its two colors and its flare shifts between them
 * @param burstTicks   how often a ring of sparks bursts out from the icon, in ticks (0 for never)
 * @param placeSound   played to everyone near when a trophy of this rank is placed (null for none)
 * @param ambientSound played now and then to a player near a trophy of this rank (null for none)
 * @param ambientTicks how often the ambient sound plays, in ticks
 */
public record PedestalRarity(Identifier id, int rank, int color, int accent, int rings, float ringTicks, float flareSize, float flareAlpha,
                             float flareSpin, float flarePulse, boolean secondFlare, int spiralArms, float spiralRate, float glints,
                             int orbiters, int halos, float haloRadius, int crown, float beamHeight, boolean shimmer, int burstTicks, @Nullable Sound placeSound,
                             @Nullable Sound ambientSound, int ambientTicks) {
    /** A sound by its sound event's id (vanilla's or a mod's), at a volume and pitch. */
    public record Sound(Identifier id, float volume, float pitch) {}

    /** Its name, in its color: the {@code trophypedestals.rarity.<namespace>.<path>} lang key. */
    public Component displayName() {
        return Component.translatable("trophypedestals.rarity." + id.getNamespace() + "." + id.getPath()).withColor(color);
    }

    /** Starts a rarity of this rank with these colors and no effects or sounds; add the ones it has. */
    public static Builder builder(Identifier id, int rank, int color, int accent) {
        return new Builder(id, rank, color, accent);
    }

    public static final class Builder {
        private final Identifier id;
        private final int rank, color, accent;
        private int rings, spiralArms, orbiters, halos, crown, burstTicks, ambientTicks = 200;
        private float haloRadius = 0.6F, ringTicks = 32.0F, flareSize, flareAlpha, flareSpin, flarePulse, spiralRate, glints, beamHeight;
        private boolean secondFlare, shimmer;
        private @Nullable Sound placeSound, ambientSound;

        private Builder(Identifier id, int rank, int color, int accent) {
            this.id = id;
            this.rank = rank;
            this.color = color & 0xFFFFFF;
            this.accent = accent & 0xFFFFFF;
        }

        /** Outlines rising out of the well: how many at once, and how many ticks each takes. */
        public Builder rings(int count, float ticks) {
            this.rings = count;
            this.ringTicks = ticks;
            return this;
        }

        /** A flare behind the icon: its width in blocks, its strength (0 to 1), and its turning speed (1 turns with the icon). */
        public Builder flare(float size, float alpha, float spin) {
            this.flareSize = size;
            this.flareAlpha = alpha;
            this.flareSpin = spin;
            return this;
        }

        /** Makes the flare swell and shrink by this share of its size. */
        public Builder flarePulse(float pulse) {
            this.flarePulse = pulse;
            return this;
        }

        /** Adds a second flare in the accent color, turning the other way. */
        public Builder secondFlare() {
            this.secondFlare = true;
            return this;
        }

        /** A spiral of sparkles rising round the icon: how many arms, and how many ticks out of each one they give one. */
        public Builder spiral(int arms, float rate) {
            this.spiralArms = arms;
            this.spiralRate = rate;
            return this;
        }

        /** The chance each tick of a glint drifting up. */
        public Builder glints(float chance) {
            this.glints = chance;
            return this;
        }

        /** Sparks circling the icon. */
        public Builder orbiters(int count) {
            this.orbiters = count;
            return this;
        }

        /** Tilted rings of light of this radius circling the icon, crossing each other when there are more than one. */
        public Builder halos(int count, float radius) {
            this.halos = count;
            this.haloRadius = radius;
            return this;
        }

        /** A crown of this many shards of light standing round the icon, turning slowly, rising and falling in a wave. */
        public Builder crown(int shards) {
            this.crown = shards;
            return this;
        }

        /** A beam of light rising this many blocks out of the well. */
        public Builder beam(float height) {
            this.beamHeight = height;
            return this;
        }

        /** Outlines alternating between the two colors, and a flare shifting between them. */
        public Builder shimmer() {
            this.shimmer = true;
            return this;
        }

        /** A ring of sparks bursting out from the icon every this many ticks. */
        public Builder burst(int ticks) {
            this.burstTicks = ticks;
            return this;
        }

        /** A sound played to everyone near when a trophy of this rank is placed. */
        public Builder placeSound(Identifier sound, float volume, float pitch) {
            this.placeSound = new Sound(sound, volume, pitch);
            return this;
        }

        /** A sound played now and then, every this many ticks, to a player near a trophy of this rank. */
        public Builder ambientSound(Identifier sound, float volume, float pitch, int everyTicks) {
            this.ambientSound = new Sound(sound, volume, pitch);
            this.ambientTicks = Math.max(1, everyTicks);
            return this;
        }

        public PedestalRarity build() {
            return new PedestalRarity(id, rank, color, accent, rings, ringTicks, flareSize, flareAlpha, flareSpin, flarePulse, secondFlare,
                    spiralArms, spiralRate, glints, orbiters, halos, haloRadius, crown, beamHeight, shimmer, burstTicks, placeSound, ambientSound, ambientTicks);
        }
    }
}
