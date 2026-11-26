package net.ixdarklord.trophypedestals.client.lens;

import net.ixdarklord.trophypedestals.client.PedestalLight;
import net.ixdarklord.trophypedestals.client.PedestalRenderer;
import net.ixdarklord.trophypedestals.client.PedestalRenderer.State;
import net.minecraft.util.ARGB;

final class HologramLensVisuals extends LensVisuals {
    private static final float SCAN_TICKS = 66.0F, SCAN_TRAIL = 0.14F;
    private static final int SCAN_LINES = 3;
    private static final float HOLO_RAIN_HEIGHT = 26.0F / 64.0F;

    @Override
    protected void draw(State state, PedestalLight.Glow glow, float up, float lift, float middle, int color) {
        float flicker = state.time % 53.0F < 2.0F ? 0.5F : 1.0F;
        glow.flat(PedestalLight.Sprite.GRID, lift, PedestalRenderer.FRAME_HALF - 0.01F, 0.0F, ARGB.color((int) (170 * flicker * state.rise), state.rarity.accent()));
        submitScanlines(state, glow, up, flicker);
        submitProjection(state, glow, up, flicker);
        submitCapBand(state, glow, PedestalLight.Sprite.HOLO_0.playing(state.time), CAP_SIDE_MIDDLE + HOLO_RAIN_HEIGHT, CAP_SIDE_MIDDLE,
                ARGB.color((int) (230 * flicker * state.rise), 0xFFFFFF));
    }

    @Override
    protected void light(State state, PedestalLight.Glow glow, float up, float lift, float middle, int color) {
        // the projector's light, and the projection's, stuttering with it
        float flicker = state.time % 53.0F < 2.0F ? 0.5F : 1.0F;
        lensLight(state, glow, up, color, 0.65F * flicker);
        glow.light(0.5F, middle, 0.5F, color, 0.35F * flicker * state.rise, 3.5F * state.size);
    }

    private static void submitProjection(State state, PedestalLight.Glow glow, float floor, float flicker) {
        float middle = state.iconY + state.middle + state.bob;
        float lo = 0.5F - PedestalRenderer.FRAME_HALF + 0.02F, hi = 0.5F + PedestalRenderer.FRAME_HALF - 0.02F;
        float[][] corners = {{lo, lo}, {hi, lo}, {hi, hi}, {lo, hi}};
        int foot = ARGB.color((int) (110 * flicker * state.rise), state.rarity.accent()), end = ARGB.color(0, state.rarity.accent());
        float wide = 0.012F;
        for (float[] corner : corners) {
            float tx = 0.5F + (corner[0] - 0.5F) * 0.25F * state.size, tz = 0.5F + (corner[1] - 0.5F) * 0.25F * state.size;
            float sx = state.faceX * wide, sz = state.faceZ * wide;
            glow.quad(corner[0] - sx, floor, corner[1] - sz, corner[0] + sx, floor, corner[1] + sz, tx + sx * 0.3F * state.size, middle, tz + sz * 0.3F * state.size,
                    tx - sx * 0.3F * state.size, middle, tz - sz * 0.3F * state.size, foot, foot, end, end);
        }
    }

    private static void submitScanlines(State state, PedestalLight.Glow glow, float floor, float flicker) {
        float cone = Math.max(PedestalRenderer.coneTop(floor, state.rarity, state.size, state.shownRise) - floor, 0.01F);
        float top = Math.max(floor + 0.25F * state.size, state.displayTop + state.bob + 0.05F * state.size);
        int accent = state.rarity.accent();
        for (int line = 0; line < SCAN_LINES; line++) {
            float t = (state.time / SCAN_TICKS + line / (float) SCAN_LINES) % 1.0F;
            // Smoothstep easing so each scan line slows at both ends.
            float y = floor + (top - floor) * t * t * (3.0F - 2.0F * t);
            float fade = Math.min(1.0F, t * 7.0F) * (float) Math.pow(1.0F - t, 1.35F) * flicker * state.rise;
            if (fade <= 0.0F) continue;
            float half = PedestalRenderer.CONE_FOOT + (PedestalRenderer.CONE_TOP * state.size - PedestalRenderer.CONE_FOOT) * Math.min(1.0F, (y - floor) / cone);
            float lo = 0.5F - half, hi = 0.5F + half, bottom = Math.max(floor, y - SCAN_TRAIL * state.size);
            glow.flat(PedestalLight.Sprite.HOLO_RING, y, half, 0.0F, ARGB.color((int) (235 * fade), accent));
            int band = ARGB.color((int) (225 * fade), accent);
            glow.sprite(PedestalLight.Sprite.HOLO_BAND, lo, y, lo, lo, bottom, lo, hi, bottom, lo, hi, y, lo, band);
            glow.sprite(PedestalLight.Sprite.HOLO_BAND, hi, y, lo, hi, bottom, lo, hi, bottom, hi, hi, y, hi, band);
            glow.sprite(PedestalLight.Sprite.HOLO_BAND, hi, y, hi, hi, bottom, hi, lo, bottom, hi, lo, y, hi, band);
            glow.sprite(PedestalLight.Sprite.HOLO_BAND, lo, y, hi, lo, bottom, hi, lo, bottom, lo, lo, y, lo, band);
        }
    }
}
