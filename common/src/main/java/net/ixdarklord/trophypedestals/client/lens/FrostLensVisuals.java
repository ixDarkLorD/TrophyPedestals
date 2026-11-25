package net.ixdarklord.trophypedestals.client.lens;

import net.ixdarklord.trophypedestals.client.PedestalLight;
import net.ixdarklord.trophypedestals.client.PedestalRenderer.State;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

final class FrostLensVisuals extends LensVisuals {
    private static final int FROST_GUSTS = 3, GUST_STEPS = 14;
    private static final float ICE_CORNER = 0.23F, ICE_FOOT = 15.0F / 16.0F, ICE_HALF = 0.09F;
    private static final float COLD_TOP = 0.77F, COLD_BOTTOM = 0.03F, COLD_OUT = 0.04F;
    private static final float ICICLE_TOP = 0.752F, ICICLE_HEIGHT = 0.25F, ICICLE_INSET = 1.0F / 16.0F;

    @Override
    protected void draw(State state, PedestalLight.Glow glow, float up, float lift, float middle, int color) {
        glow.flat(PedestalLight.Sprite.FROST, lift, LENS_DISC, 0.0F, ARGB.color((int) (120 * state.rise), state.rarity.accent()));
        float breath = 0.5F + 0.5F * Mth.sin(state.time / 30.0F);
        glow.flat(PedestalLight.Sprite.MIST, lift + 0.01F, 0.3F + 0.04F * breath, state.time * 0.004F,
                ARGB.color((int) ((14 + 14 * breath) * state.rise), 0xE8F8FF));
        submitAura(state, glow, color, 45.0F);
        submitSnowCap(state, glow);
        submitColdFog(state, glow);
        submitIcicles(state, glow);
        submitLensBase(state, glow, PedestalLight.Sprite.LENS_FROST_0, 0.0F, ARGB.color(230, 0xD8F4FF));
        submitIceClusters(state, glow);
        submitFrostGusts(state, glow, lift);
    }

    private static void submitSnowCap(State state, PedestalLight.Glow glow) {
        int color = ARGB.color((int) (255 * state.rise), 0xFFFFFF);
        glow.flat(PedestalLight.Sprite.FROST_WELL, 14.06F / 16.0F, 4.0F / 16.0F, 0.0F, color);
        glow.flat(PedestalLight.Sprite.FROST_LEDGE, 14.03F / 16.0F, 6.0F / 16.0F, 0.0F, color);
        glow.flat(PedestalLight.Sprite.FROST_RIM, 15.03F / 16.0F, 5.0F / 16.0F, 0.0F, color);
        snowSides(glow, PedestalLight.Sprite.FROST_SIDE, 6.0F / 16.0F + 0.002F, 14.02F / 16.0F, 11.9F / 16.0F, color);
        snowSides(glow, PedestalLight.Sprite.FROST_LIP, 5.0F / 16.0F + 0.002F, 15.03F / 16.0F, 14.0F / 16.0F, color);
        snowSides(glow, PedestalLight.Sprite.FROST_LIP, 4.0F / 16.0F - 0.002F, 15.03F / 16.0F, 14.0F / 16.0F, color);
    }

    private static void snowSides(PedestalLight.Glow glow, PedestalLight.Sprite sprite, float half, float top, float bottom, int color) {
        float lo = 0.5F - half, hi = 0.5F + half;
        glow.sprite(sprite, lo, top, lo, lo, bottom, lo, hi, bottom, lo, hi, top, lo, color);
        glow.sprite(sprite, hi, top, lo, hi, bottom, lo, hi, bottom, hi, hi, top, hi, color);
        glow.sprite(sprite, hi, top, hi, hi, bottom, hi, lo, bottom, hi, lo, top, hi, color);
        glow.sprite(sprite, lo, top, hi, lo, bottom, hi, lo, bottom, lo, lo, top, lo, color);
    }

    private static void submitColdFog(State state, PedestalLight.Glow glow) {
        coldSheet(state, glow, 0.5F, COLD_OUT * 0.45F, 58);
        coldSheet(state, glow, 0.0F, COLD_OUT, 118);
    }

    private static void coldSheet(State state, PedestalLight.Glow glow, float share, float out, int alpha) {
        PedestalLight.Sprite sprite = PedestalLight.Sprite.COLD_FALL_0.playing(state.time, share);
        int color = ARGB.color((int) (alpha * state.rise), 0xFFFFFF);
        float lo = CAP_LOW - out, hi = CAP_HIGH + out, top = COLD_TOP, bottom = COLD_BOTTOM;
        glow.sprite(sprite, lo, top, lo, lo, bottom, lo, hi, bottom, lo, hi, top, lo, color);
        glow.sprite(sprite, hi, top, lo, hi, bottom, lo, hi, bottom, hi, hi, top, hi, color);
        glow.sprite(sprite, hi, top, hi, hi, bottom, hi, lo, bottom, hi, lo, top, hi, color);
        glow.sprite(sprite, lo, top, hi, lo, bottom, hi, lo, bottom, lo, lo, top, lo, color);
    }

    private static void submitIceClusters(State state, PedestalLight.Glow glow) {
        float height = ICE_HALF * 2.0F;
        for (int corner = 0; corner < 4; corner++) {
            float x = (corner & 1) == 0 ? ICE_CORNER : 1.0F - ICE_CORNER, z = (corner & 2) == 0 ? ICE_CORNER : 1.0F - ICE_CORNER;
            upright(state, glow, PedestalLight.Sprite.FROST_CRYSTAL_0.frame(corner % 3), x, ICE_FOOT, z, ICE_HALF, height, ARGB.color((int) (245 * state.rise), 0xFFFFFF));
            float flash = Mth.sin(state.time / 19.0F + corner * 1.7F);
            if (flash > 0.85F) {
                float bright = (flash - 0.85F) / 0.15F;
                glow.billboard(PedestalLight.Sprite.APPRAISAL_GLARE, x, ICE_FOOT + height * 0.7F, z, 0.12F * bright, 0.0F, ARGB.color((int) (220 * bright * state.rise), 0xE8F8FF));
            }
        }
    }

    private static void submitFrostGusts(State state, PedestalLight.Glow glow, float lift) {
        for (int gust = 0; gust < FROST_GUSTS; gust++) {
            float life = 0.5F + 0.5F * Mth.sin(state.time / 29.0F + gust * 2.1F);
            if (life < 0.05F) continue;
            float head = state.time * 0.075F + gust * Mth.TWO_PI / FROST_GUSTS;
            float height = lift + (0.16F + 0.2F * gust) * state.size + 0.03F * Mth.sin(state.time / 13.0F + gust * 2.0F) * state.size;
            float radius = (0.44F + 0.03F * Mth.sin(state.time / 17.0F + gust)) * state.size;
            for (int step = 0; step < GUST_STEPS; step++) {
                float t0 = step / (float) GUST_STEPS, t1 = (step + 1) / (float) GUST_STEPS;
                // Gust is a ring arc trailing behind its head, with a sine ripple in height.
                float a0 = head - (1.0F - t0) * 1.3F, a1 = head - (1.0F - t1) * 1.3F;
                float x0 = 0.5F + Mth.cos(a0) * radius, z0 = 0.5F + Mth.sin(a0) * radius, y0 = height + Mth.sin(a0 * 2.0F + gust) * 0.02F * state.size;
                float x1 = 0.5F + Mth.cos(a1) * radius, z1 = 0.5F + Mth.sin(a1) * radius, y1 = height + Mth.sin(a1 * 2.0F + gust) * 0.02F * state.size;
                float w0 = 0.012F * t0 * state.size, w1 = 0.012F * t1 * state.size;
                int c0 = ARGB.color((int) (t0 * t0 * 190 * life * state.rise), 0xE8F8FF), c1 = ARGB.color((int) (t1 * t1 * 190 * life * state.rise), 0xE8F8FF);
                glow.quad(x0, y0 - w0, z0, x0, y0 + w0, z0, x1, y1 + w1, z1, x1, y1 - w1, z1, c0, c0, c1, c1);
            }
        }
    }

    private static void submitIcicles(State state, PedestalLight.Glow glow) {
        submitCapBand(state, glow, PedestalLight.Sprite.ICICLES, ICICLE_TOP, ICICLE_TOP - ICICLE_HEIGHT, ICICLE_INSET, ARGB.color((int) (235 * state.rise), 0xFFFFFF));
    }
}
