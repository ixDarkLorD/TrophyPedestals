package net.ixdarklord.trophypedestals.compat.render;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * 1.20.1's stand-in for 1.21's vertex calls ({@code addVertex(pose, x, y, z).setColor(...).setUv(...)...}) over
 * 1.20.1's buffers, which take a vertex's parts in its format's order and then {@code endVertex()}.
 */
public final class VertexConsumer {
    private static final Vector4f POSITION = new Vector4f();
    private static final Vector3f NORMAL = new Vector3f();
    private final com.mojang.blaze3d.vertex.VertexConsumer buffer;
    private boolean open;
    private PoseStack.Pose pose;
    private float x, y, z, u, v, normalX, normalY = 1.0F, normalZ;
    private int color = 0xFFFFFFFF, overlay = -1, light = -1;
    private boolean hasColor, hasUv, hasNormal;
    private PoseStack.Pose normalPose;

    public VertexConsumer(com.mojang.blaze3d.vertex.VertexConsumer buffer) {
        this.buffer = buffer;
    }

    public VertexConsumer addVertex(PoseStack.Pose pose, float x, float y, float z) {
        flush();
        open = true;
        this.pose = pose;
        this.x = x;
        this.y = y;
        this.z = z;
        hasColor = hasUv = hasNormal = false;
        overlay = light = -1;
        return this;
    }

    public VertexConsumer setColor(int argb) {
        color = argb;
        hasColor = true;
        return this;
    }

    public VertexConsumer setUv(float u, float v) {
        this.u = u;
        this.v = v;
        hasUv = true;
        return this;
    }

    public VertexConsumer setOverlay(int overlay) {
        this.overlay = overlay;
        return this;
    }

    public VertexConsumer setLight(int light) {
        this.light = light;
        return this;
    }

    public VertexConsumer setNormal(PoseStack.Pose pose, float x, float y, float z) {
        normalPose = pose;
        normalX = x;
        normalY = y;
        normalZ = z;
        hasNormal = true;
        return this;
    }

    public void flush() {
        if (!open) return;
        open = false;
        if (hasColor && hasUv && overlay != -1 && light != -1 && hasNormal) {
            Vector4f at = pose.pose().transform(POSITION.set(x, y, z, 1.0F));
            Vector3f normal = normalPose.normal().transform(NORMAL.set(normalX, normalY, normalZ));
            buffer.vertex(at.x(), at.y(), at.z(), (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F,
                    (color >>> 24) / 255.0F, u, v, overlay, light, normal.x(), normal.y(), normal.z());
            return;
        }
        buffer.vertex(pose.pose(), x, y, z);
        if (hasColor) buffer.color(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, color >>> 24);
        if (hasUv) buffer.uv(u, v);
        if (overlay != -1) buffer.overlayCoords(overlay);
        if (light != -1) buffer.uv2(light);
        if (hasNormal) buffer.normal(normalPose.normal(), normalX, normalY, normalZ);
        buffer.endVertex();
    }
}
