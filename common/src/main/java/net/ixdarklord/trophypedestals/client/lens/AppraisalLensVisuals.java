package net.ixdarklord.trophypedestals.client.lens;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.client.PedestalLight;
import net.ixdarklord.trophypedestals.client.PedestalRenderer;
import net.ixdarklord.trophypedestals.client.PedestalRenderer.State;
import net.ixdarklord.trophypedestals.compat.ARGB;
import net.ixdarklord.trophypedestals.compat.render.SubmitNodeCollector;
import net.ixdarklord.trophypedestals.pedestal.Lens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class AppraisalLensVisuals extends LensVisuals {
    private static final float[] APPRAISAL_AURA = {35.0F, 55.0F, 70.0F, 85.0F}, STAR_SPOTS = {-0.26F, 0.2F, 0.28F, 0.08F, -0.17F, -0.22F, 0.2F, -0.26F};
    private static final int GLEAM_FRAMES = 12, PEBBLES = 6, ORBIT_TAIL = 5, FLARE_TRAIL = 22, CROWN_FRAMES = 16, SPIRAL_ARMS = 2, SPIRAL_STEPS = 36, ORBS = 6, DIAMONDS = 4, DIAMOND_TRAIL = 4;
    private static final float CROWN_TICKS = 4.0F, CROWN_HALF = 0.19F, CROWN_LIFT = 0.05F, CROWN_MIDDLE = 18.0F / 32.0F, ORB_POP = 0.86F, BANNER_HALF = 0.17F, BANNER_SIDE = 0.5F, BANNER_BACK = 0.12F, ORB_TICKS = 96.0F;
    private static final float GLEAM_TICKS = 90.0F, GLEAM_SHARE = 0.27F, STAR_TICKS = 52.0F;
    private static final float APPRAISAL_TRIM_DROP = 1.0F / 16.0F;
    private static final ResourceLocation[] VELVETS = {TrophyPedestalsMod.id("textures/block/pedestal/velvet_common.png"), TrophyPedestalsMod.id("textures/block/pedestal/velvet_uncommon.png"),
            TrophyPedestalsMod.id("textures/block/pedestal/velvet_rare.png"), TrophyPedestalsMod.id("textures/block/pedestal/velvet_epic.png")};

    @Override
    protected void draw(State state, PedestalLight.Glow glow, float up, float lift, float middle, int color) {
        submitAppraisal(state, glow, lift, middle, color);
    }

    private static void submitAppraisal(State state, PedestalLight.Glow glow, float lift, float middle, int color) {
        int tier = state.itemRarity;
        float pulse = 0.82F + 0.18F * Mth.sin(state.time / 16.0F);
        submitAura(state, glow, color, APPRAISAL_AURA[tier] * pulse);
        int shine = ARGB.srgbLerp(0.55F, ARGB.opaque(color), 0xFFFFFFFF);
        submitLensBase(state, glow, PedestalLight.Sprite.LENS_FACETS_0, state.time * 0.003F, ARGB.color(225, shine));
        submitGleam(state, glow, middle, shine);
        if (tier == 0) submitPebbles(state, glow, lift);
        if (tier == 1) {
            submitExperienceOrbs(state, glow, lift);
            submitOrbitLights(state, glow, middle, 3, 0.38F, 0.14F, 0.03F, 0xFFFFFF55, false);
        }
        if (tier == 2) {
            submitBackBloom(state, glow, middle, 0.5F, 62.0F, 0xFF55FFFF);
            submitDiamonds(state, glow, middle);
            submitOrbitLights(state, glow, middle, 2, 0.52F, 0.2F, -0.04F, 0xFF55FFFF, true);
            submitLightSpiral(state, glow, lift);
        }
        if (tier >= 3) submitGodRays(state, glow, lift, 5, shine, 136.0F);
        if (tier >= 3) submitStarGlints(state, glow, middle, tier + 1, shine);
        if (tier >= 3) {
            submitAurora(state, glow, lift);
            submitRoyalBanners(state, glow);
            submitEpicCrown(state, glow);
        }
        submitCapBand(state, glow, PedestalLight.Sprite.APPRAISAL_TRIM_0.frame(tier), CAP_TOP_EDGE - APPRAISAL_TRIM_DROP, CAP_BOTTOM_EDGE - APPRAISAL_TRIM_DROP, ARGB.color((int) (250 * state.rise), 0xFFFFFF));
        submitCapBand(state, glow, PedestalLight.Sprite.APPRAISAL_GLOW_0.frame(tier), CAP_TOP_EDGE - APPRAISAL_TRIM_DROP, CAP_BOTTOM_EDGE - APPRAISAL_TRIM_DROP, ARGB.color((int) ((90 + 110 * pulse) * state.rise), 0xFFFFFF));
    }

    private static void submitGleam(State state, PedestalLight.Glow glow, float middle, int shine) {
        float cycle = (state.time + (state.blockPos.hashCode() & 0xFF)) / GLEAM_TICKS % 1.0F;
        if (cycle >= GLEAM_SHARE) return;
        int frame = Math.min(GLEAM_FRAMES - 1, (int) (cycle / GLEAM_SHARE * GLEAM_FRAMES));
        float half = Mth.clamp(state.displayTop - state.iconY - state.middle, 0.1F * state.size, 0.7F * state.size);
        upright(state, glow, PedestalLight.Sprite.GLEAM_0.frame(frame), 0.5F, middle - half, 0.5F, half * 1.3F, half * 2.0F, ARGB.color((int) (255 * state.rise), shine));
    }

    private static void submitRoyalBanners(State state, PedestalLight.Glow glow) {
        float half = BANNER_HALF * state.size, height = half * 4.0F;
        float top = state.displayTop + state.bob + (CROWN_LIFT + CROWN_HALF * 2.0F * CROWN_MIDDLE) * state.size;
        for (int side = -1; side <= 1; side += 2) {
            float x = 0.5F + (state.faceX * side * BANNER_SIDE + state.backX * BANNER_BACK) * state.size;
            float z = 0.5F + (state.faceZ * side * BANNER_SIDE + state.backZ * BANNER_BACK) * state.size;
            upright(state, glow, PedestalLight.Sprite.APPRAISAL_BANNER_0.playing(state.time, side > 0 ? 0.5F : 0.0F),
                    x, top - height, z, half, height, ARGB.color((int) (250 * state.rise), 0xFFFFFF));
        }
    }

    private static void submitEpicCrown(State state, PedestalLight.Glow glow) {
        float half = CROWN_HALF * state.size, sway = 0.012F * Mth.sin(state.time / 21.0F) * state.size;
        float bottom = state.displayTop + state.bob + sway + CROWN_LIFT * state.size;
        upright(state, glow, PedestalLight.Sprite.APPRAISAL_CROWN_0.playing(state.time),
                0.5F, bottom, 0.5F, half, half * 2.0F, ARGB.color((int) (255 * state.rise), 0xFFFFFF));
        float cycle = state.time / (CROWN_TICKS * CROWN_FRAMES) % 1.0F;
        if (cycle > 0.3F && cycle < 0.46F) {
            float flash = Mth.sin((cycle - 0.3F) / 0.16F * Mth.PI), across = 0.0F;
            glow.billboard(PedestalLight.Sprite.STAR_GLINT, 0.5F + state.faceX * across, bottom + half * 1.69F, 0.5F + state.faceZ * across,
                    0.11F * flash * state.size, state.time * 0.02F, ARGB.color((int) (255 * flash * state.rise), 0xFFFFFF));
        }
    }

    private static void submitBackBloom(State state, PedestalLight.Glow glow, float middle, float half, float strength, int tint) {
        float breath = 0.75F + 0.25F * Mth.sin(state.time / 22.0F);
        float x = 0.5F + state.backX * CIRCLE_BACK * state.size, z = 0.5F + state.backZ * CIRCLE_BACK * state.size;
        glow.billboard(PedestalLight.Sprite.APPRAISAL_BLOOM, x, middle, z, half * state.size * (0.92F + 0.08F * breath), 0.0F, ARGB.color((int) (strength * breath * state.rise), tint));
    }

    private static void submitOrbitLights(State state, PedestalLight.Glow glow, float middle, int lights, float radius, float tilt, float speed, int tint, boolean bloom) {
        float reach = radius * state.size, rise = tilt * state.size, direction = Math.signum(speed);
        for (int light = 0; light < lights; light++) {
            // Lights spaced evenly round a tilted elliptical orbit.
            float angle = state.time * speed + light * Mth.TWO_PI / lights;
            float x = 0.5F + Mth.cos(angle) * reach, y = middle + Mth.sin(angle) * rise, z = 0.5F + Mth.sin(angle) * reach;
            float pulse = 0.82F + 0.18F * Mth.sin(state.time / 7.0F + light * 2.1F);
            if (bloom) {
                for (int tail = ORBIT_TAIL; tail >= 1; tail--) {
                    float back = angle - direction * tail * 0.13F, left = 1.0F - tail / (ORBIT_TAIL + 1.0F);
                    glow.billboard(PedestalLight.Sprite.APPRAISAL_BLOOM, 0.5F + Mth.cos(back) * reach, middle + Mth.sin(back) * rise, 0.5F + Mth.sin(back) * reach,
                            0.06F * left * state.size, 0.0F, ARGB.color((int) (125 * left * state.rise), tint));
                }
                glow.billboard(PedestalLight.Sprite.APPRAISAL_BLOOM, x, y, z, 0.13F * pulse * state.size, 0.0F, ARGB.color((int) (205 * state.rise), tint));
                glow.billboard(PedestalLight.Sprite.APPRAISAL_BLOOM, x, y, z, 0.045F * state.size, 0.0F, ARGB.color((int) (255 * state.rise), 0xFFFFFF));
                glow.billboard(PedestalLight.Sprite.APPRAISAL_GLARE, x, y, z, 0.22F * pulse * state.size, 0.0F, ARGB.color((int) (165 * state.rise), tint));
            } else {
                submitFlareLines(state, glow, angle, reach, rise, middle, direction, pulse, tint);
            }
        }
    }

    private static void submitFlareLines(State state, PedestalLight.Glow glow, float angle, float reach, float rise, float middle, float direction, float pulse, int tint) {
        float cos = Mth.cos(angle), sin = Mth.sin(angle), x = 0.5F + cos * reach, y = middle + sin * rise, z = 0.5F + sin * reach;
        float footY = PedestalRenderer.GLASS_TOP + RIPPLE_LIFT, footX = 0.5F + cos * reach * 0.25F, footZ = 0.5F + sin * reach * 0.25F;
        float headX = 0.5F + cos * reach * 1.25F, headY = y + 0.24F * state.size, headZ = 0.5F + sin * reach * 1.25F;
        float foot = 0.008F * state.size, waist = 0.026F * pulse * state.size, head = 0.05F * state.size;
        int clear = ARGB.color(0, tint), faint = ARGB.color((int) (45 * state.rise), tint), bright = ARGB.color((int) (185 * pulse * state.rise), tint);
        flareSegment(state, glow, footX, footY, footZ, foot, faint, x, y, z, waist, bright);
        flareSegment(state, glow, x, y, z, waist, bright, headX, headY, headZ, head, clear);
        int white = ARGB.color((int) (230 * state.rise), 0xFFFFFF), noWhite = ARGB.color(0, 0xFFFFFF);
        float thin = 0.008F * state.size, dx = (headX - footX) * 0.16F, dy = (headY - footY) * 0.16F, dz = (headZ - footZ) * 0.16F;
        flareSegment(state, glow, x - dx, y - dy, z - dz, 0.0F, noWhite, x, y, z, thin, white);
        flareSegment(state, glow, x, y, z, thin, white, x + dx, y + dy, z + dz, 0.0F, noWhite);
        for (int step = 0; step < FLARE_TRAIL; step++) {
            float t0 = step / (float) FLARE_TRAIL, t1 = (step + 1) / (float) FLARE_TRAIL;
            float a0 = angle - direction * (1.0F - t0) * 1.5F, a1 = angle - direction * (1.0F - t1) * 1.5F;
            float x0 = 0.5F + Mth.cos(a0) * reach, y0 = middle + Mth.sin(a0) * rise, z0 = 0.5F + Mth.sin(a0) * reach;
            float x1 = 0.5F + Mth.cos(a1) * reach, y1 = middle + Mth.sin(a1) * rise, z1 = 0.5F + Mth.sin(a1) * reach;
            float w0 = 0.014F * t0 * state.size, w1 = 0.014F * t1 * state.size;
            int c0 = ARGB.color((int) (t0 * t0 * 200 * state.rise), tint), c1 = ARGB.color((int) (t1 * t1 * 200 * state.rise), tint);
            glow.quad(x0, y0 - w0, z0, x0, y0 + w0, z0, x1, y1 + w1, z1, x1, y1 - w1, z1, c0, c0, c1, c1);
        }
    }

    private static void flareSegment(State state, PedestalLight.Glow glow, float x0, float y0, float z0, float half0, int color0, float x1, float y1, float z1, float half1, int color1) {
        glow.quad(x0 - state.faceX * half0, y0, z0 - state.faceZ * half0, x0 + state.faceX * half0, y0, z0 + state.faceZ * half0,
                x1 + state.faceX * half1, y1, z1 + state.faceZ * half1, x1 - state.faceX * half1, y1, z1 - state.faceZ * half1, color0, color0, color1, color1);
    }

    private static void submitLightSpiral(State state, PedestalLight.Glow glow, float lift) {
        float top = state.displayTop + state.bob + 0.1F * state.size, width = 0.014F * state.size;
        for (int arm = 0; arm < SPIRAL_ARMS; arm++) {
            float offset = state.time * 0.05F + arm * Mth.TWO_PI / SPIRAL_ARMS;
            for (int step = 0; step < SPIRAL_STEPS; step++) {
                float t0 = step / (float) SPIRAL_STEPS, t1 = (step + 1) / (float) SPIRAL_STEPS;
                // Rising helix: 1.6 turns per arm, radius shrinking with height.
                float a0 = t0 * Mth.TWO_PI * 1.6F + offset, a1 = t1 * Mth.TWO_PI * 1.6F + offset;
                float r0 = (0.42F - 0.14F * t0) * state.size, r1 = (0.42F - 0.14F * t1) * state.size;
                float x0 = 0.5F + Mth.cos(a0) * r0, z0 = 0.5F + Mth.sin(a0) * r0, y0 = lift + (top - lift) * t0;
                float x1 = 0.5F + Mth.cos(a1) * r1, z1 = 0.5F + Mth.sin(a1) * r1, y1 = lift + (top - lift) * t1;
                int c0 = spiralColor(state, t0), c1 = spiralColor(state, t1);
                glow.quad(x0, y0 - width, z0, x0, y0 + width, z0, x1, y1 + width, z1, x1, y1 - width, z1, c0, c0, c1, c1);
            }
        }
    }

    private static int spiralColor(State state, float t) {
        // Travelling bright bands, with alpha fading in and out along the spiral.
        float wave = Math.max(0.0F, Mth.sin(t * Mth.TWO_PI * 2.0F - state.time * 0.12F));
        return ARGB.color((int) (Mth.sin(t * Mth.PI) * (70.0F + 185.0F * wave) * state.rise), ARGB.srgbLerp(wave * 0.7F, 0xFF55FFFF, 0xFFFFFFFF));
    }

    private static void submitPebbles(State state, PedestalLight.Glow glow, float lift) {
        for (int pebble = 0; pebble < PEBBLES; pebble++) {
            float angle = state.time * 0.004F + pebble * Mth.TWO_PI / PEBBLES, radius = (0.3F + 0.05F * (pebble % 2)) * state.size;
            float bob = 0.03F * Mth.sin(state.time / 18.0F + pebble * 1.9F) * state.size;
            float x = 0.5F + Mth.cos(angle) * radius, y = lift + (0.12F + 0.09F * (pebble % 3)) * state.size + bob, z = 0.5F + Mth.sin(angle) * radius;
            float half = (0.05F + 0.012F * (pebble % 3)) * state.size;
            PedestalLight.Sprite sprite = PedestalLight.Sprite.APPRAISAL_PEBBLE_0.frame(pebble % 3);
            float roll = state.time * (0.012F + 0.003F * (pebble % 3)) * (pebble % 2 == 0 ? 1.0F : -1.0F) + pebble * 1.3F;
            glow.billboard(sprite, x, y, z, half, roll, ARGB.color((int) (255 * state.rise), 0xFFFFFF));
            float dust = (state.time / 34.0F + pebble * 0.37F) % 1.0F;
            glow.billboard(sprite, x, y - half - dust * 0.16F * state.size, z, half * 0.22F, 0.0F, ARGB.color((int) (150 * Mth.sin(dust * Mth.PI) * state.rise), 0xB8B4AC));
        }
    }

    private static void submitExperienceOrbs(State state, PedestalLight.Glow glow, float lift) {
        float top = state.displayTop + state.bob + 0.12F * state.size;
        for (int orb = 0; orb < ORBS; orb++) {
            float age = (state.time / ORB_TICKS + orb / (float) ORBS) % 1.0F;
            float fade = Math.min(1.0F, age * 6.0F) * Math.min(1.0F, (1.0F - age) * 4.0F);
            float flicker = 0.5F + 0.5F * Mth.sin(state.time / 5.0F + orb * 1.7F);
            int tint = ARGB.srgbLerp(flicker, 0xFFFFFF55, 0xFFB8FF3A);
            float half = (0.045F + 0.012F * (orb % 3)) * state.size;
            for (int trail = 2; trail >= 0; trail--) {
                float then = age - trail * 0.02F;
                if (then <= 0.0F) continue;
                // Orb climbs a helix whose radius wobbles with height.
                float angle = then * Mth.TWO_PI * 1.25F + orb * 2.4F;
                float radius = (0.3F + 0.05F * Mth.sin(then * Mth.TWO_PI * 2.0F + orb)) * state.size;
                float x = 0.5F + Mth.cos(angle) * radius, y = lift + 0.04F + (top - lift) * then, z = 0.5F + Mth.sin(angle) * radius;
                PedestalLight.Sprite sprite = trail == 0 ? PedestalLight.Sprite.APPRAISAL_ORB_0.frame(orb % 3) : PedestalLight.Sprite.APPRAISAL_ORB_1;
                glow.billboard(sprite, x, y, z, half * (1.0F - trail * 0.3F), 0.0F, ARGB.color((int) (255 * fade * (1.0F - trail * 0.36F) * state.rise), tint));
                if (trail == 0 && age > ORB_POP) {
                    float pop = Mth.sin((age - ORB_POP) / (1.0F - ORB_POP) * Mth.PI);
                    glow.billboard(PedestalLight.Sprite.APPRAISAL_GLARE, x, y, z, 0.17F * state.size * pop, 0.0F, ARGB.color((int) (210 * pop * state.rise), tint));
                }
            }
        }
    }

    private static void submitDiamonds(State state, PedestalLight.Glow glow, float middle) {
        float radius = 0.42F * state.size, half = 0.08F * state.size;
        for (int gem = 0; gem < DIAMONDS; gem++) {
            float angle = state.time * 0.022F + gem * Mth.TWO_PI / DIAMONDS;
            float bob = 0.025F * Mth.sin(state.time / 14.0F + gem * 1.6F) * state.size;
            float x = 0.5F + Mth.cos(angle) * radius, y = middle + Mth.sin(angle) * 0.1F * state.size + bob, z = 0.5F + Mth.sin(angle) * radius;
            for (int trail = 1; trail <= DIAMOND_TRAIL; trail++) {
                float back = angle - trail * 0.17F, left = 1.0F - trail / (DIAMOND_TRAIL + 1.0F);
                glow.billboard(PedestalLight.Sprite.APPRAISAL_BLOOM, 0.5F + Mth.cos(back) * radius, middle + Mth.sin(back) * 0.1F * state.size + bob, 0.5F + Mth.sin(back) * radius,
                        0.05F * left * state.size, 0.0F, ARGB.color((int) (130 * left * state.rise), 0xFF55FFFF));
            }
            glow.billboard(PedestalLight.Sprite.APPRAISAL_HALO, x, y, z, half * 2.5F, 0.0F, ARGB.color((int) (120 * state.rise), 0xFF55FFFF));
            upright(state, glow, PedestalLight.Sprite.APPRAISAL_GEM_0.playing(state.time, gem / 4.0F), x, y - half, z, half, half * 2.0F, ARGB.color((int) (255 * state.rise), 0xFFFFFF));
            float flash = Mth.sin(state.time / 13.0F + gem * 1.7F);
            if (flash > 0.8F) {
                float bright = (flash - 0.8F) / 0.2F;
                glow.billboard(PedestalLight.Sprite.APPRAISAL_BLOOM, x, y, z, 0.15F * bright * state.size, 0.0F, ARGB.color((int) (190 * bright * state.rise), 0xFFAAFFFF));
                glow.billboard(PedestalLight.Sprite.APPRAISAL_GLARE, x, y, z, 0.26F * bright * state.size, 0.0F, ARGB.color((int) (220 * bright * state.rise), 0xFFFFFFFF));
            }
        }
    }

    private static void submitStarGlints(State state, PedestalLight.Glow glow, float middle, int stars, int shine) {
        float fx = -state.backX * 0.24F * state.size, fz = -state.backZ * 0.24F * state.size;
        for (int star = 0; star < stars; star++) {
            float pulse = Mth.sin(state.time / STAR_TICKS * Mth.TWO_PI + star * Mth.TWO_PI / stars);
            if (pulse <= 0.0F) continue;
            float flash = pulse * pulse * pulse;
            float across = STAR_SPOTS[star * 2] * state.size, up = STAR_SPOTS[star * 2 + 1] * state.size;
            glow.billboard(PedestalLight.Sprite.STAR_GLINT, 0.5F + state.faceX * across + fx, middle + up, 0.5F + state.faceZ * across + fz,
                    0.11F * state.size * flash, state.time * 0.012F, ARGB.color((int) (255 * flash * state.rise), shine));
        }
    }

    private static void submitGodRays(State state, PedestalLight.Glow glow, float floor, int rays, int shine, float strength) {
        float top = floor + 1.2F * state.size;
        float bx = state.backX * 0.1F * state.size, bz = state.backZ * 0.1F * state.size;
        for (int ray = 0; ray < rays; ray++) {
            float across = (ray - (rays - 1) / 2.0F) * 0.17F * state.size + 0.025F * Mth.sin(state.time / 21.0F + ray * 1.9F) * state.size;
            float wave = Mth.sin(state.time / 17.0F + ray * 2.3F);
            float swell = 0.45F + 0.55F * wave * wave;
            float foot = 0.018F * state.size, head = 0.075F * state.size, spread = across * 1.45F;
            float x0 = 0.5F + bx + state.faceX * across * 0.45F, z0 = 0.5F + bz + state.faceZ * across * 0.45F;
            float x1 = 0.5F + bx + state.faceX * spread, z1 = 0.5F + bz + state.faceZ * spread;
            int low = ARGB.color((int) (strength * swell * state.rise), shine), high = ARGB.color(0, shine);
            glow.quad(x0 - state.faceX * foot, floor, z0 - state.faceZ * foot, x0 + state.faceX * foot, floor, z0 + state.faceZ * foot,
                    x1 + state.faceX * head, top, z1 + state.faceZ * head, x1 - state.faceX * head, top, z1 - state.faceZ * head, low, low, high, high);
        }
    }

    private static void submitAurora(State state, PedestalLight.Glow glow, float floor) {
        float x = 0.5F + state.backX * CIRCLE_BACK * state.size, z = 0.5F + state.backZ * CIRCLE_BACK * state.size;
        upright(state, glow, PedestalLight.Sprite.AURORA_0.playing(state.time), x, floor, z, 0.66F * state.size, 1.3F * state.size, ARGB.color((int) (235 * state.rise), 0xFFFFFF));
    }

    public static void submitVelvet(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.lens != Lens.APPRAISAL || !state.lensEffects || !state.detailed || state.lensFade <= 0.0F) return;
        int alpha = (int) (255 * state.lensFade * state.rise), light = state.lightCoords;
        int top = ARGB.color(alpha, 0xFFFFFF), across = ARGB.color(alpha, 0xCCCCCC), along = ARGB.color(alpha, 0x999999);
        float px = 1.0F / 16.0F, e = 0.002F;
        collector.submitCustomGeometry(poseStack, RenderType.entityTranslucent(VELVETS[Mth.clamp(state.itemRarity, 0, VELVETS.length - 1)]), (pose, consumer) -> {
            velvetRing(consumer, pose, 2 * px, 14 * px, 3 * px, 13 * px, 14 * px + e, top, light);
            velvetRing(consumer, pose, 3 * px, 13 * px, 4 * px, 12 * px, 15 * px + e, top, light);
            velvetTop(consumer, pose, 4 * px, 4 * px, 12 * px, 12 * px, 14.06F * px, top, light);
            velvetSides(consumer, pose, 2 * px - e, 14 * px + e, 12 * px, 14 * px + e, across, along, light, false);
            velvetSides(consumer, pose, 3 * px - e, 13 * px + e, 14 * px, 15 * px + e, across, along, light, false);
            velvetSides(consumer, pose, 4 * px + e, 12 * px - e, 14 * px, 15 * px + e, across, along, light, true);
        });
    }

    private static void velvetRing(VertexConsumer consumer, PoseStack.Pose pose, float lo, float hi, float innerLo, float innerHi, float y, int argb, int light) {
        velvetTop(consumer, pose, lo, lo, hi, innerLo, y, argb, light);
        velvetTop(consumer, pose, lo, innerHi, hi, hi, y, argb, light);
        velvetTop(consumer, pose, lo, innerLo, innerLo, innerHi, y, argb, light);
        velvetTop(consumer, pose, innerHi, innerLo, hi, innerHi, y, argb, light);
    }

    private static void velvetTop(VertexConsumer consumer, PoseStack.Pose pose, float x0, float z0, float x1, float z1, float y, int argb, int light) {
        PedestalRenderer.lens(consumer, pose, x0, y, z0, x0, z0, argb, light);
        PedestalRenderer.lens(consumer, pose, x0, y, z1, x0, z1, argb, light);
        PedestalRenderer.lens(consumer, pose, x1, y, z1, x1, z1, argb, light);
        PedestalRenderer.lens(consumer, pose, x1, y, z0, x1, z0, argb, light);
    }

    private static void velvetSides(VertexConsumer consumer, PoseStack.Pose pose, float lo, float hi, float bottom, float top, int across, int along, int light, boolean inward) {
        velvetSide(consumer, pose, hi, lo, lo, lo, bottom, top, across, light, inward);
        velvetSide(consumer, pose, lo, hi, hi, hi, bottom, top, across, light, inward);
        velvetSide(consumer, pose, lo, lo, lo, hi, bottom, top, along, light, inward);
        velvetSide(consumer, pose, hi, hi, hi, lo, bottom, top, along, light, inward);
    }

    private static void velvetSide(VertexConsumer consumer, PoseStack.Pose pose, float ax, float az, float bx, float bz, float bottom, float top, int argb, int light, boolean inward) {
        float x0 = inward ? bx : ax, z0 = inward ? bz : az, x1 = inward ? ax : bx, z1 = inward ? az : bz;
        float u0 = ax != bx ? x0 : z0, u1 = ax != bx ? x1 : z1;
        PedestalRenderer.lens(consumer, pose, x0, top, z0, u0, 1.0F - top, argb, light);
        PedestalRenderer.lens(consumer, pose, x0, bottom, z0, u0, 1.0F - bottom, argb, light);
        PedestalRenderer.lens(consumer, pose, x1, bottom, z1, u1, 1.0F - bottom, argb, light);
        PedestalRenderer.lens(consumer, pose, x1, top, z1, u1, 1.0F - top, argb, light);
    }
}
