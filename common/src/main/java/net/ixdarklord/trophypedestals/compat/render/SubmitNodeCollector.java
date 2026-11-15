package net.ixdarklord.trophypedestals.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FormattedCharSequence;

/**
 * 1.20.1's stand-in for 26.1's {@code SubmitNodeCollector}: the same calls, drawing straight into the frame's buffers,
 * so the pedestal renderers are the same code on every version.
 */
public final class SubmitNodeCollector implements OrderedSubmitNodeCollector {
    private final MultiBufferSource buffers;

    public SubmitNodeCollector(MultiBufferSource buffers) {
        this.buffers = buffers;
    }

    public MultiBufferSource buffers() {
        return buffers;
    }

    public OrderedSubmitNodeCollector order(int order) {
        return this;
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType type, CustomGeometryRenderer renderer) {
        VertexConsumer consumer = new VertexConsumer(buffers.getBuffer(type));
        renderer.render(poseStack.last(), consumer);
        consumer.flush();
    }

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean shadow, Font.DisplayMode mode,
                           int light, int color, int background, int outline) {
        Minecraft.getInstance().font.drawInBatch(text, x, y, color, shadow, poseStack.last().pose(), buffers, mode, background, light);
    }

    @FunctionalInterface
    public interface CustomGeometryRenderer {
        void render(PoseStack.Pose pose, VertexConsumer consumer);
    }
}
