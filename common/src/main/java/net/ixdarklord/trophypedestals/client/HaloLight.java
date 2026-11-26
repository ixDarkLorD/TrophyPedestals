package net.ixdarklord.trophypedestals.client;

import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.trophypedestals.client.lens.PrismLensVisuals;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * The Prism's halo projected onto the world around it, as light from the halo's heart through its ring of stained glass
 * would fall: a band of the panes' colours along every surface the halo's plane meets (see {@link LitFaces}), running
 * smoothly over each face and round each edge, turning with the halo, and shadowed by anything in its way.
 * <p>
 * Each point of each face works out where it is as seen from the halo's heart, which pane's light reaches it and how far
 * from the band's middle, which the shader blends between the points (see {@code light_halo.fsh}). How strong the light
 * is there, for how far it has come, how square on it falls and whether it gets there at all, travels in each point's
 * colour, so brightness and shadows change smoothly too.
 */
final class HaloLight {
    /** How far the light reaches, in blocks; it fades out smoothly from {@link #FADE_FROM} on, so it never ends in an edge. */
    private static final int RANGE = 10;
    private static final double FADE_FROM = 6.0;
    /** How thick the band is, as an angle off the halo's plane, either side, in radians. */
    private static final double THICKNESS = 0.17;
    /** How far the light goes before it's half as bright. */
    private static final double FADE_DISTANCE = 6.0;
    private static final float PANE = Mth.TWO_PI / PrismLensVisuals.HALO_SHARDS;
    private static final LitFaces.Cache FACES = new LitFaces.Cache(RANGE, 6000, 10000L, 0.4).soft(0.15);

    // a face's working: each point's place in the band, and its light
    private static float[] u = new float[25], v = new float[25];
    private static int[] argb = new int[25];

    private HaloLight() {
    }

    static void beginFrame(ClientLevel level) {
        FACES.beginFrame(level);
    }

    /**
     * Draws a Prism's halo's light.
     *
     * @param x        the halo's heart
     * @param faceX    the way across the halo's plane
     * @param roll     how far round the halo has turned
     * @param glint    the pane glinting as a streak passes through it, or -1
     * @param shine    how brightly it glints, 0 to 1
     * @param strength how strong the light is, 0 to 1 and more
     * @param seen     how far the camera is: the farther, the more plainly it's drawn (see {@link LitFaces#coarse})
     */
    static void draw(VfxDraw draw, ClientLevel level, long block, double x, double y, double z, float faceX, float faceZ, float roll,
                     int glint, float shine, float strength, double seen, long now) {
        // its surfaces are found from above the pedestal's middle: the halo leans toward the camera, and they'd otherwise
        // be found again whenever the camera moves round it
        LitFaces.Faces faces = FACES.get(level, block, BlockPos.getX(block) + 0.5, y, BlockPos.getZ(block) + 0.5, now);
        if (faces == null || strength <= 0.0F) return;
        // the way the halo's plane faces
        double normalX = -faceZ, normalZ = faceX;
        int red = glint < 0 ? 0 : Math.round(glint / 11.0F * 255.0F), green = glint < 0 ? 0 : Math.round(shine * 255.0F);
        double[] p = faces.points(), middles = faces.middles();
        boolean coarse = LitFaces.coarse(seen);
        double smallest = LitFaces.smallest(seen);
        int faintest = (int) (LitFaces.faintest(seen) * 255.0F);
        for (int f = 0; f < faces.count(); f++) {
            // a face the band can't reach: it's off the band by more than its own size
            double mx = middles[f * 4] - x, my = middles[f * 4 + 1] - y, mz = middles[f * 4 + 2] - z;
            double middleDistance = Math.sqrt(mx * mx + my * my + mz * mz);
            double off = Math.abs(Math.asin(Mth.clamp((mx * normalX + mz * normalZ) / Math.max(middleDistance, 1.0E-4), -1.0, 1.0)));
            if (off - middles[f * 4 + 3] / Math.max(middleDistance, 0.5) > THICKNESS || middles[f * 4 + 3] < smallest
                    || !LitFaces.inView(faces, f)) continue;
            int grid = faces.grids()[f], side = grid + 1, start = faces.starts()[f], count = side * side;
            float nx = faces.normals()[f * 3], ny = faces.normals()[f * 3 + 1], nz = faces.normals()[f * 3 + 2];
            // far off, only the surface's corners: the band's place across them runs smoothly enough to blend between
            int[] corners = coarse && grid > 1 ? LitFaces.corners(grid) : null;
            boolean any = false;
            for (int j = 0, points = corners == null ? count : 4; j < points; j++) {
                int k = corners == null ? j : corners[j];
                int i = (start + k) * 3;
                double wx = p[i] - x, wy = p[i + 1] - y, wz = p[i + 2] - z;
                double across = wx * faceX + wz * faceZ, away = wx * normalX + wz * normalZ;
                double distance = Math.sqrt(wx * wx + wy * wy + wz * wz);
                double panes = (roll - Mth.atan2(wy, across)) / PANE;
                // keep a face's points on the same turn of the halo as its first
                if (k > 0) panes += Math.rint((u[0] - panes) / PrismLensVisuals.HALO_SHARDS) * PrismLensVisuals.HALO_SHARDS;
                u[k] = (float) panes;
                v[k] = (float) (Mth.atan2(away, Math.sqrt(across * across + wy * wy)) / THICKNESS);
                // how square on the light falls on the face here
                double square = -(wx * nx + wy * ny + wz * nz) / Math.max(distance, 1.0E-4);
                float light = (float) (strength * faces.reach()[start + k] * Math.max(0.0, 0.25 + 0.75 * square)
                        / (1.0 + Mth.square(distance / FADE_DISTANCE)) * PointLights.fadeOut(distance, FADE_FROM, RANGE));
                int alpha = Mth.clamp((int) (light * 255.0F), 0, 255);
                if (alpha > faintest) any = true;
                argb[k] = ARGB.color(alpha, red, green, 0);
            }
            if (!any) continue;
            if (corners != null) {
                int k00 = corners[0], k01 = corners[1], k11 = corners[2], k10 = corners[3];
                if ((v[k00] > 1.0F && v[k01] > 1.0F && v[k11] > 1.0F && v[k10] > 1.0F)
                        || (v[k00] < -1.0F && v[k01] < -1.0F && v[k11] < -1.0F && v[k10] < -1.0F)) continue;
                draw.quad(faces.point(start + k00), u[k00], v[k00], argb[k00], faces.point(start + k01), u[k01], v[k01], argb[k01],
                        faces.point(start + k11), u[k11], v[k11], argb[k11], faces.point(start + k10), u[k10], v[k10], argb[k10]);
                continue;
            }
            for (int a = 0; a < grid; a++) {
                for (int b = 0; b < grid; b++) {
                    int k00 = a * side + b, k01 = k00 + 1, k11 = k00 + side + 1, k10 = k00 + side;
                    // a square wholly off the band on one side, or unlit
                    if ((v[k00] > 1.0F && v[k01] > 1.0F && v[k11] > 1.0F && v[k10] > 1.0F)
                            || (v[k00] < -1.0F && v[k01] < -1.0F && v[k11] < -1.0F && v[k10] < -1.0F)) continue;
                    if ((argb[k00] | argb[k01] | argb[k11] | argb[k10]) >>> 24 == 0) continue;
                    draw.quad(faces.point(start + k00), u[k00], v[k00], argb[k00], faces.point(start + k01), u[k01], v[k01], argb[k01],
                            faces.point(start + k11), u[k11], v[k11], argb[k11], faces.point(start + k10), u[k10], v[k10], argb[k10]);
                }
            }
        }
    }
}
