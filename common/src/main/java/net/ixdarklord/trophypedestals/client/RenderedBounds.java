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
            return new float[] {bounds.reach, bounds.bottom, bounds.top};
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        return bounds;
    }

    private static final class Bounds implements VertexConsumer {
        float reach, bottom = Float.MAX_VALUE, top = -Float.MAX_VALUE;

        boolean empty() {
            return bottom == Float.MAX_VALUE;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) return this;
            // Reach is the horizontal radius from the centre; height tracks min and max y.
            reach = Math.max(reach, (float) Math.hypot(x, z));
            bottom = Math.min(bottom, y);
            top = Math.max(top, y);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }
    }
}
