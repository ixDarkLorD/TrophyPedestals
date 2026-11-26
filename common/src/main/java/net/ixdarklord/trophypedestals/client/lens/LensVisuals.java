package net.ixdarklord.trophypedestals.client.lens;

import net.ixdarklord.trophypedestals.client.PedestalLight;
import net.ixdarklord.trophypedestals.client.PedestalRenderer;
import net.ixdarklord.trophypedestals.client.PedestalRenderer.State;
import net.ixdarklord.trophypedestals.pedestal.Lens;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

import java.util.EnumMap;
import java.util.Map;

public abstract class LensVisuals {
    protected static final float AURA_SIZE = 0.42F, AURA_PULSE = 0.07F, AURA_TICKS = 30.0F;
    public static final float RIPPLE_LIFT = 0.06F;
    protected static final float LENS_DISC = 0.235F, CIRCLE_BACK = 0.22F;
    protected static final float LENS_BASE_LIFT = 0.004F, LENS_BASE_HALF = 0.17F;
    protected static final float CAP_TOP_EDGE = 14.0F / 16.0F, CAP_BOTTOM_EDGE = 10.0F / 16.0F;
    protected static final float CAP_SIDE_MIDDLE = 13.0F / 16.0F;
    protected static final float CAP_LOW = 2.0F / 16.0F, CAP_HIGH = 14.0F / 16.0F;

    private static final class Registry {
        private static final Map<Lens, LensVisuals> VISUALS = new EnumMap<>(Lens.class);

        static {
            VISUALS.put(Lens.CLEAR, new ClearLensVisuals());
            VISUALS.put(Lens.SANGUINE, new SanguineLensVisuals());
            VISUALS.put(Lens.PRISM, new PrismLensVisuals());
            VISUALS.put(Lens.TIDE, new TideLensVisuals());
            VISUALS.put(Lens.APPRAISAL, new AppraisalLensVisuals());
            VISUALS.put(Lens.EMBER, new EmberLensVisuals());
            VISUALS.put(Lens.FROST, new FrostLensVisuals());
            VISUALS.put(Lens.ENDER, new EnderLensVisuals());
            VISUALS.put(Lens.HOLOGRAM, new HologramLensVisuals());
        }
    }

    public static void submit(State state, PedestalLight.Glow glow, float up) {
        float lift = up + RIPPLE_LIFT;
        float middle = state.iconY + state.middle + state.bob;
        LensVisuals visuals = Registry.VISUALS.get(state.lens);
        visuals.draw(state, glow, up, lift, middle, state.rarity.color());
        visuals.light(state, glow, up, lift, middle, state.rarity.color());
    }

    /**
     * Only the light the lens fitted to this pedestal gives off, none of its effects: for a pedestal too far off for them
     * to be drawn, whose light still falls on the world around it as it would up close.
     */
    public static void submitLight(State state, PedestalLight.Glow glow, float up) {
        float lift = up + RIPPLE_LIFT;
        float middle = state.iconY + state.middle + state.bob;
        Registry.VISUALS.get(state.lens).light(state, glow, up, lift, middle, state.rarity.color());
    }

    protected abstract void draw(State state, PedestalLight.Glow glow, float up, float lift, float middle, int color);

    /**
     * The light the lens gives off onto the world around it (see {@link PedestalLight.Glow#light}), from what it draws:
     * given after it's drawn, and alone for a pedestal too far off for its effects to be drawn, so it lights the same
     * near or far. A lens gives off a glow of its colour from its glass unless it says otherwise.
     */
    protected void light(State state, PedestalLight.Glow glow, float up, float lift, float middle, int color) {
        lensLight(state, glow, up, color, 0.6F);
    }

    /** How far a lens's own glow reaches onto the blocks around, in blocks. */
    protected static final float LENS_LIGHT_REACH = 3.2F;

    /**
     * The light a lens's glass gives off onto the world around the pedestal (see {@link PedestalLight.Glow#light}): from
     * just above the glass, of the lens's colour, as strong as {@code strength} while the display is up.
     */
    protected static void lensLight(State state, PedestalLight.Glow glow, float up, int color, float strength) {
        glow.light(0.5F, up + 0.1F, 0.5F, color, strength * state.rise, LENS_LIGHT_REACH * Math.max(1.0F, state.size));
    }

    protected static void upright(State state, PedestalLight.Glow glow, PedestalLight.Sprite sprite, float x, float bottom, float z, float half, float height, int color) {
        float fx = state.faceX * half, fz = state.faceZ * half, top = bottom + height;
        glow.sprite(sprite, x - fx, top, z - fz, x - fx, bottom, z - fz, x + fx, bottom, z + fz, x + fx, top, z + fz, color);
    }

    protected static float easeInOut(float t) {
        return t * t * (3.0F - 2.0F * t);
    }

    protected static void submitLensBase(State state, PedestalLight.Glow glow, PedestalLight.Sprite first, float roll, int color) {
        glow.flat(first.playing(state.time), PedestalRenderer.GLASS_TOP + LENS_BASE_LIFT, LENS_BASE_HALF, roll, ARGB.color((int) (ARGB.alpha(color) * state.rise), color));
    }

    protected static void submitCapBand(State state, PedestalLight.Glow glow, PedestalLight.Sprite texture, float top, float bottom, int color) {
        submitCapBand(state, glow, texture, top, bottom, -0.003F, color);
    }

    protected static void submitCapBand(State state, PedestalLight.Glow glow, PedestalLight.Sprite texture, float top, float bottom, float inset, int color) {
        float lo = CAP_LOW + inset, hi = CAP_HIGH - inset;
        glow.sprite(texture, lo, top, lo, lo, bottom, lo, hi, bottom, lo, hi, top, lo, color);
        glow.sprite(texture, hi, top, lo, hi, bottom, lo, hi, bottom, hi, hi, top, hi, color);
        glow.sprite(texture, hi, top, hi, hi, bottom, hi, lo, bottom, hi, lo, top, hi, color);
        glow.sprite(texture, lo, top, hi, lo, bottom, hi, lo, bottom, lo, lo, top, lo, color);
    }

    protected static void submitAura(State state, PedestalLight.Glow glow, int color, float alpha) {
        float pulse = 1.0F + AURA_PULSE * Mth.sin(state.time * Mth.TWO_PI / AURA_TICKS);
        float y = state.iconY + state.middle + state.bob;
        glow.aura(y, AURA_SIZE * state.size * pulse, ARGB.color((int) (alpha * pulse * state.rise), color));
    }

    protected static void haloQuad(State state, PedestalLight.Glow glow, PedestalLight.Sprite sprite, float x, float y, float z, float half, float roll, int color) {
        // Corners of a square rotated by roll, spread along the face's horizontal axis.
        float cos = Mth.cos(roll) * half, sin = Mth.sin(roll) * half, ax = -cos - sin, ay = cos - sin, bx = -cos + sin, by = -cos - sin;
        glow.sprite(sprite, x + state.faceX * ax, y + ay, z + state.faceZ * ax, x + state.faceX * bx, y + by, z + state.faceZ * bx,
                x - state.faceX * ax, y - ay, z - state.faceZ * ax, x - state.faceX * bx, y - by, z - state.faceZ * bx, color);
    }
}
