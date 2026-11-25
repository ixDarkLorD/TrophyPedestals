package net.ixdarklord.trophypedestals.client.lens;

import net.ixdarklord.trophypedestals.client.PedestalLight;
import net.ixdarklord.trophypedestals.client.PedestalRenderer.State;
import net.ixdarklord.trophypedestals.compat.ARGB;
import net.minecraft.util.Mth;

final class TideLensVisuals extends LensVisuals {
    private static final float RIPPLE_TICKS = 54.0F, RIPPLE_FROM = 0.16F, RIPPLE_TO = 0.56F, RIPPLE_WHITE = 0.35F;
    private static final int TIDE_RINGS = 2;
    private static final float TIDE_RING_TICKS = 70.0F;
    private static final float KELP_FOOT = 14.0F / 16.0F, KELP_HALF = 0.06F;
    private static final float[][] KELP_SPOTS = {{4.9F, 4.9F, 1.0F}, {11.1F, 4.9F, 0.8F}, {4.9F, 11.1F, 0.85F}, {11.1F, 11.1F, 1.05F},
            {4.6F, 7.0F, 0.55F}, {11.4F, 8.8F, 0.62F}, {7.2F, 4.6F, 0.68F}, {9.0F, 11.4F, 0.5F},
            {4.5F, 9.4F, 0.7F}, {11.5F, 6.4F, 0.45F}, {9.6F, 4.5F, 0.42F}, {6.4F, 11.5F, 0.6F}};
    private static final int KELP_FRAMES = 8, TIDE_FISH = 4;

    @Override
    protected void draw(State state, PedestalLight.Glow glow, float up, float lift, float middle, int color) {
        int foam = ARGB.srgbLerp(RIPPLE_WHITE, ARGB.opaque(color), 0xFFFFFFFF);
        for (int ripple = 0; ripple < 2; ripple++) submitRipple(state, glow, lift, ripple / 2.0F, false, foam);
        submitAura(state, glow, color, 50.0F);
        submitTideRings(state, glow, lift, middle, foam);
        submitLensBase(state, glow, PedestalLight.Sprite.LENS_CAUSTIC_0, 0.0F, ARGB.color(170, 0xB8FFF4));
        submitKelp(state, glow);
        submitTideFish(state, glow, middle);
    }

    private static void submitKelp(State state, PedestalLight.Glow glow) {
        for (int frond = 0; frond < KELP_SPOTS.length; frond++) {
            float[] spot = KELP_SPOTS[frond];
            float half = KELP_HALF * spot[2], height = half * 4.0F, ticks = 3.5F + (frond % 3) * 0.7F;
            upright(state, glow, PedestalLight.Sprite.TIDE_KELP_0.frame(((int) (state.time / ticks) + frond * 3) % KELP_FRAMES),
                    spot[0] / 16.0F, KELP_FOOT, spot[1] / 16.0F, frond % 2 == 0 ? half : -half, height, ARGB.color((int) (250 * state.rise), 0xFFFFFF));
        }
    }

    private static void submitTideFish(State state, PedestalLight.Glow glow, float middle) {
        for (int fish = 0; fish < TIDE_FISH; fish++) {
            float direction = fish % 2 == 0 ? 1.0F : -1.0F, angle = state.time * 0.028F * direction + fish * Mth.HALF_PI;
            float radius = (0.42F + 0.04F * Mth.sin(state.time / 23.0F + fish)) * state.size, half = 0.075F * state.size;
            float x = 0.5F + Mth.cos(angle) * radius, z = 0.5F + Mth.sin(angle) * radius;
            float y = middle + (-0.16F + 0.11F * fish) * state.size + 0.025F * Mth.sin(state.time / 11.0F + fish * 2.0F) * state.size;
            // Orbit tangent projected onto the viewer's horizontal axis, for fish facing.
            float across = -Mth.sin(angle) * direction * state.faceX + Mth.cos(angle) * direction * state.faceZ;
            int flick = ((int) (state.time / 3.0F) + fish) % 2;
            upright(state, glow, PedestalLight.Sprite.TIDE_FISH_0.frame((fish % 2) * 2 + flick), x, y - half, z, across >= 0.0F ? half : -half, half * 2.0F, ARGB.color((int) (250 * state.rise), 0xFFFFFF));
        }
    }

    private static void submitTideRings(State state, PedestalLight.Glow glow, float lift, float middle, int foam) {
        float wave = Mth.sin(state.time / 16.0F);
        float swell = 1.0F + 0.1F * Mth.sin(state.time / 9.0F);
        glow.flat(PedestalLight.Sprite.RIPPLE, middle + wave * 0.06F * state.size, 0.3F * state.size * swell, 0.0F,
                ARGB.color((int) (130 * state.rise), foam));
        float top = state.displayTop + state.bob + 0.12F * state.size;
        for (int ring = 0; ring < TIDE_RINGS; ring++) {
            float age = (state.time / TIDE_RING_TICKS + ring / (float) TIDE_RINGS) % 1.0F;
            float y = lift + (top - lift) * age;
            float radius = (0.2F + 0.14F * Mth.sin(age * Mth.PI) + 0.02F * Mth.sin(state.time / 5.0F + ring * 2.0F)) * state.size;
            float fade = Math.min(1.0F, age * 6.0F) * (1.0F - age);
            glow.flat(PedestalLight.Sprite.RIPPLE, y, radius, 0.0F, ARGB.color((int) (200 * fade * state.rise), foam));
        }
    }

    private static void submitRipple(State state, PedestalLight.Glow glow, float y, float offset, boolean inward, int color) {
        float age = (state.time / RIPPLE_TICKS + offset) % 1.0F;
        if (inward) age = 1.0F - age;
        float fade = inward ? age * Math.min(1.0F, (1.0F - age) * 5.0F) : (1.0F - age) * Math.min(1.0F, age * 5.0F);
        glow.flat(PedestalLight.Sprite.RIPPLE, y, RIPPLE_FROM + (RIPPLE_TO - RIPPLE_FROM) * age, 0.0F,
                ARGB.color((int) (255 * fade * state.rise), color));
    }
}
