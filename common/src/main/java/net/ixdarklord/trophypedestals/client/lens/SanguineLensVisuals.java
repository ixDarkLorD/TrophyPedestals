package net.ixdarklord.trophypedestals.client.lens;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ixdarklord.trophypedestals.client.PedestalLight;
import net.ixdarklord.trophypedestals.client.PedestalRenderer;
import net.ixdarklord.trophypedestals.client.PedestalRenderer.State;
import net.ixdarklord.trophypedestals.compat.ARGB;
import net.ixdarklord.trophypedestals.compat.render.SubmitNodeCollector;
import net.ixdarklord.trophypedestals.pedestal.Lens;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

public final class SanguineLensVisuals extends LensVisuals {
    private static final float MOON_EYE_TICKS = 170.0F;
    private static final float BLOOD_POOL_Y = 14.0F / 16.0F + 0.004F;
    private static final float MOON_LIFT = 0.22F, FOG_TICKS = 95.0F, FOG_BACK = 0.21F, MOON_LAYER = 0.015F;
    private static final int FOG_PUFFS = 26;
    private static final int BLOOD_STARES = 4, BLOOD_EYE_FRAMES = 8, RAVENS = 9;
    private static final int BLOOD_GAZE_FRAMES = 12;

    @Override
    protected void draw(State state, PedestalLight.Glow glow, float up, float lift, float middle, int color) {
        float beat = heartbeat(state.time);
        submitAura(state, glow, color, 50.0F + 55.0F * beat);
        glow.flat(PedestalLight.Sprite.BLOOD_POOL_0.playing(state.time), BLOOD_POOL_Y, 0.25F, 0.0F, ARGB.color((int) (250 * state.rise), 0xFFFFFF));
        glow.occludes();
        submitDarkFog(state, glow, up);
        float moonY = middle + MOON_LIFT * state.size;
        submitBloodBeam(state, glow, up, moonY, beat);
        submitBloodMoon(state, glow, moonY, beat);
        submitBloodStares(state, glow, moonY);
        submitRavens(state, glow);
    }

    @Override
    protected void light(State state, PedestalLight.Glow glow, float up, float lift, float middle, int color) {
        // the moon's red light, beating with its heart, and the pool's below it
        float beat = heartbeat(state.time), moonY = middle + MOON_LIFT * state.size;
        glow.light(0.5F, moonY, 0.5F, color, (0.55F + 0.6F * beat) * state.rise, 4.5F * state.size);
        lensLight(state, glow, BLOOD_POOL_Y, color, 0.35F + 0.35F * beat);
    }

    private static float heartbeat(float time) {
        float t = time / 34.0F % 1.0F;
        // Two sine bumps per cycle: a full beat, then a weaker second one.
        float first = t < 0.14F ? Mth.sin(t / 0.14F * Mth.PI) : 0.0F, second = t > 0.2F && t < 0.36F ? Mth.sin((t - 0.2F) / 0.16F * Mth.PI) * 0.7F : 0.0F;
        return Math.max(first, second);
    }

    private static void submitBloodMoon(State state, PedestalLight.Glow glow, float moonY, float beat) {
        float half = 0.38F * state.size, height = half * 2.0F * (64.0F / 48.0F), y = moonY + moonBob(state);
        float x = 0.5F + state.backX * CIRCLE_BACK * 1.1F * state.size, z = 0.5F + state.backZ * CIRCLE_BACK * 1.1F * state.size;
        float bx = x + state.backX * moonLayer(state), bz = z + state.backZ * moonLayer(state);
        float wide = half * (2.5F + 0.15F * beat), near = half * (1.55F + 0.1F * beat);
        upright(state, glow, PedestalLight.Sprite.APPRAISAL_BLOOM, bx, y - wide, bz, wide, wide * 2.0F, ARGB.color((int) ((50 + 50 * beat) * state.rise), 0x8E0F1C));
        upright(state, glow, PedestalLight.Sprite.APPRAISAL_BLOOM, bx, y - near, bz, near, near * 2.0F, ARGB.color((int) ((85 + 65 * beat) * state.rise), 0xFF4A24));
        if (state.rise < 1.0F) {
            upright(state, glow, PedestalLight.Sprite.BLOOD_MOON_0.playing(state.time), x, y + half - height, z, half, height, ARGB.color((int) (252 * state.rise), 0xFFFFFF));
        }
        float ring = half * 4.0F / 3.0F;
        upright(state, glow, PedestalLight.Sprite.BLOOD_CORONA_0.playing(state.time), bx, y - ring, bz, ring, ring * 2.0F,
                ARGB.color((int) ((175 + 80 * beat) * state.rise), 0xFFFFFF));
        float hot = -half * 0.8F, hx = x + state.faceX * hot - state.backX * moonLayer(state) * 2.0F, hz = z + state.faceZ * hot - state.backZ * moonLayer(state) * 2.0F, flare = 0.8F + 0.2F * beat;
        glow.billboard(PedestalLight.Sprite.BLOOD_GLARE, hx, y, hz, half * 0.78F * flare, state.time * 0.004F, ARGB.color((int) (200 * flare * state.rise), 0xFFB070));
        upright(state, glow, PedestalLight.Sprite.BLOOD_GLARE, hx, y - half * 0.085F, hz, half * 1.85F * flare, half * 0.17F, ARGB.color((int) (150 * flare * state.rise), 0xFF6A3A));
    }

    private static float moonLayer(State state) {
        return MOON_LAYER * Math.max(1.0F, state.size);
    }

    private static float moonBob(State state) {
        return 0.015F * Mth.sin(state.time / 31.0F) * state.size;
    }

    public static void submitMoonDisc(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (state.lens != Lens.SANGUINE || !state.detailed || !state.lensEffects || state.rise * state.lensFade < 1.0F) return;
        float half = 0.38F * state.size, height = half * 2.0F * (64.0F / 48.0F);
        float top = state.iconY + state.middle + state.bob + MOON_LIFT * state.size + moonBob(state) + half, bottom = top - height;
        float x = 0.5F + state.backX * CIRCLE_BACK * 1.1F * state.size, z = 0.5F + state.backZ * CIRCLE_BACK * 1.1F * state.size;
        float fx = state.faceX * half, fz = state.faceZ * half;
        PedestalLight.Sprite frame = PedestalLight.Sprite.BLOOD_MOON_0.playing(state.time);
        float[] uv = frame.uv();
        collector.submitCustomGeometry(poseStack, RenderType.text(frame.sheet()), (pose, consumer) -> {
            moonVertex(consumer, pose, x - fx, top, z - fz, uv[0], uv[1]);
            moonVertex(consumer, pose, x - fx, bottom, z - fz, uv[0], uv[3]);
            moonVertex(consumer, pose, x + fx, bottom, z + fz, uv[2], uv[3]);
            moonVertex(consumer, pose, x + fx, top, z + fz, uv[2], uv[1]);
            moonVertex(consumer, pose, x + fx, top, z + fz, uv[2], uv[1]);
            moonVertex(consumer, pose, x + fx, bottom, z + fz, uv[2], uv[3]);
            moonVertex(consumer, pose, x - fx, bottom, z - fz, uv[0], uv[3]);
            moonVertex(consumer, pose, x - fx, top, z - fz, uv[0], uv[1]);
        });
    }

    private static void moonVertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v) {
        consumer.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setLight(PedestalRenderer.FULL_BRIGHT);
    }

    private static void submitDarkFog(State state, PedestalLight.Glow glow, float lensY) {
        for (int puff = 0; puff < FOG_PUFFS; puff++) {
            float life = state.time / FOG_TICKS + puff / (float) FOG_PUFFS;
            int round = Mth.floor(life), seed = roundSeed(round, 40 + puff);
            float p = life - round, angle = (seed >>> 8 & 255) / 255.0F * Mth.TWO_PI, spread = easeInOut(p);
            boolean vapor = puff % 3 == 0;
            float from = vapor ? 0.2F + 0.04F * (seed >>> 16 & 15) / 15.0F : 0.12F * (seed >>> 16 & 15) / 15.0F;
            float reach = from + (0.18F + 0.18F * (seed >>> 20 & 15) / 15.0F) * spread * state.size;
            float y = lensY + 0.04F + (0.65F + 0.4F * (seed >>> 24 & 15) / 15.0F) * p * state.size;
            float half = Mth.lerp(spread, vapor ? 0.08F : 0.12F, vapor ? 0.32F : 0.42F) * state.size;
            int tint = ARGB.srgbLerp(p, vapor ? 0x7A0E1A : 0x34101A, vapor ? 0x1C0A0E : 0x120A0E);
            float across = Mth.cos(angle) * reach, depth = FOG_BACK * state.size * Mth.clamp(p / 0.18F, 0.3F, 1.0F);
            haloQuad(state, glow, PedestalLight.Sprite.BLOOD_FOG, 0.5F + state.faceX * across + state.backX * depth, y, 0.5F + state.faceZ * across + state.backZ * depth, half,
                    angle + state.time * 0.014F, ARGB.color((int) ((vapor ? 165 : 205) * Mth.sin(p * Mth.PI) * state.rise), tint));
        }
    }

    private static void submitBloodBeam(State state, PedestalLight.Glow glow, float lensY, float moonY, float beat) {
        float x = 0.5F + state.backX * CIRCLE_BACK * 1.1F * state.size, z = 0.5F + state.backZ * CIRCLE_BACK * 1.1F * state.size, fromY = lensY + 0.01F;
        float top = 0.085F * state.size, foot = 0.05F + 0.008F * beat, fromMoon = moonY - 0.25F * state.size;
        // Back-projects the beam tip so it lines up with the moon's plane.
        float plane = CIRCLE_BACK * 1.1F * state.size, through = Math.min(0.5F, 0.1F * state.size / Math.max(0.01F, fromMoon - fromY)), tuck = plane * through / (1.0F - through);
        float tipX = x + state.backX * tuck, tipZ = z + state.backZ * tuck;
        glow.sprite(PedestalLight.Sprite.BLOOD_BEAM_0.playing(state.time),
                tipX - state.faceX * top, fromMoon, tipZ - state.faceZ * top, 0.5F - state.faceX * foot, fromY, 0.5F - state.faceZ * foot,
                0.5F + state.faceX * foot, fromY, 0.5F + state.faceZ * foot, tipX + state.faceX * top, fromMoon, tipZ + state.faceZ * top,
                ARGB.color((int) (240 * state.rise), 0xFFFFFF));
        glow.billboard(PedestalLight.Sprite.APPRAISAL_BLOOM, 0.5F, fromY + 0.01F, 0.5F, 0.13F + 0.03F * beat, 0.0F, ARGB.color((int) ((110 + 80 * beat) * state.rise), 0xFF3A24));
    }

    private static int roundSeed(int round, int event) {
        int seed = round * 73856093 ^ (event + 7) * 19349663;
        seed ^= seed >>> 13;
        seed *= 0x5BD1E995;
        return seed ^ seed >>> 15;
    }

    private static void submitBloodStares(State state, PedestalLight.Glow glow, float middle) {
        float bx = 0.5F + state.backX * CIRCLE_BACK * 1.1F * state.size, bz = 0.5F + state.backZ * CIRCLE_BACK * 1.1F * state.size;
        submitMoonEye(state, glow, bx - state.backX * moonLayer(state) * 2.0F, middle, bz - state.backZ * moonLayer(state) * 2.0F);
        for (int stare = 0; stare < BLOOD_STARES; stare++) {
            float round = state.time / (84.0F + stare * 29.0F) + stare * 0.41F;
            int count = Mth.floor(round), seed = roundSeed(count, stare);
            float p = (round - count - (seed >>> 8 & 255) / 255.0F * 0.45F) / 0.55F;
            if ((seed >>> 5 & 3) == 0 || p < 0.0F || p >= 1.0F) continue;
            float half = (0.075F + 0.035F * (seed >>> 12 & 15) / 15.0F) * state.size;
            float shift = ((seed >>> 16 & 255) / 255.0F - 0.5F) * 0.6F;
            float angle = (stare + 0.5F) * Mth.TWO_PI / BLOOD_STARES + shift, reach = (0.6F + 0.08F * (seed >>> 9 & 15) / 15.0F) * state.size;
            float across = Mth.cos(angle) * reach, x = bx + state.faceX * across, y = middle + Mth.sin(angle) * reach, z = bz + state.faceZ * across, shown = Mth.sin(p * Mth.PI);
            glow.billboard(PedestalLight.Sprite.APPRAISAL_BLOOM, x, y, z, half * 2.2F, 0.0F, ARGB.color((int) (125 * shown * state.rise), 0xFF2020));
            upright(state, glow, PedestalLight.Sprite.BLOOD_EYES_0.frame(Math.min(BLOOD_EYE_FRAMES - 1, (int) (p * BLOOD_EYE_FRAMES))), x, y - half * 0.5F, z, half, half,
                    ARGB.color((int) (255 * Math.min(1.0F, shown * 3.0F) * state.rise), 0xFFFFFF));
        }
    }

    private static void submitMoonEye(State state, PedestalLight.Glow glow, float moonX, float middle, float moonZ) {
        float round = state.time / MOON_EYE_TICKS;
        int count = Mth.floor(round), seed = roundSeed(count, 11);
        float p = (round - count - (seed >>> 8 & 255) / 255.0F * 0.45F) / 0.5F;
        if (p < 0.0F || p >= 1.0F) return;
        float blinkAt = 0.4F + 0.2F * (seed >>> 16 & 255) / 255.0F, blink = Math.min(1.0F, Math.abs(p - blinkAt) / 0.035F);
        float opened = easeInOut(Math.min(1.0F, Math.min(p / 0.2F, (1.0F - p) / 0.16F))) * blink;
        if (opened <= 0.02F) return;
        float x = moonX + state.faceX * 0.032F * state.size, y = middle + (0.013F + 0.015F * Mth.sin(state.time / 31.0F)) * state.size, z = moonZ + state.faceZ * 0.032F * state.size;
        float half = 0.317F * state.size;
        upright(state, glow, PedestalLight.Sprite.BLOOD_FLASH, x, y - half * 0.8F, z, half * 0.8F, half * 1.6F, ARGB.color((int) (70 * opened * state.rise), 0xFF3A24));
        upright(state, glow, PedestalLight.Sprite.BLOOD_GAZE_0.frame(Math.min(BLOOD_GAZE_FRAMES - 1, (int) (opened * BLOOD_GAZE_FRAMES))), x, y - half, z, half, half * 2.0F,
                ARGB.color((int) (250 * state.rise), 0xFFFFFF));
    }

    private static void submitRavens(State state, PedestalLight.Glow glow) {
        float radius = 0.44F * state.size, half = 0.09F * state.size;
        for (int raven = 0; raven < RAVENS; raven++) {
            float angle = state.time * 0.024F + raven * Mth.TWO_PI / RAVENS;
            float x = 0.5F + Mth.cos(angle) * radius, z = 0.5F + Mth.sin(angle) * radius;
            float y = state.displayTop + state.bob + (0.2F + 0.018F * Mth.sin(state.time / 9.0F + raven * 1.3F)) * state.size;
            float heading = -Mth.sin(angle) * state.faceX + Mth.cos(angle) * state.faceZ;
            boolean near = (x - 0.5F) * state.backX + (z - 0.5F) * state.backZ < CIRCLE_BACK * 1.1F * state.size;
            upright(state, glow, (near ? PedestalLight.Sprite.RAVEN_FRONT_0 : PedestalLight.Sprite.RAVEN_0).playing(state.time, raven / 3.0F), x, y - half, z, heading >= 0.0F ? half : -half, half * 2.0F,
                    ARGB.color((int) (255 * state.rise), 0xFFFFFF));
        }
    }
}
