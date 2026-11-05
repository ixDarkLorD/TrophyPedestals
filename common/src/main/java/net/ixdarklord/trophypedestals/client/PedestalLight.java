package net.ixdarklord.trophypedestals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxBlend;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldVfx;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

/**
 * Where a pedestal's light goes: its glows, outlines, beam, halos, crown shards and flares.
 * <p>
 * A placed pedestal's light is drawn as world effects ({@link WorldVfx}), after the rest of the level, the clouds
 * included: drawn with the block entities, it wrote no depth (so its faded parts don't cut holes in one another), and
 * the clouds, drawn after it, were painted straight over it. Its shapes are gathered here while the pedestals are
 * drawn and handed to the world effects at the end of the frame. An item's light (in a slot or a hand, where there are
 * no clouds) goes into the translucent item sheet, after the item's own model.
 */
public final class PedestalLight {
    // Its fades: a white texture whose alpha is the product of how far across and how far down it is (full in one
    // corner, nothing along the two edges away from it), tinted and added to the scene.
    private static final VfxMaterial FADE = new VfxMaterial(TrophyPedestalsMod.id("textures/misc/corner_fade.png"), VfxBlend.ADDITIVE, 0.0F);
    // Its size, for sampling it between its edge texels' middles (where its fade is exact).
    private static final int FADE_SIZE = 64;
    // The lens's glow: a round soft glow lying over the glass.
    private static final VfxMaterial LENS = new VfxMaterial(VfxMaterial.GLOW, VfxBlend.ADDITIVE, 0.0F);
    private static final int QUAD = 17;
    private static final int MAX_QUADS = 1 << 16;

    // This frame's shapes, in world coordinates: quads (four corners, a color, and the fade's span across and down), lens
    // glows and flares.
    private static float[] quads = new float[QUAD * 1024];
    private static int quadCount;
    private static float[] lenses = new float[5 * 64];
    private static int lensCount;
    private static float[] flares = new float[6 * 64];
    private static int flareCount;
    private static VfxMaterial flareMaterial;

    private PedestalLight() {
    }

    /** Draws the gathered light every frame. */
    public static void init(Identifier flareTexture) {
        flareMaterial = new VfxMaterial(flareTexture, VfxBlend.ALPHA, 0.0F);
        WorldVfx.register(TrophyPedestalsMod.id("pedestal_light"), PedestalLight::render);
    }

    private static void render(VfxFrame frame) {
        if (quadCount > 0) {
            VfxDraw draw = frame.draw(FADE);
            for (int i = 0; i < quadCount; i++) {
                int at = i * QUAD;
                draw.quad(new Vec3(quads[at], quads[at + 1], quads[at + 2]), new Vec3(quads[at + 3], quads[at + 4], quads[at + 5]),
                        new Vec3(quads[at + 6], quads[at + 7], quads[at + 8]), new Vec3(quads[at + 9], quads[at + 10], quads[at + 11]),
                        quads[at + 13], quads[at + 15], quads[at + 14], quads[at + 16], Float.floatToRawIntBits(quads[at + 12]));
            }
        }
        if (lensCount > 0) {
            VfxDraw draw = frame.draw(LENS);
            for (int i = 0; i < lensCount; i++) {
                int at = i * 5;
                draw.flat(lenses[at], lenses[at + 1], lenses[at + 2], lenses[at + 3], 0.0F, Float.floatToRawIntBits(lenses[at + 4]));
            }
        }
        if (flareCount > 0) {
            VfxDraw draw = frame.draw(flareMaterial);
            for (int i = 0; i < flareCount; i++) {
                int at = i * 6;
                draw.billboard(flares[at], flares[at + 1], flares[at + 2], flares[at + 3], flares[at + 4], Float.floatToRawIntBits(flares[at + 5]));
            }
        }
        quadCount = lensCount = flareCount = 0;
    }

    /** A flare facing the camera at this place in the world, of this half width, turned by this angle (radians). */
    static void flare(double x, double y, double z, float half, float roll, int argb) {
        if (ARGB.alpha(argb) == 0) return;
        if ((flareCount + 1) * 6 > flares.length) flares = Arrays.copyOf(flares, flares.length * 2);
        int at = flareCount++ * 6;
        flares[at] = (float) x;
        flares[at + 1] = (float) y;
        flares[at + 2] = (float) z;
        flares[at + 3] = half;
        flares[at + 4] = roll;
        flares[at + 5] = Float.intBitsToFloat(argb);
    }

    /** Where a pedestal's light goes. Positions are in the block's units, over its model. */
    interface Glow {
        /** A quad seen from both sides, each corner in its own color, for fades. */
        void quad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                  float dx, float dy, float dz, int a, int b, int c, int d);

        /** A soft round glow lying flat at this height over the well's middle, its color at the middle. */
        void lens(float y, float radius, int color, int segments);
    }

    /** A placed pedestal's light, at this block, for the world effects. */
    static Glow world(BlockPos pos) {
        return world(pos.getX(), pos.getY(), pos.getZ(), 1.0F);
    }

    /**
     * A pedestal's light for the world effects, its block's corner here and its units this many blocks (smaller for a
     * trophy shrunk onto a trophy display).
     */
    static Glow world(double x, double y, double z, float scale) {
        WORLD.x = (float) x;
        WORLD.y = (float) y;
        WORLD.z = (float) z;
        WORLD.scale = scale;
        return WORLD;
    }

    /**
     * An item's light in the translucent item sheet (a white sprite, full bright, its fade in the vertex colors). Item
     * models are batched into the item sheets, which are drawn after everything else, so light drawn any other way would
     * go under the pedestal's own model; the translucent item sheet is the last of them, after its body and icon. With
     * {@code fine}, its faces are cut finer (for the outlines in hand, which stand close inside the cone).
     */
    static Glow itemSheet(VertexConsumer consumer, PoseStack.Pose pose, TextureAtlasSprite white, boolean fine) {
        return new ItemSheet(consumer, pose, (white.getU0() + white.getU1()) / 2.0F, (white.getV0() + white.getV1()) / 2.0F,
                fine ? ItemSheet.FINE_PIECE : ItemSheet.PIECE_SIZE);
    }

    /**
     * A pedestal's light in a scene of its own (a SceneViewport, where there are no world effects), added to the scene
     * as the world effects add it: {@code RenderTypes.dragonRays()}, additive, depth-tested and writing no depth, so its
     * faded parts don't cut into one another or into what's behind them. Triangles, each wound both ways.
     */
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

        // A disc from the middle, fading to nothing at its edge.
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

    // The world effects color a quad all one, so a quad whose corners differ is drawn as up to four, one for each
    // corner that shows, all over the same place and added together: each in its corner's color, full at its corner and
    // fading to nothing along the two far edges (the fade texture, turned to face that corner). Their sum is the smooth
    // blend of the four corners, with no steps anywhere in it.
    private static final class World implements Glow {
        private float x, y, z, scale = 1.0F;

        @Override
        public void quad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                         float dx, float dy, float dz, int a, int b, int c, int d) {
            // Across runs a to b, down runs a to d: each corner's fade is full at its own across and down ends.
            corner(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, a, 1.0F, 0.0F, 1.0F, 0.0F);
            corner(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, b, 0.0F, 1.0F, 1.0F, 0.0F);
            corner(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, c, 0.0F, 1.0F, 0.0F, 1.0F);
            corner(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, d, 1.0F, 0.0F, 0.0F, 1.0F);
        }

        // One corner's share, the fade running from u0 to u1 across (a to b) and v0 to v1 down (a to d).
        private void corner(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                            float dx, float dy, float dz, int color, float u0, float u1, float v0, float v1) {
            if (ARGB.alpha(color) == 0 || quadCount >= MAX_QUADS) return;
            if ((quadCount + 1) * QUAD > quads.length) quads = Arrays.copyOf(quads, quads.length * 2);
            int at = quadCount++ * QUAD;
            // In the order the world effects take a quad's texture: (u0, v0), (u0, v1), (u1, v1), (u1, v0).
            put(at, ax, ay, az);
            put(at + 3, dx, dy, dz);
            put(at + 6, cx, cy, cz);
            put(at + 9, bx, by, bz);
            quads[at + 12] = Float.intBitsToFloat(color);
            quads[at + 13] = texel(u0);
            quads[at + 14] = texel(u1);
            quads[at + 15] = texel(v0);
            quads[at + 16] = texel(v1);
        }

        private void put(int at, float px, float py, float pz) {
            quads[at] = x + px * scale;
            quads[at + 1] = y + py * scale;
            quads[at + 2] = z + pz * scale;
        }

        // From the fade's 0 to 1 to the texture's, between its edge texels' middles.
        private static float texel(float t) {
            return (0.5F + t * (FADE_SIZE - 1)) / FADE_SIZE;
        }

        @Override
        public void lens(float y, float radius, int color, int segments) {
            if (ARGB.alpha(color) == 0) return;
            if ((lensCount + 1) * 5 > lenses.length) lenses = Arrays.copyOf(lenses, lenses.length * 2);
            int at = lensCount++ * 5;
            lenses[at] = x + 0.5F * scale;
            lenses[at + 1] = this.y + y * scale;
            lenses[at + 2] = z + 0.5F * scale;
            lenses[at + 3] = radius * scale;
            lenses[at + 4] = Float.intBitsToFloat(color);
        }
    }

    private record ItemSheet(VertexConsumer consumer, PoseStack.Pose pose, float u, float v, float pieceSize) implements Glow {
        // How many pieces a quad is cut into each way. The sheet sorts its faces back to front before drawing, by each
        // face's middle, and writes depth: a broad face (the cone's) and a thin one (an outline's) sorted by their
        // middles can come out in the wrong order, so a faded outline in front hides the light behind it. Small pieces
        // sort true: each is cut into pieces no longer than this (in blocks) each way (finer where faces stand close to
        // one another), at most this many each way.
        private static final float PIECE_SIZE = 0.06F, FINE_PIECE = 0.03F;
        private static final int MAX_CUTS = 24;
        // Scratch for one piece (drawn on the render thread only): its corners and colors, and the order its corners go
        // in, once each way round.
        private static final float[] PIECE = new float[12];
        private static final int[] PIECE_COLORS = new int[4];
        private static final int[] WINDING = {0, 1, 2, 3, 0, 3, 2, 1};

        // As small quads, each wound both ways so it's seen from either side, colors blended across.
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

        // The piece between (s0, t0) and (s1, t1), s running a to b and t running a to d.
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

        // A disc from the middle, fading to nothing at its edge: each slice a quad with its outer corner doubled.
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

        // Blended rather than added as in the world, so it's drawn stronger, its color brought up to full brightness
        // (its brightest channel at full, its hue kept, as light added over the scene looks, and a touch whiter where it
        // adds up) to match a placed pedestal's light.
        private void vertex(float x, float y, float z, int color) {
            int strong = ARGB.color(Math.min(255, ARGB.alpha(color) * 2), ARGB.srgbLerp(0.15F, bright(color), 0xFFFFFFFF));
            consumer.addVertex(pose, x, y, z).setColor(strong).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(pose, 0.0F, 1.0F, 0.0F);
        }

        // The color scaled so its brightest channel is full (opaque; black stays black).
        private static int bright(int color) {
            int peak = Math.max(ARGB.red(color), Math.max(ARGB.green(color), ARGB.blue(color)));
            return peak == 0 ? 0xFF000000 : ARGB.opaque(ARGB.scaleRGB(color, 255.0F / peak));
        }
    }
}
