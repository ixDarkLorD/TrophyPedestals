package net.ixdarklord.trophypedestals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
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
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

/**
 * How big a mob really draws: its renderer is run once into a collector that keeps no geometry, only the corners of
 * what it would have drawn. Unlike the mob's hit box, this sees wings, tails and the models other mods draw far past
 * theirs (a GeckoLib boss included, as it draws through {@link OrderedSubmitNodeCollector#submitCustomGeometry}).
 */
final class RenderedBounds implements SubmitNodeCollector {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final CameraRenderState CAMERA = new CameraRenderState();
    private static final Direction[] FACES = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};

    private final Vector3f corner = new Vector3f();
    private final Bounds bounds = new Bounds();

    private RenderedBounds() {}

    /**
     * The mob as its renderer draws it standing at the origin facing south, in blocks: {reach, bottom, top}, where reach is
     * the farthest it draws from its upright axis. Null when its renderer draws nothing or can't be run this way.
     */
    static float @Nullable [] measure(Entity mob) {
        try {
            EntityRenderState shown = Minecraft.getInstance().getEntityRenderDispatcher().extractEntity(mob, 0.0F);
            shown.lightCoords = FULL_BRIGHT;
            shown.shadowRadius = 0.0F;
            shown.shadowPieces.clear();
            shown.nameTag = null;
            shown.ageInTicks = 0.0F;
            PedestalRenderer.idle(shown, 0.0F);
            RenderedBounds collector = new RenderedBounds();
            Minecraft.getInstance().getEntityRenderDispatcher().submit(shown, CAMERA, 0.0, 0.0, 0.0, new PoseStack(), collector);
            Bounds bounds = collector.bounds;
            if (bounds.empty() || bounds.top - bounds.bottom < 0.01F || bounds.reach < 0.01F) return null;
            return new float[] {bounds.reach, bounds.bottom, bounds.top};
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    private void include(Matrix4f pose, Vector3fc point) {
        bounds.addVertex(corner.set(point).mulPosition(pose));
    }

    private void include(PoseStack poseStack, List<BakedQuad> quads) {
        Matrix4f pose = poseStack.last().pose();
        for (BakedQuad quad : quads) {
            for (int i = 0; i < BakedQuad.VERTEX_COUNT; i++) include(pose, quad.position(i));
        }
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        return this;
    }

    // model parts reach here too: 26.3 sends them through submitModel as a Model.Simple
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int light, int overlay, int tint,
                                @Nullable UvMapping uvMapping, int outline) {
        ((Model) model).setupAnim(state);
        model.renderToBuffer(poseStack, bounds, light, overlay, tint);
    }

    @Override
    public void submitBlockModel(PoseStack poseStack, RenderType renderType, List<BlockStateModelPart> parts, int[] tints, int light, int overlay, int outline) {
        for (BlockStateModelPart part : parts) {
            for (Direction face : FACES) include(poseStack, part.getQuads(face));
        }
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext context, int light, int overlay, int outline, int[] tints, ItemQuads quads,
                           ItemStackRenderState.FoilType foil) {
        include(poseStack, quads.all());
    }

    @Override
    public void submitCustomGeometry(PoseStack poseStack, RenderType renderType, CustomGeometryRenderer renderer) {
        renderer.render(poseStack.last(), bounds);
    }

    // shadows, fire, leads, names, outlines, overlays, particles and gizmos are no part of the mob's own size
    @Override
    public <S> void submitCrumblingOverlay(Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int light, int overlay, int tint,
                                          ModelFeatureRenderer.CrumblingOverlay crumbling) {}

    @Override
    public void submitShadow(PoseStack poseStack, float radius, List<EntityRenderState.ShadowPiece> pieces) {}

    @Override
    public void submitNameTag(PoseStack poseStack, @Nullable Vec3 at, int offset, Component name, boolean seeThrough, int light, CameraRenderState camera) {}

    @Override
    public void submitText(PoseStack poseStack, float x, float y, FormattedCharSequence text, boolean shadow, Font.DisplayMode mode, int light, int color,
                           int background, int outline) {}

    @Override
    public void submitTextBackground(PoseStack poseStack, float x0, float y0, float x1, float y1, int color, Font.DisplayMode mode, int light) {}

    @Override
    public void submitFlame(PoseStack poseStack, EntityRenderState state, Quaternionf rotation) {}

    @Override
    public void submitLeash(PoseStack poseStack, EntityRenderState.LeashState leash) {}

    @Override
    public void submitMovingBlock(PoseStack poseStack, MovingBlockRenderState block, int light) {}

    @Override
    public void submitBreakingBlockModel(PoseStack poseStack, List<BlockStateModelPart> parts, int progress, boolean flag) {}

    @Override
    public void submitShapeOutline(PoseStack poseStack, VoxelShape shape, RenderType renderType, int color, float width, boolean flag) {}

    @Override
    public void submitQuadParticleGroup(QuadParticleRenderState particles) {}

    @Override
    public void submitGizmoPrimitives(DrawableGizmoPrimitives.Group gizmos, CameraRenderState camera, boolean flag) {}

    /** A vertex consumer that keeps only how far the vertices it is given reach. */
    private static final class Bounds implements VertexConsumer {
        float reach, bottom = Float.MAX_VALUE, top = -Float.MAX_VALUE;

        boolean empty() {
            return bottom == Float.MAX_VALUE;
        }

        void addVertex(Vector3f point) {
            addVertex(point.x, point.y, point.z);
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) return this;
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
        public VertexConsumer setColor(int color) {
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
        public VertexConsumer setUv3(float u, float v) {
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            return this;
        }
    }
}
