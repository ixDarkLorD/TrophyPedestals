package net.ixdarklord.trophypedestals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ixdarklord.trophypedestals.compat.render.CameraRenderState;
import net.ixdarklord.trophypedestals.compat.render.EntityRenderState;
import net.ixdarklord.trophypedestals.compat.render.EntityRenderStates;
import net.ixdarklord.trophypedestals.compat.render.SubmitNodeCollector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

final class RenderedBounds implements MultiBufferSource {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final CameraRenderState CAMERA = new CameraRenderState();

    private final Bounds bounds = new Bounds();

    private RenderedBounds() {}

    static float @Nullable [] measure(Entity mob) {
        try {
            EntityRenderState shown = EntityRenderStates.extract(mob, 0.0F);
            shown.lightCoords = FULL_BRIGHT;
            shown.shadowRadius = 0.0F;
            shown.shadowPieces.clear();
            shown.nameTag = null;
            shown.ageInTicks = 0.0F;
            PedestalRenderer.idle(shown, 0.0F);
            RenderedBounds buffers = new RenderedBounds();
            SubmitNodeCollector collector = new SubmitNodeCollector(buffers);
            EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            boolean hitBoxes = dispatcher.shouldRenderHitBoxes();
            dispatcher.setRenderHitBoxes(false);
            try {
                EntityRenderStates.submit(shown, CAMERA, 0.0, 0.0, 0.0, new PoseStack(), collector);
                collector.flush();
            } finally {
                dispatcher.setRenderHitBoxes(hitBoxes);
            }
            Bounds bounds = buffers.bounds;
            if (bounds.empty() || bounds.top - bounds.bottom < 0.01F || bounds.reach < 0.01F) return null;
            float cut = bounds.cut();
            return new float[] {bounds.reach, bounds.bottom, bounds.top, bounds.reachAbove(cut), cut};
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        return bounds;
    }

    private static final class Bounds implements VertexConsumer {
        private static final float WIDER = 1.3F, STRETCH = 0.12F;
        float reach, bottom = Float.MAX_VALUE, top = -Float.MAX_VALUE;
        float[] points = new float[256];
        int count;

        void keep(float y, float r) {
            if (count + 2 > points.length) points = Arrays.copyOf(points, points.length * 2);
            points[count++] = y;
            points[count++] = r;
        }

        float cut() {
            if (-bottom <= top) return bottom;
            // The buried column is as wide as its deeper half; cut a short stretch below the lowest part wider than it.
            float column = 0.0F, deep = bottom / 2.0F, lowest = 0.0F;
            for (int i = 0; i < count; i += 2) if (points[i] <= deep) column = Math.max(column, points[i + 1]);
            for (int i = 0; i < count; i += 2) if (points[i + 1] > column * WIDER) lowest = Math.min(lowest, points[i]);
            return Math.max(bottom, lowest - STRETCH * (top - lowest));
        }

        float reachAbove(float cut) {
            float most = 0.0F;
            for (int i = 0; i < count; i += 2) if (points[i] >= cut) most = Math.max(most, points[i + 1]);
            return most;
        }

        boolean empty() {
            return bottom == Float.MAX_VALUE;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) return this;
            // Reach is the horizontal radius from the centre; height tracks min and max y.
            reach = Math.max(reach, (float) Math.hypot(x, z));
            keep((float) y, (float) Math.hypot(x, z));
            bottom = Math.min(bottom, (float) y);
            top = Math.max(top, (float) y);
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
        public void endVertex() {}

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {}

        @Override
        public void unsetDefaultColor() {}
    }
}
