package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.world.entity.Entity;

/** A player-shaped figure as it's to be drawn (see {@link EntityRenderState}): how long it has been gliding. */
public class AvatarRenderState extends HumanoidRenderState {
    public float fallFlyingTimeInTicks;

    public AvatarRenderState(Entity entity) {
        super(entity);
    }
}
