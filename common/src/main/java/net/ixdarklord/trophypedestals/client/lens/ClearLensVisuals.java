package net.ixdarklord.trophypedestals.client.lens;

import net.ixdarklord.trophypedestals.client.PedestalLight;
import net.ixdarklord.trophypedestals.client.PedestalRenderer;
import net.ixdarklord.trophypedestals.client.PedestalRenderer.State;
import net.ixdarklord.trophypedestals.compat.ARGB;
import net.minecraft.util.Mth;

final class ClearLensVisuals extends LensVisuals {
    @Override
    protected void draw(State state, PedestalLight.Glow glow, float up, float lift, float middle, int color) {
        submitAura(state, glow, color, 35.0F);
        float shimmer = 0.75F + 0.25F * Mth.sin(state.time / 11.0F);
        glow.flat(PedestalLight.Sprite.SUNBURST, lift, LENS_DISC * 1.15F, state.time * 0.006F, ARGB.color((int) (72 * shimmer * state.rise), state.rarity.accent()));
        glow.flat(PedestalLight.Sprite.SUNBURST, lift + 0.002F, LENS_DISC * 0.8F, -state.time * 0.01F, ARGB.color((int) (42 * state.rise), 0xFFFFFF));
        submitClearCrescent(state, glow);
    }

    private static void submitClearCrescent(State state, PedestalLight.Glow glow) {
        float pulse = 0.7F + 0.3F * Mth.sin(state.time / 17.0F);
        glow.flat(PedestalLight.Sprite.LENS_CLEAR_ARC, PedestalRenderer.GLASS_TOP + LENS_BASE_LIFT, LENS_BASE_HALF, state.time * 0.018F, ARGB.color((int) (165 * pulse * state.rise), state.rarity.accent()));
    }
}
