package net.ixdarklord.trophypedestals.client.lens;

import net.ixdarklord.trophypedestals.client.LensLooks;
import net.ixdarklord.trophypedestals.client.PedestalLight;
import net.ixdarklord.trophypedestals.client.PedestalRenderer.State;
import net.ixdarklord.trophypedestals.pedestal.Lens;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/** The Prism Lens: a fan of rainbow rays, a turning halo of stained glass and little prisms circling the item. */
final class PrismLensVisuals extends LensVisuals {
    private static final float PRISM_HALO_TURN = 0.006F, HALO_FLIGHT = 0.34F, HALO_TRAIL = 0.17F;
    private static final int HALO_STREAKS = 5, HALO_SHARDS = 12;
    private static final int[] HALO_COLORS = {0xF0424A, 0xFA9230, 0xFCE24C, 0x56D264, 0x4692F4, 0x9C5AE8};
    private static final int PRISM_CRYSTALS = 5, PRISM_TRAIL = 14;

    @Override
    protected void draw(State state, PedestalLight.Glow glow, float up, float lift, float middle, int color) {
        submitLensBase(state, glow, PedestalLight.Sprite.LENS_PRISM_0, state.time * 0.006F, 0xE6FFFFFF);
        submitAura(state, glow, color, 80.0F);
        submitPrismFan(state, glow, lift);
        submitPrismHalo(state, glow, lift, middle);
        submitPrismCrystals(state, glow, middle);
    }

    /** The Prism Lens: a halo of stained glass shards in the colours of the spectrum behind the item, turning slowly with light passing round it pane by pane and a bloom and flare at its heart. */
    private static void submitPrismHalo(State state, PedestalLight.Glow glow, float lift, float middle) {
        float half = 0.44F * state.size, center = Math.max(middle, lift + half * 0.97F);
        float x = 0.5F + state.backX * CIRCLE_BACK * state.size, z = 0.5F + state.backZ * CIRCLE_BACK * state.size;
        // turning slowly in the plane facing the camera
        float roll = state.time * PRISM_HALO_TURN;
        haloQuad(state, glow, PedestalLight.Sprite.PRISM_HALO_0.playing(state.time), x, center, z, half, roll,
                ARGB.color((int) (245 * state.rise), 0xFFFFFF));
        // a soft bloom and a level flare at its heart, in the colour the light has reached, behind the item
        float breath = 0.82F + 0.18F * Mth.sin(state.time / 15.0F);
        int hue = ARGB.srgbLerp(0.25F, ARGB.opaque(LensLooks.prism(state.time, 0.0F)), 0xFFFFFFFF);
        glow.billboard(PedestalLight.Sprite.APPRAISAL_BLOOM, x, center, z, 0.25F * breath * state.size, 0.0F, ARGB.color((int) (135 * state.rise), hue));
        glow.billboard(PedestalLight.Sprite.APPRAISAL_BLOOM, x, center, z, 0.11F * breath * state.size, 0.0F, ARGB.color((int) (175 * state.rise), 0xFFFFFF));
        glow.billboard(PedestalLight.Sprite.APPRAISAL_GLARE, x, center, z, 0.28F * breath * state.size, 0.0F, ARGB.color((int) (150 * state.rise), hue));
        for (int streak = 0; streak < HALO_STREAKS; streak++) submitHaloStreak(state, glow, x, center, z, half, roll, streak);
    }

    /**
     * A streak of light shot from the halo's heart through a random shard, at a random moment of each round, trailing
     * behind it. The shard flashes with a quick glint as it passes, and the streak fades away beyond the halo.
     */
    private static void submitHaloStreak(State state, PedestalLight.Glow glow, float x, float y, float z, float half, float roll, int streak) {
        float period = 58.0F + streak * 13.0F, round = state.time / period + streak * 0.37F;
        int count = Mth.floor(round), seed = count * 73856093 ^ streak * 19349663;
        seed ^= seed >>> 13;
        seed *= 0x5BD1E995;
        seed ^= seed >>> 15;
        float p = (round - count - (seed >>> 8 & 255) / 255.0F * 0.55F) / 0.45F;
        if (p < 0.0F || p > 1.0F) return;
        int shard = Math.floorMod(seed, HALO_SHARDS), tint = HALO_COLORS[shard % HALO_COLORS.length];
        float angle = roll - (shard + 0.5F) * Mth.TWO_PI / HALO_SHARDS, cos = Mth.cos(angle), sin = Mth.sin(angle);
        float far = half * 1.14F, passes = HALO_FLIGHT * 0.58F / 1.14F, head = far * Math.min(1.0F, p / HALO_FLIGHT), tail = far * Mth.clamp((p - HALO_TRAIL) / HALO_FLIGHT, 0.0F, 1.0F);
        float fade = 1.0F - easeInOut(Mth.clamp((p - passes) / (HALO_FLIGHT + HALO_TRAIL - passes), 0.0F, 1.0F));
        if (fade > 0.0F && head > tail) {
            haloLine(state, glow, x, y, z, cos, sin, tail, head, 0.015F * state.size, ARGB.color((int) (215 * fade * state.rise), tint));
            haloLine(state, glow, x, y, z, cos, sin, tail + (head - tail) * 0.4F, head, 0.006F * state.size, ARGB.color((int) (255 * fade * state.rise), 0xFFFFFF));
        }
        if (p < passes) return;
        float after = Math.min(1.0F, (p - passes) / 0.3F), flash = (1.0F - after) * (1.0F - after);
        int base = shard % 2;
        haloQuad(state, glow, PedestalLight.Sprite.PRISM_HALO_FLASH_0.frame(base), x, y, z, half, roll - (shard - base) * Mth.TWO_PI / HALO_SHARDS,
                ARGB.color((int) (255 * flash * state.rise), 0xFFFFFF));
        float at = half * 0.72F;
        glow.billboard(PedestalLight.Sprite.STAR_GLINT, x + state.faceX * cos * at, y + sin * at, z + state.faceZ * cos * at,
                0.1F * state.size * flash, state.time * 0.04F, ARGB.color((int) (255 * flash * state.rise), 0xFFFFFF));
    }

    /** A thin line of light across the halo's plane, from {@code from} to {@code to} out along a direction, fading away behind it. */
    private static void haloLine(State state, PedestalLight.Glow glow, float x, float y, float z, float cos, float sin, float from, float to, float width, int color) {
        float fromSide = cos * from, fromUp = sin * from, side = cos * to, up = sin * to, offSide = -sin * width, offUp = cos * width;
        int clear = ARGB.color(0, color);
        glow.quad(x + state.faceX * fromSide, y + fromUp, z + state.faceZ * fromSide, x + state.faceX * fromSide, y + fromUp, z + state.faceZ * fromSide,
                x + state.faceX * (side + offSide), y + up + offUp, z + state.faceZ * (side + offSide),
                x + state.faceX * (side - offSide), y + up - offUp, z + state.faceZ * (side - offSide), clear, clear, color, color);
    }

    /** The Prism Lens: three little glass gems, a triangle, a cut diamond and a hexagon, circling the item, each drawing a ribbon of the spectrum behind it. */
    private static void submitPrismCrystals(State state, PedestalLight.Glow glow, float middle) {
        float radius = 0.44F * state.size, rise = 0.15F * state.size, width = 0.013F * state.size;
        for (int crystal = 0; crystal < PRISM_CRYSTALS; crystal++) {
            float head = state.time * 0.032F + crystal * Mth.TWO_PI / PRISM_CRYSTALS;
            for (int step = 0; step < PRISM_TRAIL; step++) {
                float t0 = step / (float) PRISM_TRAIL, t1 = (step + 1) / (float) PRISM_TRAIL;
                float a0 = head - (1.0F - t0) * 1.05F, a1 = head - (1.0F - t1) * 1.05F;
                int c0 = ARGB.color((int) (t0 * t0 * 200 * state.rise), LensLooks.prism(state.time, t0)), c1 = ARGB.color((int) (t1 * t1 * 200 * state.rise), LensLooks.prism(state.time, t1));
                float w0 = width * t0, w1 = width * t1;
                glow.quad(0.5F + Mth.cos(a0) * radius, middle + Mth.sin(a0) * rise - w0, 0.5F + Mth.sin(a0) * radius, 0.5F + Mth.cos(a0) * radius, middle + Mth.sin(a0) * rise + w0, 0.5F + Mth.sin(a0) * radius,
                        0.5F + Mth.cos(a1) * radius, middle + Mth.sin(a1) * rise + w1, 0.5F + Mth.sin(a1) * radius, 0.5F + Mth.cos(a1) * radius, middle + Mth.sin(a1) * rise - w1, 0.5F + Mth.sin(a1) * radius, c0, c0, c1, c1);
            }
            int hue = ARGB.srgbLerp(0.3F, ARGB.opaque(LensLooks.prism(state.time, crystal / (float) PRISM_CRYSTALS)), 0xFFFFFFFF);
            glow.billboard(PedestalLight.Sprite.PRISM_CRYSTAL.frame(crystal), 0.5F + Mth.cos(head) * radius, middle + Mth.sin(head) * rise, 0.5F + Mth.sin(head) * radius, 0.06F * state.size, 0.0F,
                    ARGB.color((int) (255 * state.rise), hue));
        }
    }

    /** The prism splits the light: a slowly turning fan of rainbow rays rising from the lens. */
    private static void submitPrismFan(State state, PedestalLight.Glow glow, float floor) {
        int rays = 7;
        for (int ray = 0; ray < rays; ray++) {
            float angle = state.time * 0.012F + ray * Mth.TWO_PI / rays;
            float reach = 0.5F * state.size, rise = 0.8F * state.size;
            float tx = 0.5F + Mth.cos(angle) * reach, tz = 0.5F + Mth.sin(angle) * reach, ty = floor + rise;
            float pulse = 0.6F + 0.4F * Mth.sin(state.time * 0.05F + ray * 1.7F);
            int hue = LensLooks.prism(state.time, ray / (float) rays);
            int foot = ARGB.color((int) (130 * pulse * state.rise), hue), end = ARGB.color(0, hue);
            float foot0 = 0.01F, top = 0.05F * state.size;
            float fx = state.faceX, fz = state.faceZ;
            glow.quad(0.5F - fx * foot0, floor, 0.5F - fz * foot0, 0.5F + fx * foot0, floor, 0.5F + fz * foot0,
                    tx + fx * top, ty, tz + fz * top, tx - fx * top, ty, tz - fz * top, foot, foot, end, end);
        }
    }
}
