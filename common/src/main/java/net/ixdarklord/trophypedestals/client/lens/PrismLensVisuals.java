package net.ixdarklord.trophypedestals.client.lens;

import net.ixdarklord.trophypedestals.client.LensLooks;
import net.ixdarklord.trophypedestals.client.PedestalLight;
import net.ixdarklord.trophypedestals.client.PedestalRenderer.State;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class PrismLensVisuals extends LensVisuals {
    private static final float PRISM_HALO_TURN = 0.006F, HALO_FLIGHT = 0.34F, HALO_TRAIL = 0.17F;
    private static final int HALO_STREAKS = 5;
    /** How many panes of glass the halo has. */
    public static final int HALO_SHARDS = 12;
    private static final int[] HALO_COLORS = {0xF0424A, 0xFA9230, 0xFCE24C, 0x56D264, 0x4692F4, 0x9C5AE8};
    private static final int PRISM_CRYSTALS = 5, PRISM_TRAIL = 14;

    @Override
    protected void draw(State state, PedestalLight.Glow glow, float up, float lift, float middle, int color) {
        submitLensBase(state, glow, PedestalLight.Sprite.LENS_PRISM_0, state.time * 0.006F, 0xE6FFFFFF);
        submitAura(state, glow, color, 80.0F);
        // from the lens's glass itself, so the rays come out of it rather than above it
        submitPrismFan(state, glow, up);
        submitPrismHalo(state, glow, lift, middle);
        submitPrismCrystals(state, glow, middle);
    }

    @Override
    protected void light(State state, PedestalLight.Glow glow, float up, float lift, float middle, int color) {
        // the light the lens has reached on its way round the rainbow, and the halo's heart glowing in it
        int hue = ARGB.opaque(LensLooks.prism(state.time, 0.0F));
        lensLight(state, glow, up, hue, 0.34F);
        glow.light(0.5F + state.backX * haloBack(state.size), haloY(lift, middle, state.size), 0.5F + state.backZ * haloBack(state.size), hue,
                0.25F * state.rise, 2.8F * state.size);
    }

    private static void submitPrismHalo(State state, PedestalLight.Glow glow, float lift, float middle) {
        float half = haloHalf(state.size), center = haloY(lift, middle, state.size);
        float x = 0.5F + state.backX * haloBack(state.size), z = 0.5F + state.backZ * haloBack(state.size);
        // turning slowly in the plane facing the camera
        float roll = haloRoll(state.time);
        haloQuad(state, glow, PedestalLight.Sprite.PRISM_HALO_0.playing(state.time), x, center, z, half, roll,
                ARGB.color((int) (245 * state.rise), 0xFFFFFF));
        float breath = 0.82F + 0.18F * Mth.sin(state.time / 15.0F);
        int hue = ARGB.srgbLerp(0.25F, ARGB.opaque(LensLooks.prism(state.time, 0.0F)), 0xFFFFFFFF);
        glow.billboard(PedestalLight.Sprite.APPRAISAL_BLOOM, x, center, z, 0.25F * breath * state.size, 0.0F, ARGB.color((int) (135 * state.rise), hue));
        glow.billboard(PedestalLight.Sprite.APPRAISAL_BLOOM, x, center, z, 0.11F * breath * state.size, 0.0F, ARGB.color((int) (175 * state.rise), 0xFFFFFF));
        glow.billboard(PedestalLight.Sprite.APPRAISAL_GLARE, x, center, z, 0.28F * breath * state.size, 0.0F, ARGB.color((int) (150 * state.rise), hue));
        for (int streak = 0; streak < HALO_STREAKS; streak++) submitHaloStreak(state, glow, x, center, z, half, roll, streak);
    }

    private static void submitHaloStreak(State state, PedestalLight.Glow glow, float x, float y, float z, float half, float roll, int streak) {
        float p = streakFlight(state.time, streak);
        if (p < 0.0F || p > 1.0F) return;
        int shard = streakShard(state.time, streak), tint = haloColor(shard);
        float angle = shardAngle(roll, shard), cos = Mth.cos(angle), sin = Mth.sin(angle);
        float far = half * 1.14F, passes = STREAK_PASSES, head = far * Math.min(1.0F, p / HALO_FLIGHT), tail = far * Mth.clamp((p - HALO_TRAIL) / HALO_FLIGHT, 0.0F, 1.0F);
        float fade = 1.0F - easeInOut(Mth.clamp((p - passes) / (HALO_FLIGHT + HALO_TRAIL - passes), 0.0F, 1.0F));
        if (fade > 0.0F && head > tail) {
            haloLine(state, glow, x, y, z, cos, sin, tail, head, 0.015F * state.size, ARGB.color((int) (215 * fade * state.rise), tint));
            haloLine(state, glow, x, y, z, cos, sin, tail + (head - tail) * 0.4F, head, 0.006F * state.size, ARGB.color((int) (255 * fade * state.rise), 0xFFFFFF));
        }
        if (p < passes) return;
        float flash = streakGlint(p);
        int base = shard % 2;
        haloQuad(state, glow, PedestalLight.Sprite.PRISM_HALO_FLASH_0.frame(base), x, y, z, half, roll - (shard - base) * Mth.TWO_PI / HALO_SHARDS,
                ARGB.color((int) (255 * flash * state.rise), 0xFFFFFF));
        float at = half * 0.72F;
        glow.billboard(PedestalLight.Sprite.STAR_GLINT, x + state.faceX * cos * at, y + sin * at, z + state.faceZ * cos * at,
                0.1F * state.size * flash, state.time * 0.04F, ARGB.color((int) (255 * flash * state.rise), 0xFFFFFF));
    }

    /** When a streak reaches its shard, the part of its flight it has flown by then. */
    private static final float STREAK_PASSES = HALO_FLIGHT * 0.58F / 1.14F;

    /** Half the halo's width, for a lens of this size. */
    public static float haloHalf(float size) {
        return 0.44F * size;
    }

    /** The height of the halo's heart above the pedestal's block: by the item, and never down in the lens. */
    public static float haloY(float lift, float middle, float size) {
        return Math.max(middle, lift + haloHalf(size) * 0.97F);
    }

    /** How far the halo stands behind the item, away from the camera. */
    public static float haloBack(float size) {
        return CIRCLE_BACK * size;
    }

    /** How far round the halo has turned. */
    public static float haloRoll(float time) {
        return time * PRISM_HALO_TURN;
    }

    /** The angle, in the halo's plane, of the middle of a pane, the halo turned by {@code roll}. */
    public static float shardAngle(float roll, int shard) {
        return roll - (shard + 0.5F) * Mth.TWO_PI / HALO_SHARDS;
    }

    /** The colour of a pane of the halo's glass. */
    public static int haloColor(int shard) {
        return HALO_COLORS[shard % HALO_COLORS.length];
    }

    /** How brightly a pane glints now, from 0 to 1: as a streak of light passes through it. */
    public static float shardGlint(float time, int shard) {
        float glint = 0.0F;
        for (int streak = 0; streak < HALO_STREAKS; streak++) {
            float p = streakFlight(time, streak);
            if (p >= STREAK_PASSES && p <= 1.0F && streakShard(time, streak) == shard) glint = Math.max(glint, streakGlint(p));
        }
        return glint;
    }

    /** How far a streak is through its flight, from 0 to 1, or outside that while it isn't flying. */
    private static float streakFlight(float time, int streak) {
        float round = time / (58.0F + streak * 13.0F) + streak * 0.37F;
        int count = Mth.floor(round), seed = streakSeed(round, streak);
        return (round - count - (seed >>> 8 & 255) / 255.0F * 0.55F) / 0.45F;
    }

    /** The pane a streak flies through this round. */
    private static int streakShard(float time, int streak) {
        return Math.floorMod(streakSeed(time / (58.0F + streak * 13.0F) + streak * 0.37F, streak), HALO_SHARDS);
    }

    private static int streakSeed(float round, int streak) {
        int seed = Mth.floor(round) * 73856093 ^ streak * 19349663;
        seed ^= seed >>> 13;
        seed *= 0x5BD1E995;
        seed ^= seed >>> 15;
        return seed;
    }

    /** The glint of a pane a streak has just passed, fading fast. */
    private static float streakGlint(float flight) {
        float after = Math.min(1.0F, (flight - STREAK_PASSES) / 0.3F);
        return (1.0F - after) * (1.0F - after);
    }

    private static void haloLine(State state, PedestalLight.Glow glow, float x, float y, float z, float cos, float sin, float from, float to, float width, int color) {
        // Streak runs along (cos, sin); the width offset is its perpendicular (-sin, cos).
        float fromSide = cos * from, fromUp = sin * from, side = cos * to, up = sin * to, offSide = -sin * width, offUp = cos * width;
        int clear = ARGB.color(0, color);
        glow.quad(x + state.faceX * fromSide, y + fromUp, z + state.faceZ * fromSide, x + state.faceX * fromSide, y + fromUp, z + state.faceZ * fromSide,
                x + state.faceX * (side + offSide), y + up + offUp, z + state.faceZ * (side + offSide),
                x + state.faceX * (side - offSide), y + up - offUp, z + state.faceZ * (side - offSide), clear, clear, color, color);
    }

    private static void submitPrismCrystals(State state, PedestalLight.Glow glow, float middle) {
        float radius = 0.44F * state.size, rise = 0.15F * state.size, width = 0.013F * state.size;
        for (int crystal = 0; crystal < PRISM_CRYSTALS; crystal++) {
            float head = state.time * 0.032F + crystal * Mth.TWO_PI / PRISM_CRYSTALS;
            for (int step = 0; step < PRISM_TRAIL; step++) {
                float t0 = step / (float) PRISM_TRAIL, t1 = (step + 1) / (float) PRISM_TRAIL;
                // Trail samples sit on the same ellipse, trailing back from the head angle.
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

    /** The fan's rays: how many, and how far each leans out for how far it rises. */
    public static final int FAN_RAYS = 7;
    public static final float FAN_REACH = 0.5F, FAN_RISE = 0.8F;

    /** Which way round a ray of the fan points now. */
    public static float fanAngle(float time, int ray) {
        return time * 0.012F + ray * Mth.TWO_PI / FAN_RAYS;
    }

    /** How bright a ray of the fan is now, from 0.2 to 1. */
    public static float fanPulse(float time, int ray) {
        return 0.6F + 0.4F * Mth.sin(time * 0.05F + ray * 1.7F);
    }

    /** A ray of the fan's colour now. */
    public static int fanColor(float time, int ray) {
        return LensLooks.prism(time, ray / (float) FAN_RAYS);
    }

    private static void submitPrismFan(State state, PedestalLight.Glow glow, float floor) {
        for (int ray = 0; ray < FAN_RAYS; ray++) {
            float angle = fanAngle(state.time, ray);
            float reach = FAN_REACH * state.size, rise = FAN_RISE * state.size;
            float tx = 0.5F + Mth.cos(angle) * reach, tz = 0.5F + Mth.sin(angle) * reach, ty = floor + rise;
            float pulse = fanPulse(state.time, ray);
            int hue = fanColor(state.time, ray);
            int foot = ARGB.color((int) (130 * pulse * state.rise), hue), end = ARGB.color(0, hue);
            float foot0 = 0.01F, top = 0.05F * state.size;
            float fx = state.faceX, fz = state.faceZ;
            glow.quad(0.5F - fx * foot0, floor, 0.5F - fz * foot0, 0.5F + fx * foot0, floor, 0.5F + fz * foot0,
                    tx + fx * top, ty, tz + fz * top, tx - fx * top, ty, tz - fz * top, foot, foot, end, end);
        }
    }
}
