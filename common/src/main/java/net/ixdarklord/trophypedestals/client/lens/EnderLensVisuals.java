package net.ixdarklord.trophypedestals.client.lens;

import net.ixdarklord.trophypedestals.client.PedestalLight;
import net.ixdarklord.trophypedestals.client.PedestalRenderer.State;
import net.ixdarklord.trophypedestals.compat.ARGB;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

final class EnderLensVisuals extends LensVisuals {
    private static final float RIFT_PULSE_TICKS = 26.0F;
    private static final float END_GATE_HALF = 0.56F;
    private static final float END_FRAME_TICKS = 14.0F;
    private static final int END_ISLANDS = 3;
    private static final float[] END_ISLAND_SIZE = {0.15F, 0.12F, 0.13F};

    @Override
    protected void draw(State state, PedestalLight.Glow glow, float up, float lift, float middle, int color) {
        boolean endShader = Minecraft.getInstance().options.graphicsMode().get() != GraphicsStatus.FAST;
        glow.flat(endShader ? PedestalLight.Sprite.END_RIFT_LENS : PedestalLight.Sprite.END_LENS_0.playing(state.time), up + 0.003F, LENS_BASE_HALF, 0.0F,
                ARGB.color((int) (255 * state.rise), 0xFFFFFF));
        submitLensBase(state, glow, PedestalLight.Sprite.LENS_EYE_0, 0.0F, 0xF0FFFFFF);
        submitAura(state, glow, color, 55.0F);
        glow.occludes();
        float gx = 0.5F + state.backX * CIRCLE_BACK * state.size, gz = 0.5F + state.backZ * CIRCLE_BACK * state.size;
        glow.billboard((endShader ? PedestalLight.Sprite.END_RIFT_0 : PedestalLight.Sprite.END_GATE_0).playing(state.time), gx, middle, gz, END_GATE_HALF * state.size, 0.0F,
                ARGB.color((int) (240 * state.rise), 0xFFFFFF));
        submitRiftGlow(state, glow, gx, middle, gz);
        submitRiftBeam(state, glow, up, gx, middle, gz);
        submitEndIslands(state, glow, middle);
        submitCapBand(state, glow, PedestalLight.Sprite.END_BAND, CAP_TOP_EDGE, CAP_BOTTOM_EDGE, ARGB.color((int) (250 * state.rise), 0xFFFFFF));
        float wake = (state.time / END_FRAME_TICKS) % 1.0F;
        submitCapBand(state, glow, PedestalLight.Sprite.END_GLOW_0.playing(state.time), CAP_TOP_EDGE, CAP_BOTTOM_EDGE,
                ARGB.color((int) ((150 + 105 * Mth.sin(wake * Mth.PI)) * state.rise), 0xFFFFFF));
    }

    @Override
    protected void light(State state, PedestalLight.Glow glow, float up, float lift, float middle, int color) {
        // the rift's light, from where it hangs
        float gx = 0.5F + state.backX * CIRCLE_BACK * state.size, gz = 0.5F + state.backZ * CIRCLE_BACK * state.size;
        glow.light(gx, middle, gz, color, (0.75F + 0.15F * Mth.sin(state.time * 0.05F)) * state.rise, 4.5F * state.size);
        // the glowing band round the cap, waking and fading with it
        float wake = (state.time / END_FRAME_TICKS) % 1.0F;
        glow.light(0.5F, (CAP_TOP_EDGE + CAP_BOTTOM_EDGE) / 2.0F, 0.5F, color, (0.3F + 0.3F * Mth.sin(wake * Mth.PI)) * state.rise, 2.5F);
    }

    private static void submitRiftGlow(State state, PedestalLight.Glow glow, float riftX, float riftY, float riftZ) {
        float breath = 0.84F + 0.16F * Mth.sin(state.time / 19.0F);
        float x = riftX - state.backX * 0.03F * state.size, z = riftZ - state.backZ * 0.03F * state.size;
        glow.billboard(PedestalLight.Sprite.END_BLOOM, x, riftY, z, 0.88F * breath * state.size, 0.0F, ARGB.color((int) (125 * state.rise), 0xA050F0));
        glow.billboard(PedestalLight.Sprite.END_GLARE, x, riftY, z, 1.02F * breath * state.size, 0.0F, ARGB.color((int) (150 * state.rise), 0xD8A8FF));
    }

    private static void submitRiftBeam(State state, PedestalLight.Glow glow, float lensY, float riftX, float riftY, float riftZ) {
        float pulse = 0.85F + 0.15F * Mth.sin(state.time / 5.0F), half = 0.045F * pulse * state.size, foot = 0.045F * pulse;
        float fromY = lensY + 0.01F, fx = state.faceX * half, fz = state.faceZ * half, footX = state.faceX * foot, footZ = state.faceZ * foot;
        glow.sprite(PedestalLight.Sprite.END_BEAM_0.playing(state.time),
                riftX - fx, riftY, riftZ - fz, 0.5F - footX, fromY, 0.5F - footZ, 0.5F + footX, fromY, 0.5F + footZ, riftX + fx, riftY, riftZ + fz,
                ARGB.color((int) (235 * state.rise), 0xFFFFFF));
        glow.billboard(PedestalLight.Sprite.APPRAISAL_BLOOM, 0.5F, fromY + 0.01F, 0.5F, 0.11F * pulse, 0.0F, ARGB.color((int) (190 * state.rise), 0xC070FF));
        glow.billboard(PedestalLight.Sprite.APPRAISAL_GLARE, 0.5F, fromY + 0.01F, 0.5F, 0.15F * pulse, 0.0F, ARGB.color((int) (150 * state.rise), 0xE0B0FF));
        glow.billboard(PedestalLight.Sprite.APPRAISAL_BLOOM, riftX, riftY, riftZ, 0.16F * pulse * state.size, 0.0F, ARGB.color((int) (120 * state.rise), 0xC070FF));
        for (int energy = 0; energy < 3; energy++) {
            float t = (state.time / RIFT_PULSE_TICKS + energy / 3.0F) % 1.0F;
            glow.billboard(PedestalLight.Sprite.APPRAISAL_BLOOM, Mth.lerp(t, 0.5F, riftX), Mth.lerp(t, fromY, riftY), Mth.lerp(t, 0.5F, riftZ),
                    0.04F * state.size, 0.0F, ARGB.color((int) (230 * Mth.sin(t * Mth.PI) * state.rise), 0xF0DCFF));
        }
    }

    private static void submitEndIslands(State state, PedestalLight.Glow glow, float middle) {
        for (int island = 0; island < END_ISLANDS; island++) {
            float angle = state.time * 0.006F + island * Mth.TWO_PI / END_ISLANDS;
            float half = END_ISLAND_SIZE[island] * state.size, drift = 0.04F * Mth.sin(state.time / 24.0F + island * 2.1F) * state.size;
            float x = 0.5F + Mth.cos(angle) * 0.5F * state.size, z = 0.5F + Mth.sin(angle) * 0.5F * state.size;
            float y = middle + (island - 1) * 0.16F * state.size + drift;
            upright(state, glow, PedestalLight.Sprite.END_ISLAND_0.frame(island), x, y - half, z, half, half * 2.0F, ARGB.color((int) (250 * state.rise), 0xFFFFFF));
        }
    }
}
