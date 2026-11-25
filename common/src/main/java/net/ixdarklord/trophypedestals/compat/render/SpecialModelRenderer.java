package net.ixdarklord.trophypedestals.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public interface SpecialModelRenderer<T> {
    void submit(@Nullable T argument, PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outlineColor);

    void getExtents(Consumer<Vector3fc> output);

    @Nullable T extractArgument(ItemStack stack);

    interface Unbaked<T> {
        @Nullable SpecialModelRenderer<T> bake(BakingContext context);

        MapCodec<? extends Unbaked<T>> type();
    }

    final class BakingContext {
        static final BakingContext INSTANCE = new BakingContext();

        private BakingContext() {}
    }
}
