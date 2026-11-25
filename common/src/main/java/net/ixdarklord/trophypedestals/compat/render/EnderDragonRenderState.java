package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.world.entity.Entity;

public class EnderDragonRenderState extends LivingEntityRenderState {
    public float flapTime = -1.0F;

    public EnderDragonRenderState(Entity entity) {
        super(entity);
    }
}
