package net.ixdarklord.trophypedestals.mixin.client;

import net.ixdarklord.trophypedestals.client.PosedFigure;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(HumanoidRenderState.class)
public class HumanoidRenderStateMixin implements PosedFigure {
    @Unique
    private float @Nullable [] trophypedestals$pose;

    @Override
    public float @Nullable [] trophypedestals$pose() {
        return trophypedestals$pose;
    }

    @Override
    public void trophypedestals$setPose(float @Nullable [] pose) {
        trophypedestals$pose = pose;
    }
}
