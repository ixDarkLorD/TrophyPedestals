package net.ixdarklord.trophypedestals.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.function.Consumer;

public final class ModelSizes {
    private static final float MODEL_FEET = 1.501F;

    private ModelSizes() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static float @Nullable [] measure(EntityModel model, Entity mob) {
        try {
            model.setupAnim(mob, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
            float[] bounds = {0.0F, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE};
            model.renderToBuffer(new PoseStack(), new Points(point -> {
                bounds[0] = Math.max(bounds[0], (float) Math.hypot(point.x, point.z));
                bounds[1] = Math.min(bounds[1], point.y);
                bounds[2] = Math.max(bounds[2], point.y);
                bounds[3] = Math.min(bounds[3], point.x);
                bounds[4] = Math.max(bounds[4], point.x);
                bounds[5] = Math.min(bounds[5], point.z);
                bounds[6] = Math.max(bounds[6], point.z);
            }), 0, 0, -1);
            float height = bounds[2] - bounds[1];
            float width = Math.max(0.01F, Math.max(bounds[4] - bounds[3], bounds[6] - bounds[5]));
            return height > 0.01F && bounds[0] > 0.01F ? new float[] {bounds[0], height, Math.max(0.0F, bounds[2] - MODEL_FEET), width} : null;
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void pose(EntityModel model, LivingEntity mob, EntityRenderState state) {
        EntityRenderStates.apply(state);
        float walked = mob.walkAnimation.position(), speed = mob.walkAnimation.speed();
        model.prepareMobModel(mob, walked, speed, 0.0F);
        model.setupAnim(mob, walked, speed, state.ageInTicks, Mth.wrapDegrees(mob.getYHeadRot() - mob.yBodyRot), mob.getXRot());
    }

    public static void visitModel(EntityModel<?> model, Consumer<Vector3f> corners) {
        model.renderToBuffer(new PoseStack(), new Points(corners), 0, 0, -1);
    }

    public static void visitItem(LivingEntity mob, ItemStack stack, boolean leftHand, Consumer<Vector3f> corners) {
        Points points = new Points(corners);
        Minecraft.getInstance().getItemRenderer().renderStatic(mob, stack,
                leftHand ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, leftHand,
                new PoseStack(), type -> points, mob.level(), 0xF000F0, OverlayTexture.NO_OVERLAY, 0);
    }

    private static final class Points implements VertexConsumer {
        private final Consumer<Vector3f> corners;
        private final Vector3f point = new Vector3f();

        Points(Consumer<Vector3f> corners) {
            this.corners = corners;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            corners.accept(point.set(x, y, z));
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
