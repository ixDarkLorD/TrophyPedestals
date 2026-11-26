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
        private final float[][] uv = new float[4][2];
        private final float[][] normal = new float[4][3];
        private final int[][] packed = new int[4][3];
        private final float[] height = new float[4];
        private int count;

        Clipped(VertexConsumer inner, Matrix4f toLocal) {
            this.inner = inner;
            this.toLocal = toLocal;
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
        public VertexConsumer setNormal(float x, float y, float z) {
            return inner.setNormal(x, y, z);
        }
    }
}
