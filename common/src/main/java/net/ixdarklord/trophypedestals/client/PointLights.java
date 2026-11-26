package net.ixdarklord.trophypedestals.client;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.trophypedestals.compat.ARGB;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

/**
 * The light a pedestal's effects give off, where they give it: each glowing thing a lens draws, its flames, its moon, its
 * rift, says so as it's drawn ({@link PedestalLight.Glow#light}), and its light falls on the surfaces around it (see
 * {@link LitFaces}). Each point of each surface adds up every light of that pedestal that reaches it, for how near and how
 * square on it is, so the colours mix where lights overlap, and the light blends smoothly across each face and over each
 * edge, fading to nothing at each light's reach rather than ending in a rim. A pedestal's lights share one set of
 * surfaces, with their shadows cast from just above its lens, so they hold still however its lights beat or move.
 */
final class PointLights {
    /** The most lights a frame, and how far a pedestal's lights may reach, in blocks. */
    private static final int MOST = 512, RANGE = 6;
    /** How strong a light of strength 1 is right by it (its alpha), and how much farther than asked it reaches. */
    private static final float STRENGTH = 0.62F, REACHES = 1.25F;
    /** How high above its block a pedestal's lights are found from: just over its cap. */
    private static final double ANCHOR_HEIGHT = 1.0;
    /** The most pedestals whose shading is kept; past that, the one farthest from the newest is forgotten. */
    private static final int MOST_KEPT = 256;
    private static final Long2ObjectOpenHashMap<Shading> SHADING = new Long2ObjectOpenHashMap<>();
    private static final LitFaces.Cache FACES = new LitFaces.Cache(RANGE, 2500, 10000L, 0.6).soft(0.35).finest(2);

    private static final long[] owner = new long[MOST];
    private static final double[] lightX = new double[MOST], lightY = new double[MOST], lightZ = new double[MOST];
    private static final float[] red = new float[MOST], green = new float[MOST], blue = new float[MOST], power = new float[MOST],
            reach = new float[MOST];
    private static final double[] away = new double[MOST];
    private static final int[] order = new int[MOST];
    private static final int[] group = new int[MOST];
    private static int count;
    // a face's working: each point's light
    private static final int[] argb = new int[25];

    private PointLights() {
    }

    /** A light a pedestal's effects give off this frame: at (x, y, z) in the world, of its colour and strength, reaching {@code radius} blocks. */
    static void emit(long pedestal, double x, double y, double z, int color, float strength, float radius) {
        if (count >= MOST || strength <= 0.0F || radius <= 0.0F) return;
        int full = PedestalLighting.fullBright(color);
        float even = PedestalLighting.balance(full) * strength;
        owner[count] = pedestal;
        lightX[count] = x;
        lightY[count] = y;
        lightZ[count] = z;
        red[count] = ARGB.red(full) / 255.0F * even;
        green[count] = ARGB.green(full) / 255.0F * even;
        blue[count] = ARGB.blue(full) / 255.0F * even;
        power[count] = even;
        reach[count] = Math.min(radius * REACHES, RANGE);
        count++;
    }

    /**
     * Draws the frame's lights, and forgets them: each pedestal's together, the nearest pedestal's first, so the surfaces
     * of those nearest the camera are found first, and theirs light up first when there's a lot to find.
     */
    static void render(VfxDraw draw, ClientLevel level, Vec3 camera, long now) {
        FACES.beginFrame(level);
        for (int i = 0; i < count; i++) {
            long key = owner[i];
            away[i] = camera.distanceToSqr(BlockPos.getX(key) + 0.5, BlockPos.getY(key) + 0.5, BlockPos.getZ(key) + 0.5);
            order[i] = i;
        }
        // nearest first, and a pedestal's lights next to each other
        IntArrays.quickSort(order, 0, count, (a, b) -> {
            int nearer = Double.compare(away[a], away[b]);
            return nearer != 0 ? nearer : Long.compare(owner[a], owner[b]);
        });
        for (int i = 0; i < count; ) {
            long key = owner[order[i]];
            int size = 0;
            while (i < count && owner[order[i]] == key) group[size++] = order[i++];
            drawGroup(draw, level, camera, size, now);
        }
        count = 0;
    }

    private static void drawGroup(VfxDraw draw, ClientLevel level, Vec3 camera, int size, long now) {
        // where the pedestal's light is found from: just above its lens, wherever its lights are and however brightly each
        // glows now, so a light that beats (the Blood Moon's) changes how bright it is, never where its shadows fall
        long key = owner[group[0]];
        double x = BlockPos.getX(key) + 0.5, y = BlockPos.getY(key) + ANCHOR_HEIGHT, z = BlockPos.getZ(key) + 0.5, farthest = 0;
        double seen = Math.sqrt(camera.distanceToSqr(x, y, z)), far = LitFaces.sight(seen);
        boolean coarse = LitFaces.coarse(seen);
        double smallest = LitFaces.smallest(seen);
        if (far <= 0.0) return;
        for (int g = 0; g < size; g++) {
            int i = group[g];
            farthest = Math.max(farthest, reach[i] + Math.sqrt(Mth.square(lightX[i] - x) + Mth.square(lightY[i] - y) + Mth.square(lightZ[i] - z)));
        }
        LitFaces.Faces faces = FACES.get(level, key, x, y, z, now);
        if (faces == null) return;
        Shading shading = SHADING.get(key);
        if (shading == null) {
            if (SHADING.size() >= MOST_KEPT) forgetFarthest(key);
            shading = new Shading();
            SHADING.put(key, shading);
        }
        if (shading.outdated(faces, size)) shading.work(faces, size, x, y, z, farthest);
        float scale = (float) (STRENGTH * far);
        float[] amounts = shading.amounts;
        // far off, a surface too small or too faintly lit to show isn't drawn
        float strongest = 0.0F;
        for (int s = 0; s < size; s++) strongest = Math.max(strongest, power[group[s]]);
        float faintest = LitFaces.faintest(seen) / Math.max(strongest * scale, 1.0E-4F);
        double[] middles = faces.middles();
        int at = 0;
        for (int n = 0; n < shading.count; n++) {
            int f = shading.faces[n];
            int grid = faces.grids()[f], side = grid + 1, start = faces.starts()[f], points = side * side, from = at;
            at += points * size;
            if (shading.most[n] < faintest || middles[f * 4 + 3] < smallest) continue;
            if (!LitFaces.inView(faces, f)) continue;
            if (coarse && grid > 1) {
                // far off: the whole surface as one, from its corners
                int k01 = grid, k11 = points - 1, k10 = grid * side;
                int c00 = shade(amounts, from, size, scale), c01 = shade(amounts, from + k01 * size, size, scale),
                        c11 = shade(amounts, from + k11 * size, size, scale), c10 = shade(amounts, from + k10 * size, size, scale);
                if ((c00 | c01 | c11 | c10) >>> 24 == 0) continue;
                draw.quad(faces.point(start), 0.0F, 0.0F, c00, faces.point(start + k01), 0.0F, 1.0F, c01,
                        faces.point(start + k11), 1.0F, 1.0F, c11, faces.point(start + k10), 1.0F, 0.0F, c10);
                continue;
            }
            boolean any = false;
            for (int k = 0; k < points; k++) {
                argb[k] = shade(amounts, from + k * size, size, scale);
                if (argb[k] >>> 24 != 0) any = true;
            }
            if (!any) continue;
            for (int a = 0; a < grid; a++) {
                for (int c = 0; c < grid; c++) {
                    int k00 = a * side + c, k01 = k00 + 1, k11 = k00 + side + 1, k10 = k00 + side;
                    if ((argb[k00] | argb[k01] | argb[k11] | argb[k10]) >>> 24 == 0) continue;
                    draw.quad(faces.point(start + k00), 0.0F, 0.0F, argb[k00], faces.point(start + k01), 0.0F, 1.0F, argb[k01],
                            faces.point(start + k11), 1.0F, 1.0F, argb[k11], faces.point(start + k10), 1.0F, 0.0F, argb[k10]);
                }
            }
        }
    }

    /** This frame's light at a point: the colour and strength of each of the group's lights, by how much of it falls there. */
    private static int shade(float[] amounts, int from, int size, float scale) {
        float r = 0, g = 0, b = 0;
        for (int s = 0; s < size; s++) {
            float amount = amounts[from + s];
            if (amount <= 0.0F) continue;
            int i = group[s];
            r += red[i] * amount;
            g += green[i] * amount;
            b += blue[i] * amount;
        }
        return color(r * scale, g * scale, b * scale);
    }

    /**
     * How much of each of a pedestal's lights falls on each point of its surfaces, for how near and how square on it is
     * and whether it gets there: the costly part, worked out only when the surfaces or where the lights are change, so a
     * frame only has to colour it with how bright each light is now (a beat, a flicker costs nothing).
     */
    private static final class Shading {
        /** How far a light may move before its shading is worked out again: a bob doesn't, a light that moves does. */
        private static final double MOVES = 0.08;
        private LitFaces.Faces from;
        private int version, lights, count;
        private int[] faces = new int[64];
        private float[] amounts = new float[256];
        /** The most light, of any one light, on each surface. */
        private float[] most = new float[64];
        private double[] places = new double[16];

        boolean outdated(LitFaces.Faces faces, int size) {
            if (faces != this.from || faces.version() != this.version || size != this.lights) return true;
            for (int s = 0; s < size; s++) {
                int i = group[s];
                if (Math.abs(this.places[s * 4] - lightX[i]) > MOVES || Math.abs(this.places[s * 4 + 1] - lightY[i]) > MOVES
                        || Math.abs(this.places[s * 4 + 2] - lightZ[i]) > MOVES || this.places[s * 4 + 3] != reach[i]) return true;
            }
            return false;
        }

        void work(LitFaces.Faces faces, int size, double x, double y, double z, double farthest) {
            this.from = faces;
            this.version = faces.version();
            this.lights = size;
            if (this.places.length < size * 4) this.places = new double[size * 4];
            for (int s = 0; s < size; s++) {
                int i = group[s];
                this.places[s * 4] = lightX[i];
                this.places[s * 4 + 1] = lightY[i];
                this.places[s * 4 + 2] = lightZ[i];
                this.places[s * 4 + 3] = reach[i];
            }
            double[] p = faces.points(), middles = faces.middles();
            float[] lit = faces.reach();
            this.count = 0;
            int at = 0;
            for (int f = 0; f < faces.count(); f++) {
                // a face none of the lights reach
                if (Math.sqrt(Mth.square(middles[f * 4] - x) + Mth.square(middles[f * 4 + 1] - y) + Mth.square(middles[f * 4 + 2] - z))
                        - middles[f * 4 + 3] > farthest) continue;
                int grid = faces.grids()[f], start = faces.starts()[f], points = (grid + 1) * (grid + 1);
                if (this.count == this.faces.length) {
                    this.faces = Arrays.copyOf(this.faces, this.count * 2);
                    this.most = Arrays.copyOf(this.most, this.count * 2);
                }
                while (at + points * size > this.amounts.length) this.amounts = Arrays.copyOf(this.amounts, this.amounts.length * 2);
                float nx = faces.normals()[f * 3], ny = faces.normals()[f * 3 + 1], nz = faces.normals()[f * 3 + 2];
                boolean any = false;
                float most = 0.0F;
                for (int k = 0; k < points; k++) {
                    int c = (start + k) * 3;
                    for (int s = 0; s < size; s++) {
                        float amount = 0.0F;
                        if (lit[start + k] > 0.0F) {
                            double wx = this.places[s * 4] - p[c], wy = this.places[s * 4 + 1] - p[c + 1], wz = this.places[s * 4 + 2] - p[c + 2];
                            double distance = Math.sqrt(wx * wx + wy * wy + wz * wz), radius = this.places[s * 4 + 3];
                            double square = (wx * nx + wy * ny + wz * nz) / Math.max(distance, 1.0E-4);
                            if (distance < radius && square > 0.0) {
                                // how near it is, falling to nothing at its reach with no rim, and how square on it falls
                                double near = 1.0 - Mth.square(distance / radius);
                                amount = (float) (near * Math.sqrt(near) * (0.2 + 0.8 * square) * lit[start + k]);
                            }
                        }
                        this.amounts[at + k * size + s] = amount;
                        if (amount > 0.0F) any = true;
                        most = Math.max(most, amount);
                    }
                }
                if (!any) continue;
                this.most[this.count] = most;
                this.faces[this.count++] = f;
                at += points * size;
            }
        }
    }

    /** Forgets the shading of the pedestal farthest from this one. */
    private static void forgetFarthest(long key) {
        long farthest = key;
        long most = -1L;
        for (long other : SHADING.keySet()) {
            long distance = BlockPos.of(other).distManhattan(BlockPos.of(key));
            if (distance > most) {
                most = distance;
                farthest = other;
            }
        }
        SHADING.remove(farthest);
    }

    /** Light of these amounts of red, green and blue: its colour at full brightness, and how much of it there is in its alpha. */
    private static int color(double r, double g, double b) {
        double most = Math.max(r, Math.max(g, b));
        if (most <= 1.0E-3) return 0;
        int alpha = (int) Math.min(255.0, most * 255.0);
        return ARGB.color(alpha, (int) (r / most * 255.0), (int) (g / most * 255.0), (int) (b / most * 255.0));
    }

    /** 1 nearer than {@code from}, falling smoothly to 0 by {@code to}. */
    static double fadeOut(double distance, double from, double to) {
        double t = Mth.clamp((to - distance) / (to - from), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }
}
