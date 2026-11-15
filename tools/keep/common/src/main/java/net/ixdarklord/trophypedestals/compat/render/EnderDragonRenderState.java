package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.world.entity.Entity;

/** The ender dragon as it's to be drawn (see {@link EntityRenderState}): where its wings are in their beat. */
public class EnderDragonRenderState extends LivingEntityRenderState {
    public float flapTime = -1.0F;

    public EnderDragonRenderState(Entity entity) {
        super(entity);
    }
}
