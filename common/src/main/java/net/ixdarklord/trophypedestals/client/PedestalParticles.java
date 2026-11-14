package net.ixdarklord.trophypedestals.client;

import net.ixdarklord.coolcatcanvas.api.client.vfx.Curve;
import net.ixdarklord.coolcatcanvas.api.client.vfx.ParticleSpec;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldParticles;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.api.client.PedestalEffect;
import net.ixdarklord.trophypedestals.api.client.PedestalEffects;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A trophy pedestal's light and sound, from its rank ({@link PedestalRarity}), glowing in its colors (CoolCatLib's
 * luminous particles, with Trophy Pedestals's own textures): a spiral of four-point sparkles rising out of the turning icon, a
 * soft halo on the cap under it as from a projector, glints drifting up, sparks circling the icon and bursts of light;
 * and its ambient sound now and then while the player is near. The particles show only while the player looks at the
 * pedestal within their reach, and not with particles set to Minimal; half as many with Decreased. Each rank's looks
 * are made once, and its sounds looked up once.
 */
public final class PedestalParticles {
    private static final double SPIRAL_RADIUS = 0.36, ORBIT_RADIUS = 0.5, SOUND_RANGE = 12.0;
    private static final int BURST_SPARKS = 16;
    // Trophy Pedestals's own particle textures, crisp pixel art in white for the colours to tint, drawn pixel for pixel: a
    // four-point sparkle, a diamond glint, a round halo.
    private static final Identifier SPARKLE = TrophyPedestalsMod.id("textures/vfx/sparkle.png");
    private static final Identifier GLINT = TrophyPedestalsMod.id("textures/vfx/glint.png");
    private static final Identifier HALO = TrophyPedestalsMod.id("textures/vfx/halo.png");

    private record Looks(ParticleSpec spiral, ParticleSpec glow, ParticleSpec star, ParticleSpec spark, ParticleSpec burst,
                         @Nullable SoundEvent ambient) {}

    private static final Map<PedestalRarity, Looks> LOOKS = new HashMap<>();

    private PedestalParticles() {}

    public static void tick(Level level, BlockPos pos, PedestalBlockEntity pedestal) {
        Identifier rank = pedestal.rarity();
        Minecraft minecraft = Minecraft.getInstance();
        if (rank == null || minecraft.player == null) return;
        if (!pedestal.displayed() || PedestalRarities.isNone(rank)) return;
        PedestalRarity rarity = PedestalRarities.get(rank);
        Looks looks = LOOKS.computeIfAbsent(rarity, PedestalParticles::make);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.95 + pedestal.height(), z = pos.getZ() + 0.5;
        long time = level.getGameTime();
        // Its ambient sound, offset by its place so pedestals side by side don't sound together.
        boolean sounds = pedestal.sounds() && TrophyPedestalsClientConfig.AMBIENT_SOUNDS.get();
        if (sounds && looks.ambient != null && (time + pos.asLong()) % rarity.ambientTicks() == 0
                && minecraft.player.distanceToSqr(x, y, z) < SOUND_RANGE * SOUND_RANGE) {
            PedestalRarity.Sound sound = rarity.ambientSound();
            level.playLocalSound(pos, looks.ambient, SoundSource.BLOCKS, sound.volume(), sound.pitch(), false);
        }
        // Everything spreads and rises as far as the pedestal's display scale makes what it shows.
        double size = pedestal.scale();
        // The rest is light: none with its flourishes off (its sounds follow their own setting).
        if (!pedestal.effects() || !TrophyPedestalsClientConfig.FLOURISHES.get() || !PedestalRenderer.lookedAt(pos, (float) size, pedestal.height())) return;
        ParticleStatus status = minecraft.options.particles().get();
        if (status == ParticleStatus.MINIMAL) return;
        boolean all = status == ParticleStatus.ALL;
        RandomSource random = level.getRandom();

        // The spiral: its arms, each a particle at a turning angle now and then, rising and drifting round as it goes,
        // and gone a little above the icon's middle.
        float rate = all ? rarity.spiralRate() : rarity.spiralRate() / 2.0F;
        if (rarity.spiralArms() > 0 && (int) ((time + 1) * rate) > (int) (time * rate)) {
            double angle = time * 0.33;
            for (int arm = 0; arm < rarity.spiralArms(); arm++) {
                double a = angle + arm * Math.PI * 2.0 / rarity.spiralArms();
                double cos = Math.cos(a), sin = Math.sin(a);
                WorldParticles.spawn(looks.spiral, x + cos * SPIRAL_RADIUS * size, y, z + sin * SPIRAL_RADIUS * size,
                        -sin * 0.012 * size, 0.026 * size, cos * 0.012 * size);
            }
        }
        // The projector's glow on the cap, kept up by overlapping soft glows.
        if (time % 6 == 0) WorldParticles.spawn(looks.glow, x, y - 0.02, z, 0.0, 0.004, 0.0);
        if (rarity.glints() > 0.0F && random.nextFloat() < (all ? rarity.glints() : rarity.glints() / 2.0F)) {
            double a = random.nextDouble() * Math.PI * 2.0;
            double r = (0.2 + random.nextDouble() * 0.35) * size;
            WorldParticles.spawn(looks.star, x + Math.cos(a) * r, y + (0.3 + random.nextDouble() * 0.6) * size, z + Math.sin(a) * r, 0.0, 0.012 * size, 0.0);
        }
        // The orbiting sparks: each leaves a short trail of fading sparks (every other tick with Decreased).
        if (rarity.orbiters() > 0 && (all || time % 2 == 0)) {
            double angle = time * 0.16, height = y + 0.42 * size;
            for (int i = 0; i < rarity.orbiters(); i++) {
                double a = angle + i * Math.PI * 2.0 / rarity.orbiters();
                double bob = Math.sin(time * 0.1 + i * 2.0) * 0.12;
                WorldParticles.spawn(looks.spark, x + Math.cos(a) * ORBIT_RADIUS * size, height + bob * size, z + Math.sin(a) * ORBIT_RADIUS * size, 0.0, 0.0, 0.0);
            }
        }
        // The burst: a ring of sparks flung out from the icon, with a chime.
        if (rarity.burstTicks() > 0 && (time + pos.asLong()) % rarity.burstTicks() == 0) {
            int sparks = all ? BURST_SPARKS : BURST_SPARKS / 2;
            for (int i = 0; i < sparks; i++) {
                double a = i * Math.PI * 2.0 / sparks;
                WorldParticles.spawn(looks.burst, x, y + 0.42 * size, z, Math.cos(a) * 0.07 * size, 0.01, Math.sin(a) * 0.07 * size);
            }
            if (sounds) level.playLocalSound(pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6F, 1.6F, false);
        }
        PedestalEffect effect = PedestalEffects.of(rarity);
        if (effect != null) effect.tick(level, pos, rarity);
    }

    private static Looks make(PedestalRarity rarity) {
        int color = rarity.color(), accent = rarity.accent();
        ParticleSpec spiral = ParticleSpec.builder(VfxMaterial.luminous(SPARKLE).withSmoothing(false))
                .lifetime(19, 23).size(Curve.of(0.085F, 0.02F)).opacity(Curve.FADE).color(accent, color).drag(0.985F).build();
        ParticleSpec glow = ParticleSpec.builder(VfxMaterial.luminous(HALO).withSmoothing(false))
                .lifetime(14).size(Curve.constant(0.18F)).opacity(Curve.of(0.0F, 0.16F, 0.0F)).color(color).build();
        ParticleSpec star = ParticleSpec.builder(VfxMaterial.luminous(GLINT).withSmoothing(false))
                .lifetime(20, 32).size(Curve.of(0.0F, 0.06F, 0.0F)).opacity(Curve.FADE).color(0xFFFFFF, accent).build();
        ParticleSpec spark = ParticleSpec.builder(VfxMaterial.luminous(SPARKLE).withSmoothing(false))
                .lifetime(8, 10).size(Curve.of(0.07F, 0.0F)).opacity(Curve.FADE).color(0xFFFFFF, accent).build();
        ParticleSpec burst = ParticleSpec.builder(VfxMaterial.luminous(SPARKLE).withSmoothing(false))
                .lifetime(14, 18).size(Curve.of(0.09F, 0.0F)).opacity(Curve.FADE).color(accent, color).drag(0.9F).build();
        PedestalRarity.Sound ambient = rarity.ambientSound();
        return new Looks(spiral, glow, star, spark, burst, ambient == null ? null : SoundEvent.createVariableRangeEvent(ambient.id()));
    }
}
