package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EntityRenderState {
    public final Entity entity;
    public int lightCoords;
    public float shadowRadius;
    public final List<Object> shadowPieces = new ArrayList<>();
    public @Nullable Component nameTag;
    public float ageInTicks;

    public EntityRenderState(Entity entity) {
        this.entity = entity;
    }
}
