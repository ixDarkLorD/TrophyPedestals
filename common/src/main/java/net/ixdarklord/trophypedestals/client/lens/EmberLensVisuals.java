package net.ixdarklord.trophypedestals.client.lens;

import net.ixdarklord.trophypedestals.client.LensLooks;
import net.ixdarklord.trophypedestals.client.PedestalLight;
import net.ixdarklord.trophypedestals.client.PedestalRenderer;
import net.ixdarklord.trophypedestals.client.PedestalRenderer.State;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

final class EmberLensVisuals extends LensVisuals {
    private static final float FLAME_RING = 0.21F;
    private static final float HEAT_HALF = 0.48F, HEAT_HEIGHT = 1.12F, HEAT_DEPTH = 0.16F;
    private static final float CAP_SIDE_BOTTOM = 12.0F / 16.0F, FIRE_BOTTOM = CAP_SIDE_MIDDLE, FIRE_HEIGHT = 0.375F;
    private static final float FIRE_BASE_HEIGHT = 4.0F / 64.0F;

    @Override
    protected void draw(State state, PedestalLight.Glow glow, float up, float lift, float middle, int color) {
        float flicker = LensLooks.flicker(state.time, state.blockPos);
        submitAura(state, glow, color, 60.0F + 50.0F * flicker);
        glow.lens(lift, PedestalRenderer.LENS_GLOW_RADIUS * 0.8F, ARGB.color((int) ((50 + 60 * flicker) * state.rise), 0xFFB040), PedestalRenderer.LENS_GLOW_SEGMENTS);
        submitFlames(state, glow, up - 0.015F);
        submitHeatHaze(state, glow, lift, flicker);
        submitCapBand(state, glow, PedestalLight.Sprite.COALS, CAP_TOP_EDGE, CAP_SIDE_BOTTOM, ARGB.color((int) (250 * state.rise), 0xFFFFFF));
        submitCapBand(state, glow, PedestalLight.Sprite.FIRE_BASE, FIRE_BOTTOM, FIRE_BOTTOM - FIRE_BASE_HEIGHT, ARGB.color((int) (255 * state.rise), 0xFFFFFF));
        submitCapBand(state, glow, PedestalLight.Sprite.FIRE_0.playing(state.time), FIRE_BOTTOM + FIRE_HEIGHT, FIRE_BOTTOM,
                ARGB.color((int) ((185 + 70 * flicker) * state.rise), 0xFFFFFF));
    }

    @Override
    protected void light(State state, PedestalLight.Glow glow, float up, float lift, float middle, int color) {
        // the flames' firelight, flickering as they do, and the burning band round the cap below them
        float flicker = LensLooks.flicker(state.time, state.blockPos);
        glow.light(0.5F, up + 0.3F, 0.5F, 0xFF9A3C, (0.75F + 0.5F * flicker) * state.rise, 4.2F);
        glow.light(0.5F, FIRE_BOTTOM, 0.5F, 0xFF7A20, (0.45F + 0.3F * flicker) * state.rise, 2.8F);
    }

    private static void submitHeatHaze(State state, PedestalLight.Glow glow, float floor, float flicker) {
        float breath = 1.0F + 0.035F * Mth.sin(state.time / 13.0F);
        heatLayer(state, glow, floor, 0.0F, HEAT_DEPTH, breath, (int) ((165 + 50 * flicker) * state.rise));
        heatLayer(state, glow, floor, 1.0F / 3.0F, 0.0F, 0.88F / breath, (int) ((105 + 35 * flicker) * state.rise));
        heatLayer(state, glow, floor, 2.0F / 3.0F, -HEAT_DEPTH, 0.76F * breath, (int) ((80 + 30 * flicker) * state.rise));
    }

    private static void heatLayer(State state, PedestalLight.Glow glow, float floor, float share, float depth, float scale, int alpha) {
        float half = HEAT_HALF * state.size * scale, top = floor + HEAT_HEIGHT * state.size * scale;
        float x = 0.5F + state.backX * depth * state.size, z = 0.5F + state.backZ * depth * state.size;
        float fx = state.faceX * half, fz = state.faceZ * half;
        glow.sprite(PedestalLight.Sprite.HEAT_0.playing(state.time, share), x - fx, top, z - fz, x - fx, floor, z - fz, x + fx, floor, z + fz, x + fx, top, z + fz,
                ARGB.color(alpha, 0xFFFFFF));
    }

    private static void submitFlames(State state, PedestalLight.Glow glow, float floor) {
        int flames = 8;
        for (int flame = 0; flame < flames; flame++) {
            float angle = flame * Mth.TWO_PI / flames + state.time * 0.004F;
            float t = state.time + flame * 9.7F;
            // Two out-of-phase sines make the flame height flicker unevenly.
            float lick = 0.6F + 0.25F * Mth.sin(t * 0.43F) + 0.15F * Mth.sin(t * 1.37F + flame);
            float height = 0.08F + 0.07F * lick, half = 0.03F + 0.006F * lick;
            float x = 0.5F + Mth.cos(angle) * FLAME_RING, z = 0.5F + Mth.sin(angle) * FLAME_RING;
            float sway = Mth.sin(t * 0.21F) * 0.012F;
            float fx = state.faceX * half, fz = state.faceZ * half;
            glow.sprite(PedestalLight.Sprite.FLAME, x - fx + sway * state.faceX, floor + height, z - fz + sway * state.faceZ, x - fx, floor, z - fz,
                    x + fx, floor, z + fz, x + fx + sway * state.faceX, floor + height, z + fz + sway * state.faceZ,
                    ARGB.color((int) ((170 + 85 * lick) * state.rise), 0xFFFFFF));
        }
    }
}
