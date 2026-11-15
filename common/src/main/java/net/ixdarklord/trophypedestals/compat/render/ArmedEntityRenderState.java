package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.world.entity.Entity;

/**
 * A mob that swings as it attacks, as it's to be drawn (see {@link EntityRenderState}): how far through its swing it
 * is.
 */
public class ArmedEntityRenderState extends LivingEntityRenderState {
    public float attackTime = -1.0F;

    public ArmedEntityRenderState(Entity entity) {
        super(entity);
    }
}
