package net.ixdarklord.trophypedestals.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * 1.20.1's stand-in for 26.1's {@code SubmitNodeCollector}: the same calls, drawing straight into the frame's buffers,
 * with what's submitted at a later order kept until {@link #flush}, as 26.1 draws it after the rest.
 */
public final class SubmitNodeCollector implements OrderedSubmitNodeCollector {
    private final MultiBufferSource buffers;
    private final List<Later> later = new ArrayList<>();

    public SubmitNodeCollector(MultiBufferSource buffers) {
        this.buffers = buffers;
    }

    public MultiBufferSource buffers() {
        return buffers;
    }

    public OrderedSubmitNodeCollector order(int order) {
        return order <= 0 ? this : new Ordered(order);
    }

    public void flush() {
        flush(false);
    }

    /**
     * Draws what was kept for later. With {@code itemFirst}, what's been drawn into the item sheets so far (a held trophy's
     * item) is finished first, so a light kept for later can't hide it, as it would if its own buffer were drawn first.
     */
    public void flush(boolean itemFirst) {
        if (later.isEmpty()) return;
        if (itemFirst && buffers instanceof MultiBufferSource.BufferSource source) {
            source.endBatch(Sheets.solidBlockSheet());
            source.endBatch(Sheets.cutoutBlockSheet());
            source.endBatch(Sheets.translucentItemSheet());
        }
        later.sort(Comparator.comparingInt(Later::order));
        for (Later draw : later) {
            PoseStack poseStack = new PoseStack();
            poseStack.last().pose().set(draw.pose);
            poseStack.last().normal().set(draw.normal);
            draw.draw.accept(poseStack);
        }
        later.clear();
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

    private record Later(int order, Matrix4f pose, Matrix3f normal, Consumer<PoseStack> draw) {}

    private final class Ordered implements OrderedSubmitNodeCollector {
        private final int order;

        private Ordered(int order) {
            this.order = order;
        }

        private void keep(PoseStack poseStack, Consumer<PoseStack> draw) {
            later.add(new Later(order, new Matrix4f(poseStack.last().pose()), new Matrix3f(poseStack.last().normal()), draw));
        }

        @Override
        public void submitCustomGeometry(PoseStack poseStack, RenderType type, CustomGeometryRenderer renderer) {
            keep(poseStack, kept -> SubmitNodeCollector.this.submitCustomGeometry(kept, type, renderer));
        }

        @Override
        public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean shadow, Font.DisplayMode mode,
                               int light, int color, int background, int outline) {
            keep(poseStack, kept -> SubmitNodeCollector.this.submitText(kept, x, y, text, shadow, mode, light, color, background, outline));
        }
    }

    @FunctionalInterface
    public interface CustomGeometryRenderer {
        void render(PoseStack.Pose pose, VertexConsumer consumer);
    }
}
