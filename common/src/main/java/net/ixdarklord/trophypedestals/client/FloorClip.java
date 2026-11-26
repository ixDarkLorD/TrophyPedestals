package net.ixdarklord.trophypedestals.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.joml.Matrix4f;
import org.joml.Vector3f;

final class FloorClip implements MultiBufferSource {
    private final MultiBufferSource buffers;
    private final Matrix4f toLocal;

    FloorClip(MultiBufferSource buffers, Matrix4f pose) {
        this.buffers = buffers;
        this.toLocal = new Matrix4f(pose).invert();
    }

    @Override
    public VertexConsumer getBuffer(RenderType type) {
        VertexConsumer inner = buffers.getBuffer(type);
        return type.mode() == VertexFormat.Mode.QUADS ? new Clipped(inner, toLocal) : inner;
    }

    private static final class Clipped implements VertexConsumer {
        private final VertexConsumer inner;
        private final Matrix4f toLocal;
        private final Vector3f local = new Vector3f();
        private final float[][] pos = new float[4][3];
        private final float[][] color = new float[4][4];
        private final float[][] uv = new float[4][2];
        private final float[][] normal = new float[4][3];
        private final int[][] packed = new int[4][2];
        private final float[] height = new float[4];
        private int count;

        Clipped(VertexConsumer inner, Matrix4f toLocal) {
            this.inner = inner;
            this.toLocal = toLocal;
        }

        @Override
        public void vertex(float x, float y, float z, float red, float green, float blue, float alpha, float u, float v, int overlay, int light,
                           float nx, float ny, float nz) {
            int i = count++;
            pos[i][0] = x;
            pos[i][1] = y;
            pos[i][2] = z;
            color[i][0] = red;
            color[i][1] = green;
            color[i][2] = blue;
            color[i][3] = alpha;
            uv[i][0] = u;
            uv[i][1] = v;
            normal[i][0] = nx;
            normal[i][1] = ny;
            normal[i][2] = nz;
            packed[i][0] = overlay;
            packed[i][1] = light;
            height[i] = toLocal.transformPosition(local.set(x, y, z)).y;
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
                inner.vertex(x, y, z, color[i][0], color[i][1], color[i][2], color[i][3], u, v, packed[i][0], packed[i][1],
                        normal[i][0], normal[i][1], normal[i][2]);
            }
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            return inner.vertex(x, y, z);
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            return inner.color(red, green, blue, alpha);
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return inner.uv(u, v);
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return inner.overlayCoords(u, v);
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return inner.uv2(u, v);
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return inner.normal(x, y, z);
        }

        @Override
        public void endVertex() {
            inner.endVertex();
        }

        @Override
        public void defaultColor(int red, int green, int blue, int alpha) {
            inner.defaultColor(red, green, blue, alpha);
        }

        @Override
        public void unsetDefaultColor() {
            inner.unsetDefaultColor();
        }
    }
}
