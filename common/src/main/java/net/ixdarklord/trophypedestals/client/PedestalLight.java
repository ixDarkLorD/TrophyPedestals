package net.ixdarklord.trophypedestals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxBlend;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldVfx;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.compat.ARGB;
import net.ixdarklord.trophypedestals.compat.render.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

/** Where a pedestal's light goes: its glows, outlines, beam, halos, crown shards and flares. */
public final class PedestalLight {
    private static final VfxMaterial FADE = new VfxMaterial(TrophyPedestalsMod.id("textures/misc/corner_fade.png"), VfxBlend.ADDITIVE, 0.0F);
    private static final int FADE_SIZE = 64;
    private static final VfxMaterial LENS = new VfxMaterial(VfxMaterial.GLOW, VfxBlend.ADDITIVE, 0.0F);
    private static final int QUAD = 5;
    private static final int MAX_QUADS = 1 << 16;

    private static double[] quadCorners = new double[12 * 1024];
    private static float[] quads = new float[QUAD * 1024];
    private static int quadCount;
    private static double[] lensCenters = new double[3 * 64];
    private static float[] lenses = new float[2 * 64];
    private static int lensCount;
    private static double[] flareCenters = new double[3 * 64];
    private static float[] flares = new float[3 * 64];
    private static int flareCount;
    private static VfxMaterial flareMaterial;

    private PedestalLight() {
    }

    public static void init(ResourceLocation flareTexture) {
        flareMaterial = new VfxMaterial(flareTexture, VfxBlend.ALPHA, 0.0F);
        WorldVfx.register(TrophyPedestalsMod.id("pedestal_light"), PedestalLight::render);
    }

    private static void render(VfxFrame frame) {
        if (quadCount > 0) {
            VfxDraw draw = frame.draw(FADE);
            for (int i = 0; i < quadCount; i++) {
                int at = i * QUAD, c = i * 12;
                double[] p = quadCorners;
                draw.quad(new Vec3(p[c], p[c + 1], p[c + 2]), new Vec3(p[c + 3], p[c + 4], p[c + 5]),
                        new Vec3(p[c + 6], p[c + 7], p[c + 8]), new Vec3(p[c + 9], p[c + 10], p[c + 11]),
                        quads[at + 1], quads[at + 3], quads[at + 2], quads[at + 4], Float.floatToRawIntBits(quads[at]));
            }
        }
        if (lensCount > 0) {
            VfxDraw draw = frame.draw(LENS);
            for (int i = 0; i < lensCount; i++) {
                int at = i * 2, c = i * 3;
                draw.flat(lensCenters[c], lensCenters[c + 1], lensCenters[c + 2], lenses[at], 0.0F, Float.floatToRawIntBits(lenses[at + 1]));
            }
        }
        if (flareCount > 0) {
            VfxDraw draw = frame.draw(flareMaterial);
            for (int i = 0; i < flareCount; i++) {
                int at = i * 3, c = i * 3;
                draw.billboard(flareCenters[c], flareCenters[c + 1], flareCenters[c + 2], flares[at], flares[at + 1], Float.floatToRawIntBits(flares[at + 2]));
            }
        }
        quadCount = lensCount = flareCount = 0;
    }

    static void flare(double x, double y, double z, float half, float roll, int argb) {
        if (ARGB.alpha(argb) == 0) return;
        if ((flareCount + 1) * 3 > flares.length) {
            flares = Arrays.copyOf(flares, flares.length * 2);
            flareCenters = Arrays.copyOf(flareCenters, flareCenters.length * 2);
        }
        int at = flareCount++ * 3;
        flareCenters[at] = x;
        flareCenters[at + 1] = y;
        flareCenters[at + 2] = z;
        flares[at] = half;
        flares[at + 1] = roll;
        flares[at + 2] = Float.intBitsToFloat(argb);
    }

    interface Glow {
        void quad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                  float dx, float dy, float dz, int a, int b, int c, int d);

        void lens(float y, float radius, int color, int segments);
    }

    static Glow world(BlockPos pos) {
        return world(pos.getX(), pos.getY(), pos.getZ(), 1.0F);
    }

    static Glow world(double x, double y, double z, float scale) {
        WORLD.x = x;
        WORLD.y = y;
        WORLD.z = z;
        WORLD.scale = scale;
        return WORLD;
    }

    static Glow itemSheet(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite white, Detail detail) {
        return new ItemSheet(consumer, pose, (white.getU0() + white.getU1()) / 2.0F, (white.getV0() + white.getV1()) / 2.0F, detail.pieceSize);
    }

    enum Detail {
        WHOLE(Float.MAX_VALUE),
        NORMAL(0.06F),
        FINE(0.03F);

        private final float pieceSize;

        Detail(float pieceSize) {
            this.pieceSize = pieceSize;
        }
    }

    static Glow additive(VertexConsumer consumer, PoseStack.Pose pose) {
        return new Additive(consumer, pose);
    }

    private record Additive(VertexConsumer consumer, PoseStack.Pose pose) implements Glow {
        @Override
        public void quad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                         float dx, float dy, float dz, int a, int b, int c, int d) {
            if ((ARGB.alpha(a) | ARGB.alpha(b) | ARGB.alpha(c) | ARGB.alpha(d)) == 0) return;
            triangle(ax, ay, az, a, bx, by, bz, b, cx, cy, cz, c);
            triangle(ax, ay, az, a, cx, cy, cz, c, dx, dy, dz, d);
        }

        private void triangle(float x0, float y0, float z0, int c0, float x1, float y1, float z1, int c1, float x2, float y2, float z2, int c2) {
            consumer.addVertex(pose, x0, y0, z0).setColor(c0);
            consumer.addVertex(pose, x1, y1, z1).setColor(c1);
            consumer.addVertex(pose, x2, y2, z2).setColor(c2);
            consumer.addVertex(pose, x0, y0, z0).setColor(c0);
            consumer.addVertex(pose, x2, y2, z2).setColor(c2);
            consumer.addVertex(pose, x1, y1, z1).setColor(c1);
        }

        @Override
        public void lens(float y, float radius, int color, int segments) {
            if (ARGB.alpha(color) == 0) return;
            int clear = ARGB.color(0, color);
            for (int i = 0; i < segments; i++) {
                float a0 = i * Mth.TWO_PI / segments, a1 = (i + 1) * Mth.TWO_PI / segments;
                triangle(0.5F, y, 0.5F, color, 0.5F + Mth.cos(a0) * radius, y, 0.5F + Mth.sin(a0) * radius, clear,
                        0.5F + Mth.cos(a1) * radius, y, 0.5F + Mth.sin(a1) * radius, clear);
            }
        }
    }

    private static final World WORLD = new World();

    private static final class World implements Glow {
        private double x, y, z;
        private float scale = 1.0F;

        @Override
        public void quad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                         float dx, float dy, float dz, int a, int b, int c, int d) {
            corner(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, a, 1.0F, 0.0F, 1.0F, 0.0F);
            corner(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, b, 0.0F, 1.0F, 1.0F, 0.0F);
            corner(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, c, 0.0F, 1.0F, 0.0F, 1.0F);
            corner(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, d, 1.0F, 0.0F, 0.0F, 1.0F);
        }

        private void corner(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                            float dx, float dy, float dz, int color, float u0, float u1, float v0, float v1) {
            if (ARGB.alpha(color) == 0 || quadCount >= MAX_QUADS) return;
            if ((quadCount + 1) * QUAD > quads.length) {
                quads = Arrays.copyOf(quads, quads.length * 2);
                quadCorners = Arrays.copyOf(quadCorners, quadCorners.length * 2);
            }
            int c = quadCount * 12, at = quadCount++ * QUAD;
            put(c, ax, ay, az);
            put(c + 3, dx, dy, dz);
            put(c + 6, cx, cy, cz);
            put(c + 9, bx, by, bz);
            quads[at] = Float.intBitsToFloat(color);
            quads[at + 1] = texel(u0);
            quads[at + 2] = texel(u1);
            quads[at + 3] = texel(v0);
            quads[at + 4] = texel(v1);
        }

        private void put(int at, float px, float py, float pz) {
            quadCorners[at] = x + px * scale;
            quadCorners[at + 1] = y + py * scale;
            quadCorners[at + 2] = z + pz * scale;
        }

        private static float texel(float t) {
            return (0.5F + t * (FADE_SIZE - 1)) / FADE_SIZE;
        }

        @Override
        public void lens(float y, float radius, int color, int segments) {
            if (ARGB.alpha(color) == 0) return;
            if ((lensCount + 1) * 2 > lenses.length) {
                lenses = Arrays.copyOf(lenses, lenses.length * 2);
                lensCenters = Arrays.copyOf(lensCenters, lensCenters.length * 2);
            }
            int c = lensCount * 3, at = lensCount++ * 2;
            lensCenters[c] = x + 0.5 * scale;
            lensCenters[c + 1] = this.y + y * scale;
            lensCenters[c + 2] = z + 0.5 * scale;
            lenses[at] = radius * scale;
            lenses[at + 1] = Float.intBitsToFloat(color);
        }
    }

    private record ItemSheet(VertexConsumer consumer, PoseStack.Pose pose, float u, float v, float pieceSize) implements Glow {
        private static final int MAX_CUTS = 24;
        private static final float[] PIECE = new float[12];
        private static final int[] PIECE_COLORS = new int[4];
        private static final int[] WINDING = {0, 1, 2, 3, 0, 3, 2, 1};

        @Override
        public void quad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                         float dx, float dy, float dz, int a, int b, int c, int d) {
            int across = cuts(bx - ax, by - ay, bz - az, pieceSize), down = cuts(dx - ax, dy - ay, dz - az, pieceSize);
            for (int i = 0; i < across; i++) {
                float s0 = i / (float) across, s1 = (i + 1) / (float) across;
                for (int j = 0; j < down; j++) {
                    float t0 = j / (float) down, t1 = (j + 1) / (float) down;
                    piece(s0, t0, s1, t1, ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, a, b, c, d);
                }
            }
        }

        private static int cuts(float x, float y, float z, float size) {
            return Mth.clamp(Mth.ceil(Mth.sqrt(x * x + y * y + z * z) / size), 1, MAX_CUTS);
        }

        private void piece(float s0, float t0, float s1, float t1, float ax, float ay, float az, float bx, float by, float bz,
                           float cx, float cy, float cz, float dx, float dy, float dz, int a, int b, int c, int d) {
            float[] p = PIECE;
            int[] k = PIECE_COLORS;
            for (int n = 0; n < 4; n++) {
                float s = n == 1 || n == 2 ? s1 : s0, t = n >= 2 ? t1 : t0;
                p[n * 3] = Mth.lerp(t, Mth.lerp(s, ax, bx), Mth.lerp(s, dx, cx));
                p[n * 3 + 1] = Mth.lerp(t, Mth.lerp(s, ay, by), Mth.lerp(s, dy, cy));
                p[n * 3 + 2] = Mth.lerp(t, Mth.lerp(s, az, bz), Mth.lerp(s, dz, cz));
                k[n] = ARGB.srgbLerp(t, ARGB.srgbLerp(s, a, b), ARGB.srgbLerp(s, d, c));
            }
            if ((ARGB.alpha(k[0]) | ARGB.alpha(k[1]) | ARGB.alpha(k[2]) | ARGB.alpha(k[3])) == 0) return;
            for (int n : WINDING) vertex(p[n * 3], p[n * 3 + 1], p[n * 3 + 2], k[n]);
        }

        @Override
        public void lens(float y, float radius, int color, int segments) {
            int clear = ARGB.color(0, color);
            for (int i = 0; i < segments; i++) {
                float a0 = i * Mth.TWO_PI / segments, a1 = (i + 1) * Mth.TWO_PI / segments;
                float x0 = 0.5F + Mth.cos(a0) * radius, z0 = 0.5F + Mth.sin(a0) * radius;
                float x1 = 0.5F + Mth.cos(a1) * radius, z1 = 0.5F + Mth.sin(a1) * radius;
                quad(0.5F, y, 0.5F, x0, y, z0, x1, y, z1, x1, y, z1, color, clear, clear, clear);
            }
        }

        private void vertex(float x, float y, float z, int color) {
            int strong = ARGB.color(Math.min(255, ARGB.alpha(color) * 2), ARGB.srgbLerp(0.15F, bright(color), 0xFFFFFFFF));
            consumer.addVertex(pose, x, y, z).setColor(strong).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
        }

        private static int bright(int color) {
            int peak = Math.max(ARGB.red(color), Math.max(ARGB.green(color), ARGB.blue(color)));
            return peak == 0 ? 0xFF000000 : ARGB.opaque(ARGB.scaleRGB(color, 255.0F / peak));
        }
    }
}
