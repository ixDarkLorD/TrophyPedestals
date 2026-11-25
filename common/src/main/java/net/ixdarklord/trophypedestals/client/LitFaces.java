package net.ixdarklord.trophypedestals.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlock;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The surfaces a light shines on: every surface around a point that faces it and is open to the air, each laid as a
 * grid, finer the nearer it is, with how much of the light reaches each point of the grid. Full blocks give their faces;
 * anything else gives its model's own faces, a lever's handle and a stair's steps as they're drawn, or, if it's drawn
 * without a model (a chest), its shape. Parts of a model that are mostly see-through (a flower's leaves, a glass pane)
 * aren't lit, as glass isn't, and neither is any part of a surface something rests on. Light doesn't reach a point with
 * anything with a shape in the way, glass aside. The lights that shine on the surfaces work out their light at each
 * point and let it blend smoothly across the grid.
 * <p>
 * Finding them takes a while, so a {@link Cache} keeps each light's surfaces and finds them again now and then.
 */
final class LitFaces {
    /** Lifted off the surface, toward the light, so what's drawn on it doesn't flicker into it. */
    private static final double LIFT = 0.004;
    /** Where across a light its shadows are cast from, as a share of its size: its middle, and four round it. */
    private static final double[] SAMPLES = {0, 0, 1, 0, -1, 0, 0, 1, 0, -1};
    /** How much of a surface something resting on it must cover for none of it to be lit. */
    private static final float MOSTLY_COVERED = 0.6F;
    /** How much of a see-through part of a model must be solid to be lit. */
    private static final float MOSTLY_SOLID = 0.85F;
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final AABB CUBE = new AABB(0, 0, 0, 1, 1, 1);

    /**
     * The surfaces around a light at ({@code x}, {@code y}, {@code z}). Surface {@code f} is a grid {@code grids[f]} squares
     * a side (0 for one no longer there), facing ({@code normals[f * 3]}, ...); its points start at {@code starts[f]} in
     * {@code points} (x, y, z each), {@code vectors} and {@code reach} (1 where the light gets there, 0 where it doesn't,
     * between for the edge of a shadow), laid out a row along its first edge at a time; {@code middles} holds its middle
     * and how far its corners are from it (x, y, z, half each), and {@code owners} the block it belongs to. They're
     * changed in place when a block among them changes, and {@link #version()} says each time they are.
     */
    static final class Faces {
        private final double x, y, z;
        private final long foundAt;
        private int count, used, version;
        /** Whether some of the blocks round the light weren't loaded when they were found, so they're found again soon. */
        private boolean incomplete;
        private byte[] grids = new byte[128];
        private float[] normals = new float[128 * 3];
        private int[] starts = new int[128];
        private double[] middles = new double[128 * 4];
        private long[] owners = new long[128];
        private double[] points = new double[2048 * 3];
        private Vec3[] vectors = new Vec3[2048];
        private float[] reach = new float[2048];

        private Faces(double x, double y, double z, long foundAt) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.foundAt = foundAt;
        }

        int count() {
            return this.count;
        }

        byte[] grids() {
            return this.grids;
        }

        float[] normals() {
            return this.normals;
        }

        int[] starts() {
            return this.starts;
        }

        double[] points() {
            return this.points;
        }

        float[] reach() {
            return this.reach;
        }

        double[] middles() {
            return this.middles;
        }

        /** Goes up each time the surfaces change in place, so what's worked out from them knows to work it out again. */
        int version() {
            return this.version;
        }

        /** A point, made once when the surface was found, so drawing it makes nothing new each frame. */
        Vec3 point(int index) {
            return this.vectors[index];
        }
    }

    /** A face of a block's model: its corners, inside the block, in the order they're drawn; which way it faces; and the side it's against, if any. */
    private record ModelFace(float[] corners, float nx, float ny, float nz, int against) {
    }

    private static final Map<BlockState, ModelFace[]> MODELS = new IdentityHashMap<>();
    private static final Map<Long, Boolean> SOLID_ENOUGH = new HashMap<>();
    private static Object modelsFrom;

    private LitFaces() {
    }

    private static final List<Cache> CACHES = new ArrayList<>();

    /** A block changed: every light that may shine on it finds its surfaces again, as soon as it can. */
    static void blockChanged(BlockPos pos) {
        for (Cache cache : CACHES) cache.changed(pos);
    }

    /**
     * How long, in nanoseconds, finding surfaces may take each frame, for each kind of light: a search that needs longer
     * goes on where it left off next frame, so finding them never makes a frame stutter, and a light keeps what it had
     * until its new surfaces are ready. Each kind has its own share, so one kind's searches never hold up another's.
     */
    private static final long CACHE_BUDGET = 700_000L;
    /**
     * How long surfaces found with every block round them loaded are kept: a block changing among them updates them at
     * once (see {@link #blockChanged}), so they're only found again now and then, in case of a change that wasn't seen.
     */
    private static final long KEEP_MILLIS = 300_000L;
    /** The most lights each kind keeps the surfaces of; past that, the one farthest from the newest is forgotten. */
    private static final int MOST_KEPT = 256;
    private static int frame;
    /** What's in view this frame: a cone from the camera, as wide as the corners of the screen and a little more. */
    private static double viewX, viewY, viewZ, forwardX, forwardY, forwardZ, viewSin, viewCos;

    /** At the start of a frame's lighting: this frame's time for finding surfaces begins, and what's in view is noted. */
    static void beginFrame() {
        frame++;
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 at = camera.position();
        Vector3fc forward = camera.forwardVector();
        viewX = at.x;
        viewY = at.y;
        viewZ = at.z;
        forwardX = forward.x();
        forwardY = forward.y();
        forwardZ = forward.z();
        double aspect = (double) minecraft.getWindow().getWidth() / Math.max(1, minecraft.getWindow().getHeight());
        double half = Math.atan(Math.tan(Math.toRadians(camera.getFov()) / 2.0) * Math.sqrt(1.0 + aspect * aspect)) + 0.15;
        half = Math.min(half, Math.PI / 2.0);
        viewSin = Math.sin(half);
        viewCos = Math.cos(half);
        fadeTo = Math.min(minecraft.options.getEffectiveRenderDistance() * 16.0, FARTHEST);
    }

    /**
     * How far from the camera, in blocks, a light is drawn in full detail, then plainly, then barely: the farther off it
     * is, the less of it can be seen, so it's drawn more simply, a whole surface at a time and only on the surfaces big
     * and bright enough to show.
     */
    private static final double DETAILED = 8.0, PLAIN = 24.0, DISTANT = 48.0;
    /** The farthest pedestals light the world, however far the world is drawn. */
    private static final double FARTHEST = 256.0;
    private static final int[] CORNERS = new int[4];
    private static double fadeTo = 80.0;

    /** Whether a light this far from the camera is drawn a whole surface at a time, from its corners. */
    static boolean coarse(double seen) {
        return seen > DETAILED;
    }

    /** How big a surface (half across, in blocks) must be for a light this far from the camera to be drawn on it. */
    static double smallest(double seen) {
        return seen <= DETAILED ? 0.0 : seen <= PLAIN ? 0.12 : seen <= DISTANT ? 0.3 : 0.45;
    }

    /** How bright (its alpha, 0 to 1) a light this far from the camera must be on a surface to be drawn there. */
    static float faintest(double seen) {
        return seen <= DETAILED ? 0.0F : seen <= PLAIN ? 0.03F : seen <= DISTANT ? 0.06F : 0.1F;
    }

    /**
     * How much of a light this far from the camera is drawn, 1 to 0: all of it until near the end of what the world is
     * drawn out to (the render distance), then fading out by its end, where the world itself ends.
     */
    static double sight(double seen) {
        double from = fadeTo * 0.8;
        if (seen <= from) return 1.0;
        double t = Mth.clamp((fadeTo - seen) / (fadeTo - from), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    /** A surface's four corners among its points, for a grid {@code grid} squares a side, in the order they're drawn. */
    static int[] corners(int grid) {
        CORNERS[0] = 0;
        CORNERS[1] = grid;
        CORNERS[2] = (grid + 1) * (grid + 1) - 1;
        CORNERS[3] = grid * (grid + 1);
        return CORNERS;
    }

    /** Whether any of surface {@code f} may be in view this frame: light falling where it can't be seen isn't drawn. */
    static boolean inView(Faces faces, int f) {
        double[] m = faces.middles;
        double dx = m[f * 4] - viewX, dy = m[f * 4 + 1] - viewY, dz = m[f * 4 + 2] - viewZ, radius = m[f * 4 + 3] * 1.75;
        double along = dx * forwardX + dy * forwardY + dz * forwardZ;
        if (along < -radius) return false;
        double across = Math.sqrt(Math.max(0.0, dx * dx + dy * dy + dz * dz - along * along));
        // how far outside the cone's side it is
        return across * viewCos - along * viewSin <= radius;
    }

    /**
     * Each light's surfaces, kept and found again when they're old, when the light has moved, or when a block they may
     * include changes, the search going on a little each frame. Until a light's are found again, it keeps what it had, so
     * it never goes dark.
     */
    static final class Cache {
        private final Long2ObjectOpenHashMap<Faces> faces = new Long2ObjectOpenHashMap<>();
        private final LongOpenHashSet stale = new LongOpenHashSet();
        private final Long2ObjectOpenHashMap<Finder> searching = new Long2ObjectOpenHashMap<>();
        private final int range, mostFaces;
        private final long refreshMillis;
        private final double moved, lowest;
        private double size;
        private ClientLevel level;
        private int frame = -1;
        private long ends;
        private int finest = 4;

        /**
         * @param range         how far the light reaches, in blocks
         * @param mostFaces     the most surfaces kept for one light, so a huge open space can't make it slow
         * @param refreshMillis how often a light's surfaces are found again: blocks placed or broken since
         * @param moved         how far a light may move before its surfaces are found again
         */
        Cache(int range, int mostFaces, long refreshMillis, double moved) {
            this(range, mostFaces, refreshMillis, moved, -2.0);
        }

        /**
         * For a light that only shines upward: only surfaces up from it at least this steeply (the height they're above
         * it, over how far they are) are found.
         */
        Cache(int range, int mostFaces, long refreshMillis, double moved, double lowest) {
            this.range = range;
            this.mostFaces = mostFaces;
            this.refreshMillis = refreshMillis;
            this.moved = moved;
            this.lowest = lowest;
            CACHES.add(this);
        }

        /**
         * The lights are this wide across, in blocks, for their shadows: a glowing thing has a size, so the edges of the
         * shadows it casts are soft.
         */
        Cache soft(double size) {
            this.size = size;
            return this;
        }

        /** How many points a block's surface is split into at most, each way: fewer for a light soft enough not to need them. */
        Cache finest(int perBlock) {
            this.finest = perBlock;
            return this;
        }

        /**
         * A block changed: each light whose surfaces may include it updates them at once, in place, which costs next to
         * nothing: the surfaces of that block and the blocks beside it are found again, and the light is worked out again
         * only at the points whose way to the light passes through it. A light still being searched for starts over.
         */
        void changed(BlockPos pos) {
            if (this.level == null) return;
            double reach = Mth.square(this.range + 2);
            for (Long2ObjectMap.Entry<Faces> entry : this.faces.long2ObjectEntrySet()) {
                Faces faces = entry.getValue();
                if (pos.distToCenterSqr(faces.x, faces.y, faces.z) > reach) continue;
                new Finder(this.level, faces, this.range, this.mostFaces, this.lowest, this.size).finest(this.finest).changed(pos);
            }
            for (Long2ObjectMap.Entry<Finder> entry : this.searching.long2ObjectEntrySet()) {
                Finder finder = entry.getValue();
                if (pos.distToCenterSqr(finder.x, finder.y, finder.z) <= reach) this.stale.add(entry.getLongKey());
            }
        }

        /** At the start of a frame: forgets everything when the world has changed. */
        void beginFrame(ClientLevel level) {
            if (level != this.level) {
                this.faces.clear();
                this.stale.clear();
                this.searching.clear();
                this.level = level;
            }
        }

        /**
         * A light's surfaces: what was found last, while they're found again if they're old, the light has moved or a
         * block among them changed. The search goes on a little each frame, within the frame's time, and its surfaces take
         * over once it's done.
         */
        Faces get(ClientLevel level, long key, double x, double y, double z, long now) {
            Faces known = this.faces.get(key);
            boolean fresh = known != null && now - known.foundAt < (known.incomplete ? this.refreshMillis : KEEP_MILLIS) && now >= known.foundAt && !this.stale.contains(key)
                    && Math.abs(known.x - x) < this.moved && Math.abs(known.y - y) < this.moved && Math.abs(known.z - z) < this.moved;
            if (fresh) return known;
            Finder finder = this.searching.get(key);
            if (finder == null || this.stale.contains(key)) {
                // a block changed since this search began, or there was none: start over from here
                finder = new Finder(level, new Faces(x, y, z, now), this.range, this.mostFaces, this.lowest, this.size).finest(this.finest);
                this.searching.put(key, finder);
                this.stale.remove(key);
            }
            if (this.frame != LitFaces.frame) {
                this.frame = LitFaces.frame;
                this.ends = System.nanoTime() + CACHE_BUDGET;
            }
            if (System.nanoTime() >= this.ends || !finder.work(this.ends)) return known;
            this.searching.remove(key);
            if (known == null && this.faces.size() >= MOST_KEPT) this.forgetFarthest(x, y, z);
            this.faces.put(key, finder.out);
            return finder.out;
        }

        /** Forgets the surfaces of the light farthest from here. */
        private void forgetFarthest(double x, double y, double z) {
            long farthest = 0L;
            double most = -1.0;
            for (Long2ObjectMap.Entry<Faces> entry : this.faces.long2ObjectEntrySet()) {
                Faces faces = entry.getValue();
                double distance = Mth.square(faces.x - x) + Mth.square(faces.y - y) + Mth.square(faces.z - z);
                if (distance > most) {
                    most = distance;
                    farthest = entry.getLongKey();
                }
            }
            if (most >= 0.0) this.faces.remove(farthest);
        }
    }

    /** Finds the surfaces around a light that face it and are open to the air, and how much of its light reaches each point of them. */
    private static final class Finder {
        private final ClientLevel level;
        private final Faces out;
        private final double x, y, z;
        private final int range, mostFaces;
        private final double lowest, size;
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(), next = new BlockPos.MutableBlockPos(),
                ray = new BlockPos.MutableBlockPos(), cover = new BlockPos.MutableBlockPos();
        private final double[] corners = new double[12];
        private double aimX, aimY, aimZ;
        /** How far through the blocks around the light the search has got. */
        private int cell;
        private int finest = 4;

        Finder finest(int perBlock) {
            this.finest = perBlock;
            return this;
        }

        /** Finds surfaces for {@code out}: all of them, or, after a block changes, those near it. */
        Finder(ClientLevel level, Faces out, int range, int mostFaces, double lowest, double size) {
            this.size = size;
            this.level = level;
            this.out = out;
            this.x = out.x;
            this.y = out.y;
            this.z = out.z;
            this.range = range;
            this.mostFaces = mostFaces;
            this.lowest = lowest;
        }

        /**
         * Goes on looking, block by block, until it's done or the frame's time is up.
         *
         * @return whether it's done
         */
        boolean work(long until) {
            int r = this.range + 2, side = 2 * r + 1, low = this.lowest > 0.0 ? -1 : -r, tall = r - low + 1, cells = side * tall * side;
            int cx = Mth.floor(this.x), cy = Mth.floor(this.y), cz = Mth.floor(this.z);
            if (this.cell == 0) {
                for (int chunkX = (cx - r) >> 4; chunkX <= (cx + r) >> 4; chunkX++) {
                    for (int chunkZ = (cz - r) >> 4; chunkZ <= (cz + r) >> 4; chunkZ++) {
                        if (!this.level.hasChunk(chunkX, chunkZ)) this.out.incomplete = true;
                    }
                }
            }
            while (this.cell < cells) {
                // the time is looked at every few blocks: looking costs a little too
                if ((this.cell & 15) == 0 && System.nanoTime() >= until) return false;
                int at = this.cell++;
                int dx = at / (tall * side) - r, dy = at / side % tall + low, dz = at % side - r;
                if (dx * dx + dy * dy + dz * dz > r * r) continue;
                this.pos.set(cx + dx, cy + dy, cz + dz);
                this.scan();
            }
            return true;
        }

        /** The surfaces of the block at {@link #pos}. */
        private void scan() {
            BlockState state = this.level.getBlockState(this.pos);
            if (state.isAir() || state.getBlock() instanceof HalfTransparentBlock || state.getBlock() instanceof IronBarsBlock) return;
            if (PedestalLighting.solid(this.level, this.pos)) {
                for (Direction face : DIRECTIONS) this.box(CUBE, face);
                return;
            }
            ModelFace[] model = model(state);
            if (model.length > 0) {
                for (ModelFace face : model) this.modelFace(face);
            } else {
                List<AABB> boxes = state.getBlock() instanceof PedestalBlock ? List.of(PedestalLighting.MODEL)
                        : state.getShape(this.level, this.pos).toAabbs();
                for (AABB box : boxes) for (Direction face : DIRECTIONS) this.box(box, face);
            }
        }

        /**
         * A block changed: the surfaces of it and the blocks beside it (which it may now cover or bare) are found again,
         * and the light worked out again wherever its way passes through the block, as its shadow may have come or gone.
         */
        void changed(BlockPos changed) {
            Faces out = this.out;
            int before = out.count;
            // its own surfaces and its neighbours': gone, and found again as they are now
            for (int f = 0; f < before; f++) {
                if (out.grids[f] == 0) continue;
                BlockPos owner = BlockPos.of(out.owners[f]);
                if (Math.abs(owner.getX() - changed.getX()) + Math.abs(owner.getY() - changed.getY()) + Math.abs(owner.getZ() - changed.getZ()) <= 1) {
                    out.grids[f] = 0;
                    out.reach[out.starts[f]] = 0.0F;
                }
            }
            this.pos.set(changed);
            this.scan();
            for (Direction side : DIRECTIONS) {
                this.pos.setWithOffset(changed, side);
                this.scan();
            }
            // the light again where its way passes through the block: as wide as the light, so a soft shadow's edge too
            double grow = this.size + 0.05;
            AABB block = new AABB(changed.getX() - grow, changed.getY() - grow, changed.getZ() - grow, changed.getX() + 1 + grow,
                    changed.getY() + 1 + grow, changed.getZ() + 1 + grow);
            for (int f = 0; f < before; f++) {
                int grid = out.grids[f];
                if (grid == 0) continue;
                this.pos.set(out.owners[f]);
                int start = out.starts[f], points = (grid + 1) * (grid + 1);
                double nx = out.normals[f * 3], ny = out.normals[f * 3 + 1], nz = out.normals[f * 3 + 2];
                double mx = out.middles[f * 4], my = out.middles[f * 4 + 1], mz = out.middles[f * 4 + 2];
                for (int k = 0; k < points; k++) {
                    int i = (start + k) * 3;
                    if (!crosses(block, this.x, this.y, this.z, out.points[i] - this.x, out.points[i + 1] - this.y, out.points[i + 2] - this.z)) continue;
                    out.reach[start + k] = this.reaches(start + k, nx, ny, nz, mx, my, mz);
                }
            }
            out.version++;
        }

        /** A face of a box of a block's shape, as its four corners: (s0, t0), (s0, t1), (s1, t1), (s1, t0) across it. */
        private void box(AABB box, Direction side) {
            Direction.Axis axis = side.getAxis();
            boolean positive = side.getAxisDirection() == Direction.AxisDirection.POSITIVE;
            int bx = this.pos.getX(), by = this.pos.getY(), bz = this.pos.getZ();
            double edge = positive ? box.max(axis) : box.min(axis), plane = axis.choose(bx, by, bz) + edge;
            Direction.Axis sAxis = PedestalLighting.sAxis(axis), tAxis = PedestalLighting.tAxis(axis);
            double s0 = sAxis.choose(bx, by, bz) + box.min(sAxis), s1 = sAxis.choose(bx, by, bz) + box.max(sAxis);
            double t0 = tAxis.choose(bx, by, bz) + box.min(tAxis), t1 = tAxis.choose(bx, by, bz) + box.max(tAxis);
            put(this.corners, 0, corner(axis, plane, s0, t0));
            put(this.corners, 1, corner(axis, plane, s0, t1));
            put(this.corners, 2, corner(axis, plane, s1, t1));
            put(this.corners, 3, corner(axis, plane, s1, t0));
            // on the block's own side, against a full block next to it: covered
            this.face(side.getStepX(), side.getStepY(), side.getStepZ(), edge <= 0.0 || edge >= 1.0 ? side : null);
        }

        private void modelFace(ModelFace face) {
            int bx = this.pos.getX(), by = this.pos.getY(), bz = this.pos.getZ();
            for (int k = 0; k < 4; k++) {
                this.corners[k * 3] = bx + face.corners[k * 3];
                this.corners[k * 3 + 1] = by + face.corners[k * 3 + 1];
                this.corners[k * 3 + 2] = bz + face.corners[k * 3 + 2];
            }
            this.face(face.nx, face.ny, face.nz, face.against < 0 ? null : DIRECTIONS[face.against]);
        }

        /** The surface in {@link #corners}, facing (nx, ny, nz), if it faces the light, is open to the air and the light reaches some of it. */
        private void face(double nx, double ny, double nz, Direction against) {
            if (this.out.count >= this.mostFaces) return;
            double[] c = this.corners;
            double mx = (c[0] + c[3] + c[6] + c[9]) / 4, my = (c[1] + c[4] + c[7] + c[10]) / 4, mz = (c[2] + c[5] + c[8] + c[11]) / 4;
            double tx = this.x - mx, ty = this.y - my, tz = this.z - mz, distance = Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (tx * nx + ty * ny + tz * nz <= 0.02 * distance) return;
            if (against != null) {
                this.next.setWithOffset(this.pos, against);
                if (PedestalLighting.solid(this.level, this.next)) return;
            }
            double half = 0;
            for (int k = 0; k < 4; k++) half = Math.max(half, Math.sqrt(Mth.square(c[k * 3] - mx) + Mth.square(c[k * 3 + 1] - my) + Mth.square(c[k * 3 + 2] - mz)));
            // a surface partly in reach still gets its share, fading out
            if (distance - half > this.range) return;
            // below where an upward light can reach, give or take the surface's size
            if (this.lowest > -2.0 && (my - this.y + half) / Math.max(distance, 1.0E-4) < this.lowest) return;
            double across = Math.max(Math.sqrt(Mth.square(c[9] - c[0]) + Mth.square(c[10] - c[1]) + Mth.square(c[11] - c[2])),
                    Math.sqrt(Mth.square(c[3] - c[0]) + Mth.square(c[4] - c[1]) + Mth.square(c[5] - c[2])));
            // nearer surfaces get a finer grid, so the light's shape stays true across them
            int perBlock = Math.min(distance < this.range * 0.4 ? 4 : distance < this.range * 0.8 ? 3 : 2, this.finest);
            int grid = Mth.clamp((int) Math.ceil(across * perBlock), 1, this.finest);
            this.add(grid, nx, ny, nz, mx, my, mz, half);
        }

        private void add(int grid, double nx, double ny, double nz, double mx, double my, double mz, double half) {
            int points = (grid + 1) * (grid + 1);
            Faces out = this.out;
            if (out.count == out.grids.length) {
                int capacity = out.count * 2;
                out.grids = Arrays.copyOf(out.grids, capacity);
                out.normals = Arrays.copyOf(out.normals, capacity * 3);
                out.starts = Arrays.copyOf(out.starts, capacity);
                out.middles = Arrays.copyOf(out.middles, capacity * 4);
                out.owners = Arrays.copyOf(out.owners, capacity);
            }
            while (out.used + points > out.reach.length) {
                int capacity = out.reach.length * 2;
                out.points = Arrays.copyOf(out.points, capacity * 3);
                out.vectors = Arrays.copyOf(out.vectors, capacity);
                out.reach = Arrays.copyOf(out.reach, capacity);
            }
            int start = this.out.used;
            double[] c = this.corners;
            for (int a = 0; a <= grid; a++) {
                for (int b = 0; b <= grid; b++) {
                    // across from the first corner toward the fourth (a), and toward the second (b)
                    double fa = a / (double) grid, fb = b / (double) grid;
                    int i = start + a * (grid + 1) + b;
                    for (int axis = 0; axis < 3; axis++) {
                        double near = c[axis] + (c[9 + axis] - c[axis]) * fa, far = c[3 + axis] + (c[6 + axis] - c[3 + axis]) * fa;
                        double lift = (axis == 0 ? nx : axis == 1 ? ny : nz) * LIFT;
                        this.out.points[i * 3 + axis] = near + (far - near) * fb + lift;
                    }
                    this.out.vectors[i] = new Vec3(this.out.points[i * 3], this.out.points[i * 3 + 1], this.out.points[i * 3 + 2]);
                }
            }
            // a surface mostly covered by something resting on it (the floor under a pedestal) isn't lit at all, its edges
            // included, rather than catching light along the sliver it leaves bare
            int covered = 0;
            for (int k = 0; k < points; k++) if (this.covered(start + k, nx, ny, nz, mx, my, mz)) covered++;
            if (covered >= points * MOSTLY_COVERED) return;
            // how much light reaches the surface's corners; if they're all in full light, so is the rest of it
            int[] corners = {0, grid, grid * (grid + 1), points - 1};
            int full = 0;
            boolean any = false;
            for (int k : corners) {
                float reaches = this.reaches(start + k, nx, ny, nz, mx, my, mz);
                this.out.reach[start + k] = reaches;
                if (reaches >= 1.0F) full++;
                if (reaches > 0.0F) any = true;
            }
            // wholly in shadow at its corners: unless it's big enough for light to fall between them, it's dark
            if (!any && half < 1.0) return;
            for (int k = 0; k < points; k++) {
                if (k == corners[0] || k == corners[1] || k == corners[2] || k == corners[3]) continue;
                this.out.reach[start + k] = full == 4 ? 1.0F : this.reaches(start + k, nx, ny, nz, mx, my, mz);
            }
            this.out.grids[this.out.count] = (byte) grid;
            this.out.normals[this.out.count * 3] = (float) nx;
            this.out.normals[this.out.count * 3 + 1] = (float) ny;
            this.out.normals[this.out.count * 3 + 2] = (float) nz;
            this.out.starts[this.out.count] = start;
            this.out.owners[this.out.count] = this.pos.asLong();
            this.out.middles[this.out.count * 4] = mx;
            this.out.middles[this.out.count * 4 + 1] = my;
            this.out.middles[this.out.count * 4 + 2] = mz;
            this.out.middles[this.out.count * 4 + 3] = half;
            this.out.used += points;
            this.out.count++;
        }

        /**
         * How much of the light reaches a point of a surface, from 0 to 1: aimed a hair off it and in from its edges, so
         * it doesn't graze them, from each of a few points across the light, as a glowing thing has a size, so a shadow's
         * edge is soft. None where something rests on the surface there, covering it (the floor under a pedestal).
         */
        private float reaches(int index, double nx, double ny, double nz, double mx, double my, double mz) {
            if (this.covered(index, nx, ny, nz, mx, my, mz)) return 0.0F;
            this.aim(index, nx, ny, nz, mx, my, mz, 0.03);
            if (this.size <= 0.0) return this.clear(this.x, this.y, this.z, this.aimX, this.aimY, this.aimZ) ? 1.0F : 0.0F;
            int lit = 0;
            for (int sample = 0; sample < SAMPLES.length; sample += 2) {
                double ox = this.x + SAMPLES[sample] * this.size, oz = this.z + SAMPLES[sample + 1] * this.size;
                if (this.clear(ox, this.y, oz, this.aimX, this.aimY, this.aimZ)) lit++;
            }
            return lit / (float) (SAMPLES.length / 2);
        }

        /**
         * Whether something rests on a surface at one of its points, covering it: looked for a little in from the surface's
         * edges, so something a hair narrower than the surface (a pedestal's base) still counts as covering its edges.
         */
        private boolean covered(int index, double nx, double ny, double nz, double mx, double my, double mz) {
            this.aim(index, nx, ny, nz, mx, my, mz, 0.1);
            this.cover.set(Mth.floor(this.aimX), Mth.floor(this.aimY), Mth.floor(this.aimZ));
            if (this.cover.equals(this.pos)) return false;
            BlockState state = this.level.getBlockState(this.cover);
            return !state.isAir() && covers(this.level, this.cover, state, this.aimX, this.aimY, this.aimZ);
        }

        /** Where to look from a point of a surface: a hair off it, and {@code inward} in from its edges so as not to graze them. */
        private void aim(int index, double nx, double ny, double nz, double mx, double my, double mz, double inward) {
            double px = this.out.points[index * 3], py = this.out.points[index * 3 + 1], pz = this.out.points[index * 3 + 2];
            double inX = mx - px, inY = my - py, inZ = mz - pz, in = Math.sqrt(inX * inX + inY * inY + inZ * inZ);
            double step = in > 1.0E-4 ? Math.min(inward, in) / in : 0.0;
            this.aimX = px + nx * 0.02 + inX * step;
            this.aimY = py + ny * 0.02 + inY * step;
            this.aimZ = pz + nz * 0.02 + inZ * step;
        }

        /**
         * Whether the light reaches a point: no full block, and nothing else with a shape but glass, in between. Every
         * block along the way counts, the light's own and the point's own too, as far as just short of the point: a
         * pedestal's lens shines from inside its block, and its cap still shades its own column and base below.
         */
        private boolean clear(double ox, double oy, double oz, double tx, double ty, double tz) {
            double dx = tx - ox, dy = ty - oy, dz = tz - oz;
            int bx = Mth.floor(ox), by = Mth.floor(oy), bz = Mth.floor(oz);
            int endX = Mth.floor(tx), endY = Mth.floor(ty), endZ = Mth.floor(tz);
            int sx = (int) Math.signum(dx), sy = (int) Math.signum(dy), sz = (int) Math.signum(dz);
            double stepX = sx == 0 ? Double.MAX_VALUE : Math.abs(1.0 / dx), stepY = sy == 0 ? Double.MAX_VALUE : Math.abs(1.0 / dy),
                    stepZ = sz == 0 ? Double.MAX_VALUE : Math.abs(1.0 / dz);
            double nextX = sx == 0 ? Double.MAX_VALUE : (sx > 0 ? bx + 1 - ox : ox - bx) * stepX;
            double nextY = sy == 0 ? Double.MAX_VALUE : (sy > 0 ? by + 1 - oy : oy - by) * stepY;
            double nextZ = sz == 0 ? Double.MAX_VALUE : (sz > 0 ? bz + 1 - oz : oz - bz) * stepZ;
            for (int steps = 0; steps < (this.range + 2) * 4; steps++) {
                this.ray.set(bx, by, bz);
                boolean last = bx == endX && by == endY && bz == endZ;
                // a full block in the way, short of the point's own block
                if (!last && !(steps == 0) && PedestalLighting.solid(this.level, this.ray)) return false;
                if (stops(this.level, this.ray, this.level.getBlockState(this.ray), ox, oy, oz, tx, ty, tz)) return false;
                if (last) return true;
                if (nextX < nextY && nextX < nextZ) {
                    bx += sx;
                    nextX += stepX;
                } else if (nextY < nextZ) {
                    by += sy;
                    nextY += stepY;
                } else {
                    bz += sz;
                    nextZ += stepZ;
                }
            }
            return true;
        }
    }

    /** Whether a block's shape covers a point, which lies in the block's space: something resting there, hiding what's under it. */
    private static boolean covers(ClientLevel level, BlockPos pos, BlockState state, double x, double y, double z) {
        if (state.getBlock() instanceof HalfTransparentBlock || state.getBlock() instanceof IronBarsBlock) return false;
        List<AABB> boxes = state.getBlock() instanceof PedestalBlock ? List.of(PedestalLighting.MODEL) : state.getShape(level, pos).toAabbs();
        double lx = x - pos.getX(), ly = y - pos.getY(), lz = z - pos.getZ();
        for (AABB box : boxes) if (box.contains(lx, ly, lz)) return true;
        return false;
    }

    /**
     * A block's model, as the faces it's drawn with that light can fall on: its solid faces, and those of its see-through
     * ones that are almost all solid (a lever's handle, not a flower's leaves). None for a block drawn without a model.
     */
    private static ModelFace[] model(BlockState state) {
        BlockStateModelSet models = Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        if (models != modelsFrom) {
            MODELS.clear();
            SOLID_ENOUGH.clear();
            modelsFrom = models;
        }
        ModelFace[] known = MODELS.get(state);
        if (known != null) return known;
        List<ModelFace> faces = new ArrayList<>();
        List<BlockStateModelPart> parts = new ArrayList<>();
        models.get(state).collectParts(RandomSource.create(42L), parts);
        for (BlockStateModelPart part : parts) {
            for (int side = -1; side < DIRECTIONS.length; side++) {
                for (BakedQuad quad : part.getQuads(side < 0 ? null : DIRECTIONS[side])) {
                    ChunkSectionLayer layer = quad.materialInfo().layer();
                    if (layer == ChunkSectionLayer.TRANSLUCENT) continue;
                    if (layer != ChunkSectionLayer.SOLID && !solidEnough(quad)) continue;
                    float[] corners = new float[12];
                    for (int k = 0; k < 4; k++) {
                        Vector3fc at = quad.position(k);
                        corners[k * 3] = at.x();
                        corners[k * 3 + 1] = at.y();
                        corners[k * 3 + 2] = at.z();
                    }
                    // which way it faces, from its corners
                    float ax = corners[3] - corners[0], ay = corners[4] - corners[1], az = corners[5] - corners[2];
                    float bx = corners[9] - corners[0], by = corners[10] - corners[1], bz = corners[11] - corners[2];
                    float nx = by * az - bz * ay, ny = bz * ax - bx * az, nz = bx * ay - by * ax;
                    float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
                    if (length < 1.0E-6F) continue;
                    nx /= length;
                    ny /= length;
                    nz /= length;
                    // face it the way the game says it faces, if the corners went round the other way
                    Direction facing = quad.direction();
                    if (nx * facing.getStepX() + ny * facing.getStepY() + nz * facing.getStepZ() < 0) {
                        nx = -nx;
                        ny = -ny;
                        nz = -nz;
                    }
                    faces.add(new ModelFace(corners, nx, ny, nz, side));
                }
            }
        }
        ModelFace[] model = faces.toArray(ModelFace[]::new);
        MODELS.put(state, model);
        return model;
    }

    /** Whether the part of its texture a see-through face shows is almost all solid. */
    private static boolean solidEnough(BakedQuad quad) {
        TextureAtlasSprite sprite = quad.materialInfo().sprite();
        float u0 = Float.MAX_VALUE, v0 = Float.MAX_VALUE, u1 = -Float.MAX_VALUE, v1 = -Float.MAX_VALUE;
        for (int k = 0; k < 4; k++) {
            long uv = quad.packedUV(k);
            u0 = Math.min(u0, UVPair.unpackU(uv));
            u1 = Math.max(u1, UVPair.unpackU(uv));
            v0 = Math.min(v0, UVPair.unpackV(uv));
            v1 = Math.max(v1, UVPair.unpackV(uv));
        }
        SpriteContents contents = sprite.contents();
        int width = contents.width(), height = contents.height();
        int x0 = Mth.clamp((int) ((u0 - sprite.getU0()) / (sprite.getU1() - sprite.getU0()) * width), 0, width - 1);
        int x1 = Mth.clamp((int) Math.ceil((u1 - sprite.getU0()) / (sprite.getU1() - sprite.getU0()) * width) - 1, x0, width - 1);
        int y0 = Mth.clamp((int) ((v0 - sprite.getV0()) / (sprite.getV1() - sprite.getV0()) * height), 0, height - 1);
        int y1 = Mth.clamp((int) Math.ceil((v1 - sprite.getV0()) / (sprite.getV1() - sprite.getV0()) * height) - 1, y0, height - 1);
        long key = (long) System.identityHashCode(contents) << 32 | (long) x0 << 24 | (long) x1 << 16 | (long) y0 << 8 | y1;
        Boolean known = SOLID_ENOUGH.get(key);
        if (known != null) return known;
        int solid = 0, all = 0;
        for (int px = x0; px <= x1; px++) {
            for (int py = y0; py <= y1; py++) {
                all++;
                if (!contents.isTransparent(0, px, py)) solid++;
            }
        }
        boolean enough = all > 0 && solid >= all * MOSTLY_SOLID;
        SOLID_ENOUGH.put(key, enough);
        return enough;
    }

    /**
     * Whether a block that isn't a full one stops light passing through its space from one point to another: anything
     * with a shape does where the light meets it (a slab, a fence, a chest, a pedestal's body), but glass and glass panes
     * let it through.
     */
    static boolean stops(ClientLevel level, BlockPos pos, BlockState state, double fromX, double fromY, double fromZ, double toX,
                         double toY, double toZ) {
        if (state.isAir() || state.getBlock() instanceof HalfTransparentBlock || state.getBlock() instanceof IronBarsBlock) return false;
        // what the block really takes up (its outline can be much bigger: a lever's covers its handle's whole swing), so a
        // lever or a flower casts no real shadow, and a slab, a fence or a chest does
        List<AABB> boxes = state.getBlock() instanceof PedestalBlock ? List.of(PedestalLighting.MODEL)
                : state.getCollisionShape(level, pos).toAabbs();
        double x = fromX - pos.getX(), y = fromY - pos.getY(), z = fromZ - pos.getZ();
        for (AABB box : boxes) if (crosses(box, x, y, z, toX - fromX, toY - fromY, toZ - fromZ)) return true;
        return false;
    }

    /**
     * Whether the way from a point (given in the box's block) to another, {@code (dx, dy, dz)} on, passes through a box
     * before it gets there: a hair short of the end, so a surface doesn't shade itself.
     */
    private static boolean crosses(AABB box, double x, double y, double z, double dx, double dy, double dz) {
        double near = 0.0, far = 0.98;
        double[] from = {x, y, z}, way = {dx, dy, dz}, low = {box.minX, box.minY, box.minZ}, high = {box.maxX, box.maxY, box.maxZ};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(way[i]) < 1.0E-9) {
                if (from[i] <= low[i] || from[i] >= high[i]) return false;
                continue;
            }
            double a = (low[i] - from[i]) / way[i], b = (high[i] - from[i]) / way[i];
            near = Math.max(near, Math.min(a, b));
            far = Math.min(far, Math.max(a, b));
            if (near >= far) return false;
        }
        return true;
    }

    private static void put(double[] corners, int index, Vec3 at) {
        corners[index * 3] = at.x;
        corners[index * 3 + 1] = at.y;
        corners[index * 3 + 2] = at.z;
    }

    static Vec3 corner(Direction.Axis axis, double plane, double s, double t) {
        return switch (axis) {
            case X -> new Vec3(plane, t, s);
            case Y -> new Vec3(s, plane, t);
            case Z -> new Vec3(s, t, plane);
        };
    }
}
