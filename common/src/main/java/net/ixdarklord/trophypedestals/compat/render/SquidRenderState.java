package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.world.entity.Entity;

public class SquidRenderState extends LivingEntityRenderState {
    public float xBodyRot = Float.NaN, zBodyRot = Float.NaN;

    public SquidRenderState(Entity entity) {
        super(entity);
    }
}
