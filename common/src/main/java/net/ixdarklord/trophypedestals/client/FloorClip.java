package net.ixdarklord.trophypedestals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.gizmos.DrawableGizmoPrimitives;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

final class FloorClip {
    private final Matrix4f toLocal;

    FloorClip(Matrix4f pose) {
        this.toLocal = new Matrix4f(pose).invert();
    }

    SubmitNodeCollector collector(SubmitNodeCollector collector) {
        return new Collector(this, collector, collector);
    }

    private VertexConsumer clipped(RenderType type, VertexConsumer consumer) {
        return type.primitiveTopology() == PrimitiveTopology.QUADS ? new Clipped(this, consumer) : consumer;
    }

    private static final class Collector implements SubmitNodeCollector {
        private final FloorClip clip;
        private final SubmitNodeCollector root;
        private final OrderedSubmitNodeCollector inner;

        Collector(FloorClip clip, SubmitNodeCollector root, OrderedSubmitNodeCollector inner) {
            this.clip = clip;
            this.root = root;
            this.inner = inner;
        }

        @Override
        public OrderedSubmitNodeCollector order(int order) {
            return new Collector(clip, root, root.order(order));
        }

        @Override
        public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int light, int overlay, int tint,
                                    @Nullable UvMapping uvMapping, int outline) {
            inner.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> {
                PoseStack at = new PoseStack();
                at.last().set(pose);
                model.setupAnim(state);
                model.renderToBuffer(at, clip.clipped(renderType, uvMapping == null ? consumer : uvMapping.wrap(consumer)), light, overlay, tint);
            });
        }

        @Override
        public <S> void submitCrumblingOverlay(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int light, int overlay, int tint,
                                              ModelFeatureRenderer.CrumblingOverlay crumbling) {}

        @Override
        public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer renderer) {
            inner.submitCustomGeometry(poseStack, renderType, (pose, consumer) -> renderer.render(pose, clip.clipped(renderType, consumer)));
        }

        @Override
        public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tints, int light, int overlay, int outline) {
            inner.submitBlockModel(poseStack, renderType, parts, tints, light, overlay, outline);
        }

        @Override
        public void submitItem(PoseStack poseStack, ItemDisplayContext context, int light, int overlay, int outline, int[] tints, ItemQuads quads,
                               ItemStackRenderState.FoilType foil) {
            inner.submitItem(poseStack, context, light, overlay, outline, tints, quads, foil);
        }

        @Override
        public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {
            inner.submitShadow(poseStack, radius, pieces);
        }

        @Override
        public void submitNameTag(PoseStack poseStack, @Nullable Vec3 at, int offset, Component name, boolean seeThrough, int light, CameraRenderState camera) {
            inner.submitNameTag(poseStack, at, offset, name, seeThrough, light, camera);
        }

        @Override
        public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean shadow, Font.DisplayMode mode, int light, int color,
                               int background, int outline) {
            inner.submitText(poseStack, x, y, text, shadow, mode, light, color, background, outline);
        }

        @Override
        public void submitTextBackground(PoseStack poseStack, float x0, float y0, float x1, float y1, int color, Font.DisplayMode mode, int light) {
            inner.submitTextBackground(poseStack, x0, y0, x1, y1, color, mode, light);
        }

        @Override
        public void submitFlame(PoseStack poseStack, EntityRenderState state, Quaternionf rotation) {
            inner.submitFlame(poseStack, state, rotation);
        }

        @Override
        public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leash) {
            inner.submitLeash(poseStack, leash);
        }

        @Override
        public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState block, int light) {
            inner.submitMovingBlock(poseStack, block, light);
        }

        @Override
        public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress, boolean flag) {
            inner.submitBreakingBlockModel(poseStack, parts, progress, flag);
        }

        @Override
        public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float width, boolean flag) {
            inner.submitShapeOutline(poseStack, shape, renderType, color, width, flag);
        }

        @Override
        public void submitQuadParticleGroup(QuadParticleRenderState particles) {
            inner.submitQuadParticleGroup(particles);
        }

        @Override
        public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group gizmos, CameraRenderState camera, boolean flag) {
            inner.submitGizmoPrimitives(gizmos, camera, flag);
        }
    }

    private static final class Clipped implements VertexConsumer {
        private final FloorClip clip;
        private final VertexConsumer inner;
        private final Vector3f local = new Vector3f();
        private final float[][] pos = new float[4][3];
        private final float[][] uv = new float[4][2];
        private final float[][] normal = new float[4][3];
        private final int[][] packed = new int[4][3];
        private final float[] height = new float[4];
        private int count;

        Clipped(FloorClip clip, VertexConsumer inner) {
            this.clip = clip;
            this.inner = inner;
        }

        @Override
        public void addVertex(float x, float y, float z, int color, float u, float v, int overlay, int light, float nx, float ny, float nz) {
            int i = count++;
            pos[i][0] = x;
            pos[i][1] = y;
            pos[i][2] = z;
            uv[i][0] = u;
            uv[i][1] = v;
            normal[i][0] = nx;
            normal[i][1] = ny;
            normal[i][2] = nz;
            packed[i][0] = color;
            packed[i][1] = overlay;
            packed[i][2] = light;
            height[i] = clip.toLocal.transformPosition(local.set(x, y, z)).y;
            if (count == 4) {
                count = 0;
                emit();
            }
        }

        private void emit() {
            if (height[0] < 0.0F && height[1] < 0.0F && height[2] < 0.0F && height[3] < 0.0F) return;
            for (int i = 0; i < 4; i++) {
                float x = pos[i][0], y = pos[i][1], z = pos[i][2], u = uv[i][0], v = uv[i][1];
                if (height[i] < 0.0F) {
                    int above = height[(i + 3) % 4] >= 0.0F ? (i + 3) % 4 : height[(i + 1) % 4] >= 0.0F ? (i + 1) % 4 : -1;
                    if (above >= 0) {
                        // Slide the buried corner along its edge to where that edge meets the floor.
                        float t = height[i] / (height[i] - height[above]);
                        x += (pos[above][0] - x) * t;
                        y += (pos[above][1] - y) * t;
                        z += (pos[above][2] - z) * t;
                        u += (uv[above][0] - u) * t;
                        v += (uv[above][1] - v) * t;
                    }
                }
                inner.addVertex(x, y, z, packed[i][0], u, v, packed[i][1], packed[i][2], normal[i][0], normal[i][1], normal[i][2]);
            }
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            return inner.addVertex(x, y, z);
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return inner.setColor(red, green, blue, alpha);
        }

        @Override
        public VertexConsumer setColor(int color) {
            return inner.setColor(color);
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            return inner.setUv(u, v);
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return inner.setUv1(u, v);
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return inner.setUv2(u, v);
        }

        @Override
        public VertexConsumer setUv3(float u, float v) {
            return inner.setUv3(u, v);
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return inner.setNormal(x, y, z);
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return inner.setLineWidth(width);
        }
    }
}
