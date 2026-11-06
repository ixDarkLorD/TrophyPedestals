package net.ixdarklord.trophypedestals.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * The size of a mob's model at rest, for the models 26.1 measures by their tree of parts but 1.20.1 builds otherwise
 * (a pig's, a fox's, a cat's: a list of parts with no root). The model is posed as it stands still and drawn into a
 * buffer that only notes where its corners land. As 26.1's measure: {reach from its middle, height}, in blocks.
 */
public final class ModelSizes {
    private ModelSizes() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static float @Nullable [] measure(EntityModel model, Entity mob) {
        try {
            // (Its body is laid down and its legs set in its pose: at rest, standing still.)
            model.setupAnim(mob, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
            Bounds bounds = new Bounds();
            model.renderToBuffer(new PoseStack(), bounds, 0, 0, 1.0F, 1.0F, 1.0F, 1.0F);
            float height = bounds.maxY - bounds.minY;
            return height > 0.01F && bounds.reach > 0.01F ? new float[] {bounds.reach, height} : null;
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    // Notes only where each corner lands. (This package has a VertexConsumer of its own: the import makes this one the game's.)
    private static final class Bounds implements VertexConsumer {
        float reach, minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            reach = Math.max(reach, (float) Math.hypot(x, z));
            minY = Math.min(minY, (float) y);
            maxY = Math.max(maxY, (float) y);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return this;
        }

        @Override
        public void endVertex() {
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
        }

        @Override
        public void unsetDefaultColor() {
        }
    }
}
