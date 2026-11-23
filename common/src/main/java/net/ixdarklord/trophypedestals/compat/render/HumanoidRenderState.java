package net.ixdarklord.trophypedestals.compat.render;

import net.ixdarklord.trophypedestals.client.PosedFigure;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/** A humanoid as it's to be drawn (see {@link EntityRenderState}): crouching, swimming or gliding. */
public class HumanoidRenderState extends ArmedEntityRenderState implements PosedFigure {
    public boolean isCrouching, isVisuallySwimming, isFallFlying;
    public float swimAmount;
    private float @Nullable [] pose;

    public HumanoidRenderState(Entity entity) {
        super(entity);
    }

    @Override
    public float @Nullable [] trophypedestals$pose() {
        return pose;
    }

    @Override
    public void trophypedestals$setPose(float @Nullable [] pose) {
        this.pose = pose;
    }
}
