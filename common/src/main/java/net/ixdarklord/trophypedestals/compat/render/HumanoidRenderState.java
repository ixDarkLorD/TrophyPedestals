package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.world.entity.Entity;

/** A humanoid as it's to be drawn (see {@link EntityRenderState}): crouching, swimming or gliding. */
public class HumanoidRenderState extends ArmedEntityRenderState {
    public boolean isCrouching, isVisuallySwimming, isFallFlying;
    public float swimAmount;

    public HumanoidRenderState(Entity entity) {
        super(entity);
    }
}
