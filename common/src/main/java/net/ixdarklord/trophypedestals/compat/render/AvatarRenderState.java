package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.world.entity.Entity;

public class AvatarRenderState extends HumanoidRenderState {
    public float fallFlyingTimeInTicks;

    public AvatarRenderState(Entity entity) {
        super(entity);
    }
}
