package net.ixdarklord.trophypedestals.client;

import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * Cones of light from a pedestal, shining up: its showcase light, and the Prism's fan of rays. Each lands as a round spot
 * on whatever is above it, projected the way the Prism's halo is (see {@link HaloLight}): every point of every surface
 * the cone reaches (see {@link LitFaces}) works out where it is across the cone, either way, as a share of the cone's
 * width there, and the shader draws the spot's round, soft edge from that (see {@code light_spot.fsh}), so it runs smoothly over each face and
 * round each edge, wider and fainter the farther it has come, and shadowed by anything in the way.
 */
final class SpotLights {
    /** How far the light reaches, in blocks, fading out smoothly from {@link #FADE_FROM} on. */
    private static final int RANGE = 12;
    private static final double FADE_FROM = 8.0;
    /** How far the light goes before it's half as bright. */
    private static final double FADE_DISTANCE = 8.0;
    /** How steeply up from the lens a surface must be for the cones to reach it, give or take its size. */
    private static final double LOWEST = 0.25;
    private static final LitFaces.Cache FACES = new LitFaces.Cache(RANGE, 4000, 10000L, 0.3, LOWEST).soft(0.12);

    // a face's working: each point's place across the cone, either way, and its light
    private static final float[] u = new float[25], v = new float[25];
    private static final int[] argb = new int[25];

    private SpotLights() {
    }

    static void beginFrame(ClientLevel level) {
        FACES.beginFrame(level);
    }

    /**
     * Draws a cone of light shining up from a pedestal's lens.
     *
     * @param block  the pedestal's block, whose cones share their surfaces
     * @param x      the lens, where the light starts
     * @param dx     which way it shines; needn't be a unit vector
     * @param foot   how wide, in blocks, the cone is where it starts
     * @param spread how much wider it gets for each block it goes
     * @param color  the light's colour, its alpha how strong it is
     * @param seen   how far the camera is: the farther, the more plainly it's drawn (see {@link LitFaces#coarse})
     */
    static void draw(VfxDraw draw, ClientLevel level, long block, double x, double y, double z, double dx, double dy, double dz, double foot,
                     double spread, int color, double seen, long now) {
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length < 1.0E-6 || ARGB.alpha(color) == 0) return;
        dx /= length;
        dy /= length;
        dz /= length;
        LitFaces.Faces faces = FACES.get(level, block, x, y, z, now);
        if (faces == null) return;
        // a cone as wide as its foot where it starts: from a point that far behind the lens
        double behind = foot / spread, ax = x - dx * behind, ay = y - dy * behind, az = z - dz * behind;
        // two ways across the cone, square to it and to each other
        double sideX, sideY, sideZ;
        if (Math.abs(dy) < 0.9) {
            sideX = -dz;
            sideY = 0.0;
            sideZ = dx;
        } else {
            sideX = 0.0;
            sideY = dz;
            sideZ = -dy;
        }
        double sideLength = Math.sqrt(sideX * sideX + sideY * sideY + sideZ * sideZ);
        sideX /= sideLength;
        sideY /= sideLength;
        sideZ /= sideLength;
        double upX = dy * sideZ - dz * sideY, upY = dz * sideX - dx * sideZ, upZ = dx * sideY - dy * sideX;
        float strength = ARGB.alpha(color) / 255.0F;
        int rgb = color & 0xFFFFFF;
        double[] p = faces.points(), middles = faces.middles();
        boolean coarse = LitFaces.coarse(seen);
        double smallest = LitFaces.smallest(seen);
        int faintest = (int) (LitFaces.faintest(seen) * 255.0F);
        for (int f = 0; f < faces.count(); f++) {
            // a face the cone can't reach: off its middle by more than the cone is wide there, give or take its size
            double mx = middles[f * 4] - ax, my = middles[f * 4 + 1] - ay, mz = middles[f * 4 + 2] - az;
            double along = mx * dx + my * dy + mz * dz;
            if (along <= 0.0) continue;
            double off = Math.sqrt(Math.max(0.0, mx * mx + my * my + mz * mz - along * along));
            if (off - middles[f * 4 + 3] > along * spread * 1.05 || middles[f * 4 + 3] < smallest || !LitFaces.inView(faces, f)) continue;
            int grid = faces.grids()[f], side = grid + 1, start = faces.starts()[f], points = side * side;
            float nx = faces.normals()[f * 3], ny = faces.normals()[f * 3 + 1], nz = faces.normals()[f * 3 + 2];
            // far off, only the surface's corners: where it is across the cone runs smoothly enough to blend between
            int[] corners = coarse && grid > 1 ? LitFaces.corners(grid) : null;
            boolean any = false;
            for (int j = 0, count = corners == null ? points : 4; j < count; j++) {
                int k = corners == null ? j : corners[j];
                int i = (start + k) * 3;
                double wx = p[i] - ax, wy = p[i + 1] - ay, wz = p[i + 2] - az;
                double on = Math.max(wx * dx + wy * dy + wz * dz, 1.0E-4);
                // how far from the cone's middle either way, as a share of how wide it is there
                u[k] = (float) ((wx * sideX + wy * sideY + wz * sideZ) / (on * spread));
                v[k] = (float) ((wx * upX + wy * upY + wz * upZ) / (on * spread));
                double lx = p[i] - x, ly = p[i + 1] - y, lz = p[i + 2] - z;
                double distance = Math.sqrt(lx * lx + ly * ly + lz * lz);
                // how square on the light falls on the face here
                double square = -(lx * nx + ly * ny + lz * nz) / Math.max(distance, 1.0E-4);
                double light = strength * faces.reach()[start + k] * Math.max(0.0, 0.3 + 0.7 * square)
                        / (1.0 + Mth.square(distance / FADE_DISTANCE)) * PointLights.fadeOut(distance, FADE_FROM, RANGE);
                int alpha = Mth.clamp((int) (light * 255.0), 0, 255);
                if (alpha > faintest) any = true;
                argb[k] = ARGB.color(alpha, rgb);
            }
            if (!any) continue;
            if (corners != null) {
                int k00 = corners[0], k01 = corners[1], k11 = corners[2], k10 = corners[3];
                if (beyond(u, k00, k01, k11, k10) || beyond(v, k00, k01, k11, k10)) continue;
                draw.quad(faces.point(start + k00), u[k00], v[k00], argb[k00], faces.point(start + k01), u[k01], v[k01], argb[k01],
                        faces.point(start + k11), u[k11], v[k11], argb[k11], faces.point(start + k10), u[k10], v[k10], argb[k10]);
                continue;
            }
            for (int a = 0; a < grid; a++) {
                for (int b = 0; b < grid; b++) {
                    int k00 = a * side + b, k01 = k00 + 1, k11 = k00 + side + 1, k10 = k00 + side;
                    // a square wholly to one side of the cone
                    if (beyond(u, k00, k01, k11, k10) || beyond(v, k00, k01, k11, k10)) continue;
                    if ((argb[k00] | argb[k01] | argb[k11] | argb[k10]) >>> 24 == 0) continue;
                    draw.quad(faces.point(start + k00), u[k00], v[k00], argb[k00], faces.point(start + k01), u[k01], v[k01], argb[k01],
                            faces.point(start + k11), u[k11], v[k11], argb[k11], faces.point(start + k10), u[k10], v[k10], argb[k10]);
                }
            }
        }
    }

    /** Whether all four corners lie past the cone's edge on the same side, one way across it. */
    private static boolean beyond(float[] at, int k00, int k01, int k11, int k10) {
        return (at[k00] > 1.0F && at[k01] > 1.0F && at[k11] > 1.0F && at[k10] > 1.0F)
                || (at[k00] < -1.0F && at[k01] < -1.0F && at[k11] < -1.0F && at[k10] < -1.0F);
    }
}
