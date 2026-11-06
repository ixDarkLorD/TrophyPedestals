package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.21.1's stand-in for 26.1's {@code EntityRenderState}: a mob as it's to be drawn. 1.21.1 draws the mob itself, so
 * this keeps the mob and what's set on it, which {@link EntityRenderStates#submit} puts on the mob just before drawing it.
 * Its subclasses mirror 26.1's (for the same {@code instanceof} checks) with the fields Trophy Pedestals sets.
 */
public class EntityRenderState {
    public final Entity entity;
    public int lightCoords;
    public float shadowRadius;
    public final List<Object> shadowPieces = new ArrayList<>();
    public @Nullable Component nameTag;
    /** The mob's age in ticks, with the frame's partial tick: what its model animates by. */
    public float ageInTicks;

    public EntityRenderState(Entity entity) {
        this.entity = entity;
    }
}
