package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.world.entity.Entity;

/** A mob that swings as it attacks, as it's to be drawn (see {@link EntityRenderState}): how far through its swing it is. */
public class ArmedEntityRenderState extends LivingEntityRenderState {
    /** 0 to 1 through its swing; a negative value leaves its own. */
    public float attackTime = -1.0F;

    public ArmedEntityRenderState(Entity entity) {
        super(entity);
    }
}
