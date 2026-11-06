package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.jetbrains.annotations.Nullable;

/** A living mob as it's to be drawn (see {@link EntityRenderState}): its pose and walk cycle. */
public class LivingEntityRenderState extends EntityRenderState {
    public @Nullable Pose pose;
    public @Nullable Direction bedOrientation;
    /** Where in its walk cycle it is, and how fast it walks; a negative speed leaves its own. */
    public float walkAnimationPos, walkAnimationSpeed = -1.0F;

    public LivingEntityRenderState(Entity entity) {
        super(entity);
    }
}
