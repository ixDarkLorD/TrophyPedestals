package net.ixdarklord.trophypedestals.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FormattedCharSequence;

/**
 * 1.21.1's stand-in for 26.1's {@code SubmitNodeCollector}: the same calls, drawing straight into the frame's buffers,
 * so the pedestal renderers are the same code on every version. 1.21.1 has no render passes to put things in:
 * {@link #order} hands back this same collector, and what's drawn goes in the order it's drawn (each render type's
 * buffer drawn as it's switched from).
 */
public final class SubmitNodeCollector implements OrderedSubmitNodeCollector {
    private final MultiBufferSource buffers;

    public SubmitNodeCollector(MultiBufferSource buffers) {
        this.buffers = buffers;
    }

    /** The buffers it draws into. */
    public MultiBufferSource buffers() {
        return buffers;
    }

    public OrderedSubmitNodeCollector order(int order) {
        return this;
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType type, CustomGeometryRenderer renderer) {
        renderer.render(poseStack.last(), buffers.getBuffer(type));
    }

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean shadow, Font.DisplayMode mode,
                           int light, int color, int background, int outline) {
        Minecraft.getInstance().font.drawInBatch(text, x, y, color, shadow, poseStack.last().pose(), buffers, mode, background, light);
    }

    /** Draws its own geometry into the buffer it's given. */
    @FunctionalInterface
    public interface CustomGeometryRenderer {
        void render(PoseStack.Pose pose, VertexConsumer consumer);
    }
}
