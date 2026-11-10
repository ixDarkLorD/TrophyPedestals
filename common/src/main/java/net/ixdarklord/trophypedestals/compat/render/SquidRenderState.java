package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.world.entity.Entity;

/** A squid as it's to be drawn (see {@link EntityRenderState}): how its body is tipped and turned, in degrees. */
public class SquidRenderState extends LivingEntityRenderState {
    /** NaN leaves its own. */
    public float xBodyRot = Float.NaN, zBodyRot = Float.NaN;

    public SquidRenderState(Entity entity) {
        super(entity);
    }
}
