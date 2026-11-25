package net.ixdarklord.trophypedestals.compat.render;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SpecialModelRendererRegistry {
    private static final Map<ResourceLocation, MapCodec<? extends SpecialModelRenderer.Unbaked<?>>> TYPES = new ConcurrentHashMap<>();

    private SpecialModelRendererRegistry() {}

    public static void register(ResourceLocation id, MapCodec<? extends SpecialModelRenderer.Unbaked<?>> codec) {
        TYPES.put(id, codec);
    }

    static @Nullable MapCodec<? extends SpecialModelRenderer.Unbaked<?>> get(ResourceLocation id) {
        return TYPES.get(id);
    }
}
