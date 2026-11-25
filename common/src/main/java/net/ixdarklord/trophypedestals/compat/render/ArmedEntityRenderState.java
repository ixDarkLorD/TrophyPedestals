package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.world.entity.Entity;

public class ArmedEntityRenderState extends LivingEntityRenderState {
    public float attackTime = -1.0F;

    public ArmedEntityRenderState(Entity entity) {
        super(entity);
    }
}
