package net.ixdarklord.trophypedestals.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 1.21.1's stand-in for 26.1's {@code ItemStackRenderState}: an item, the model it's drawn with and how it's held,
 * filled by {@link ItemModelResolver#updateForTopItem} and drawn by the item renderer.
 */
public final class ItemStackRenderState {
    // A baked quad's vertices: eight ints each, the position first.
    private static final int VERTEX_SIZE = 8;
    ItemStack stack = ItemStack.EMPTY;
    ItemDisplayContext context = ItemDisplayContext.NONE;
    @Nullable BakedModel model;
    private @Nullable AABB box;

    public boolean isEmpty() {
        return stack.isEmpty() || model == null;
    }

    public void clear() {
        stack = ItemStack.EMPTY;
        model = null;
        box = null;
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, int overlay, int outline) {
        if (isEmpty()) return;
        Minecraft.getInstance().getItemRenderer().render(stack, context, false, poseStack, collector.buffers(), light, overlay, model);
    }

    /**
     * The model's box as it's drawn (its display transform for how it's held applied, about its middle), in the item's
     * units; empty for an item drawn by code alone.
     */
    public AABB getModelBoundingBox() {
        if (box != null) return box;
        if (isEmpty()) return box = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        PoseStack pose = new PoseStack();
        model.getTransforms().getTransform(context).apply(false, pose);
        pose.translate(-0.5F, -0.5F, -0.5F);
        Matrix4f matrix = pose.last().pose();
        Vector3f corner = new Vector3f();
        float[] bounds = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        RandomSource random = RandomSource.create(42L);
        for (Direction direction : Direction.values()) include(model.getQuads(null, direction, random), matrix, corner, bounds);
        include(model.getQuads(null, null, random), matrix, corner, bounds);
        box = bounds[0] > bounds[3] ? new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
                : new AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5]);
        return box;
    }

    private static void include(Iterable<BakedQuad> quads, Matrix4f matrix, Vector3f corner, float[] bounds) {
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices();
            for (int i = 0; i + 2 < vertices.length; i += VERTEX_SIZE) {
                corner.set(Float.intBitsToFloat(vertices[i]), Float.intBitsToFloat(vertices[i + 1]), Float.intBitsToFloat(vertices[i + 2])).mulPosition(matrix);
                bounds[0] = Math.min(bounds[0], corner.x);
                bounds[1] = Math.min(bounds[1], corner.y);
                bounds[2] = Math.min(bounds[2], corner.z);
                bounds[3] = Math.max(bounds[3], corner.x);
                bounds[4] = Math.max(bounds[4], corner.y);
                bounds[5] = Math.max(bounds[5], corner.z);
            }
        }
    }
}
