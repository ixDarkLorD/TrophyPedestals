package net.ixdarklord.trophypedestals.client;

import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxBlend;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxLayer;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldVfx;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.client.lens.PrismLensVisuals;
import net.ixdarklord.trophypedestals.pedestal.Lens;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * Coloured light from lit pedestals: a lens's colour, or a ranked trophy's. The game's own block light is always white,
 * so the light is drawn, cast from where the pedestal's effects shine (see {@link SpotLights}): its showcase light up onto
 * whatever is above it, and the Prism's light through each pane of its halo's stained glass and along each ray of its fan.
 * Each lands as a patch where it meets a block, blocks in the way cast a shadow, and it moves as the lens does: the
 * Inferno's flickers, the Blood Moon's beats. The lens's glow tints its own pedestal and the pedestals beside it. It's
 * drawn with the other world effects, so shader packs keep it, and the {@code pedestal_lighting} client option turns it
 * off.
 * <p>
 * The pedestals' faces each glow reaches, and how it falls on them, are worked out now and then and kept.
 */
public final class PedestalLighting {
    /** How far, in blocks, a pedestal's glow reaches the pedestals around it. */
    private static final int REACH = 3;
    /** How many pedestals light their surroundings at once: the nearest ones. */
    private static final int MAX_SOURCES = 64;
    /**
     * How often, in milliseconds, the faces around a pedestal are found again: blocks placed or broken since, and chunks
     * that weren't loaded yet. Real time, so it goes on while the game's time stands still.
     */
    private static final long REFRESH_MILLIS = 3000L;
    /** How many pedestals may look for their faces in one frame; the rest keep what they found until a later frame. */
    private static final int SEARCHES_PER_FRAME = 2;
    /** How strong the light is where it falls straight on a face right by the pedestal (the glow's alpha). */
    private static final float STRENGTH = 0.3F;
    /**
     * The glow of the lens itself on the pedestals right around it, its own most of all, against the light it casts. Blocks
     * around get only the light cast onto them.
     */
    private static final float NEAR_MODEL = 0.8F;
    /** How wide the lens's glow is on a face right by it, and how much wider for each block farther off. */
    private static final double NEAR_RADIUS = 1.0, NEAR_WIDENS = 0.6;
    /** The showcase light, shining up: how wide its cone starts, how fast it widens, and how strong it lands. */
    private static final double BEAM_FOOT = 0.35, BEAM_SPREAD = 0.2;
    private static final float BEAM_POWER = 1.8F;
    /** The Prism's halo, projected around it (see {@link HaloLight}): how strong, and how much more a glinting pane. */
    private static final float SHARD_POWER = 2.0F;
    /** The Prism's fan of rays: thin beams, each landing as a small spot. */
    private static final double FAN_FOOT = 0.05, FAN_SPREAD = 0.07;
    private static final float FAN_POWER = 1.4F;
    /** The brightness of colour every light is evened out toward. */
    private static final float BALANCED = 0.55F;
    /** What's kept of each face: side, s0, t0, s1, t1, plane, what it is ({@link #OWN}, ...); and of how the light falls. */
    private static final int PLACE = 7, FALL = 5;
    /** Lifted off the face, toward the light, so it doesn't flicker into the block. */
    private static final double LIFT = 0.003;

    /**
     * A soft pool of light that fades to nothing at its rim, so neighbouring faces join without a seam. Its shader works
     * the pool out itself; the texture is only what the material needs to have.
     */
    private static final VfxMaterial GLOW = new VfxMaterial(TrophyPedestalsMod.id("textures/vfx/light_pool.png"), VfxBlend.ADDITIVE, 0.0F,
            TrophyPedestalsMod.id("vfx/light_pool"));
    /**
     * The same light multiplied into a pedestal's model as well: added light can't colour something already near white
     * (white and orange add up to white), so the model is also tinted by the light's colour, which keeps its detail.
     */
    /** The Prism's halo projected around it: the panes' colours worked out by its shader (see {@link HaloLight}). */
    private static final VfxMaterial HALO = new VfxMaterial(TrophyPedestalsMod.id("textures/vfx/light_pool.png"), VfxBlend.ADDITIVE, 0.0F,
            TrophyPedestalsMod.id("vfx/light_halo"));
    /**
     * The layer the light is drawn in: below every other effect, as the surfaces it falls on are. World effects write no
     * depth, so whatever is drawn later covers what's drawn earlier; drawn first, the light stays behind a Void Rift's
     * opening, as everything beyond the rift does, while where a surface is nearer than the rift, the rift isn't drawn.
     */
    private static final VfxLayer SURFACES = VfxLayer.of(-100000);
    /** The light the pedestals' effects give off, on the surfaces around them (see {@link PointLights}). */
    private static final VfxMaterial POINT = new VfxMaterial(TrophyPedestalsMod.id("textures/vfx/light_pool.png"), VfxBlend.ADDITIVE, 0.0F,
            TrophyPedestalsMod.id("vfx/light_point"));
    /** Cones of light shining up, where they land: round spots with soft edges (see {@link SpotLights}). */
    private static final VfxMaterial SPOT = new VfxMaterial(GLOW.texture(), VfxBlend.ADDITIVE, 0.0F, TrophyPedestalsMod.id("vfx/light_spot"));
    private static final VfxMaterial TINT = new VfxMaterial(GLOW.texture(), VfxBlend.MULTIPLY, 0.0F, TrophyPedestalsMod.id("vfx/light_tint"));
    /** How deeply a pedestal's model is tinted, against how strongly it's lit; and the most it ever is. */
    private static final float TINT_STRENGTH = 1.3F, MAX_TINT = 0.55F;
    /** What a face is: a neighbouring pedestal's, or the lit pedestal's own. */
    private static final double PEDESTAL = 1.0, OWN = 2.0;
    private static final Direction[] DIRECTIONS = Direction.values();
    /**
     * The solid parts of a pedestal's model, which its light falls on: its base, step, trim band, column, collar, cap and
     * the rim around its tray. Not its block shape: that fills the rim and tray as one box, and a case's glass as another,
     * so the light would show on air.
     */
    /** Where a pedestal's cap starts, above its column. */
    private static final double CAP_BOTTOM = 12 / 16.0;
    static final AABB[] MODEL = {
            box(0.5, 0, 0.5, 15.5, 1.5, 15.5), box(1.5, 1.5, 1.5, 14.5, 3, 14.5), box(3, 3, 3, 13, 3.5, 13),
            box(4, 3.5, 4, 12, 12, 12), box(3.5, 11.5, 3.5, 12.5, 12, 12.5), box(2, 12, 2, 14, 14, 14),
            box(3, 14, 3, 13, 15, 4), box(3, 14, 12, 13, 15, 13), box(3, 14, 4, 4, 15, 12), box(12, 14, 4, 13, 15, 12)};

    private static AABB box(double x0, double y0, double z0, double x1, double y1, double z1) {
        return new AABB(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0);
    }

    private static final double[] sourceX = new double[MAX_SOURCES], sourceY = new double[MAX_SOURCES], sourceZ = new double[MAX_SOURCES];
    private static final long[] sourceBlock = new long[MAX_SOURCES];
    private static final int[] sourceColor = new int[MAX_SOURCES];
    private static final float[] sourceStrength = new float[MAX_SOURCES], sourceDistance = new float[MAX_SOURCES];
    private static final int[] sourceOrder = new int[MAX_SOURCES];
    private static final @Nullable Lens[] sourceLens = new Lens[MAX_SOURCES];
    private static final double[] sourceTop = new double[MAX_SOURCES], haloX = new double[MAX_SOURCES], haloY = new double[MAX_SOURCES],
            haloZ = new double[MAX_SOURCES];
    private static final float[] sourceFaceX = new float[MAX_SOURCES], sourceFaceZ = new float[MAX_SOURCES], sourceSize = new float[MAX_SOURCES],
            sourceTime = new float[MAX_SOURCES];
    private static int sourceCount;

    /** What each pedestal lights: kept between frames, found again every {@link #REFRESH_MILLIS}. */
    private static final Long2ObjectOpenHashMap<Lit> LIT = new Long2ObjectOpenHashMap<>();
    private static ClientLevel litLevel;
    private static int searchesLeft;

    /**
     * The faces a pedestal's light reaches, from where it shines: per face its four corners, where it is ({@link #PLACE}
     * values) and how the light from where it shines falls on it ({@link #FALL} values: glow corners and strength).
     */
    private record Lit(double x, double y, double z, long foundAt, int count, Vec3[] corners, double[] places, float[] falls) {
    }

    private PedestalLighting() {
    }

    public static void init() {
        WorldVfx.register(TrophyPedestalsMod.id("pedestal_lighting"), PedestalLighting::render);
    }

    public static boolean enabled() {
        return TrophyPedestalsClientConfig.PEDESTAL_LIGHTING.get();
    }

    /**
     * A pedestal lighting its surroundings this frame.
     *
     * @param pedestal the pedestal's block
     * @param x        where the lens's glow lights its surroundings from, in the world
     * @param top      the height of the lens, where the showcase light starts shining up
     * @param color    the light's colour
     * @param strength from 0 (off) to 1 (fully lit)
     * @param lens     its lens, which can colour and move the light (the Prism's rainbow, the Inferno's flicker)
     * @param halo     the heart of the Prism's halo, in the world
     * @param faceX    the way across the halo's plane, which faces the camera
     * @param size     how big the lens's effects are
     * @param time     the lens's clock, in ticks
     * @param camera   where the camera is: far lights fade, and the nearest pedestals win when there are too many
     */
    public static void add(BlockPos pedestal, double x, double y, double z, double top, int color, float strength, @Nullable Lens lens,
                           Vec3 halo, float faceX, float faceZ, float size, float time, Vec3 camera) {
        if (strength <= 0.0F || !enabled()) return;
        // as far as the world is drawn, fading out toward its end
        double distance = Math.sqrt(camera.distanceToSqr(x, y, z));
        strength *= (float) LitFaces.sight(distance);
        if (strength <= 0.0F) return;
        int slot = sourceCount;
        if (sourceCount == MAX_SOURCES) {
            // full: replace the farthest, if this one is nearer
            slot = 0;
            for (int i = 1; i < MAX_SOURCES; i++) if (sourceDistance[i] > sourceDistance[slot]) slot = i;
            if (sourceDistance[slot] <= distance) return;
        } else {
            sourceCount++;
        }
        sourceBlock[slot] = pedestal.asLong();
        sourceX[slot] = x;
        sourceY[slot] = y;
        sourceZ[slot] = z;
        sourceColor[slot] = fullBright(color);
        sourceStrength[slot] = Math.min(1.0F, strength);
        sourceLens[slot] = lens;
        sourceDistance[slot] = (float) distance;
        sourceTop[slot] = top;
        haloX[slot] = halo.x;
        haloY[slot] = halo.y;
        haloZ[slot] = halo.z;
        sourceFaceX[slot] = faceX;
        sourceFaceZ[slot] = faceZ;
        sourceSize[slot] = size;
        sourceTime[slot] = time;
    }

    /**
     * Light a pedestal's effects give off, as they're drawn (see {@link PedestalLight.Glow#light}).
     *
     * @param pedestal the pedestal's block, whose lights share their surfaces
     */
    public static void emit(long pedestal, double x, double y, double z, int color, float strength, float radius) {
        if (enabled()) PointLights.emit(pedestal, x, y, z, color, strength, radius);
    }

    /**
     * A block changed in the world: the light around it is worked out again as soon as it can be, rather than when it's
     * next due, so breaking or placing a block near a pedestal changes its light at once.
     */
    public static void blockChanged(BlockPos pos) {
        LitFaces.blockChanged(pos);
        double reach = Mth.square(REACH + 2);
        for (Long2ObjectMap.Entry<Lit> entry : LIT.long2ObjectEntrySet()) {
            Lit lit = entry.getValue();
            if (pos.distToCenterSqr(lit.x, lit.y, lit.z) <= reach) {
                // as if found long ago: found again first chance, kept meanwhile
                entry.setValue(new Lit(lit.x, lit.y, lit.z, 0L, lit.count, lit.corners, lit.places, lit.falls));
            }
        }
    }

    private static void render(VfxFrame frame) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !enabled()) {
            sourceCount = 0;
            return;
        }
        LitFaces.beginFrame();
        // the light the pedestals' effects gave off as they were drawn
        PointLights.render(frame.draw(POINT, SURFACES), level, frame.cameraPosition(), Util.getMillis());
        if (sourceCount == 0) return;
        if (level != litLevel) {
            LIT.clear();
            litLevel = level;
        }
        long now = Util.getMillis();
        // the effects' clock, in ticks: it stops while the game is paused, and runs on when the game's time is held
        float time = frame.time() * 20.0F;
        searchesLeft = SEARCHES_PER_FRAME;
        VfxDraw draw = frame.draw(GLOW, SURFACES), tint = frame.draw(TINT, SURFACES), spot = frame.draw(SPOT, SURFACES),
                halo = frame.draw(HALO, SURFACES);
        HaloLight.beginFrame(level);
        SpotLights.beginFrame(level);
        // the nearest pedestals first, so their surfaces are found first when there's a lot to find
        for (int i = 0; i < sourceCount; i++) sourceOrder[i] = i;
        IntArrays.quickSort(sourceOrder, 0, sourceCount, (a, b) -> Float.compare(sourceDistance[a], sourceDistance[b]));
        for (int n = 0; n < sourceCount; n++) {
            int s = sourceOrder[n];
            Lens lens = sourceLens[s];
            float strength = sourceStrength[s] * beat(lens, time, sourceBlock[s]);
            boolean prism = lens == Lens.PRISM;
            // the Prism's own glow is the colour its light has reached on its way round the rainbow
            int color = prism ? fullBright(ARGB.opaque(LensLooks.prism(sourceTime[s], 0.0F))) : sourceColor[s];
            // the lens's own glow, on what's right around it and on the pedestal
            Lit lit = lit(level, sourceBlock[s], sourceX[s], sourceY[s], sourceZ[s], now);
            float even = balance(color);
            if (lit != null) drawStill(draw, tint, lit, color, strength * even * NEAR_MODEL);
            // the showcase light, shining up to whatever is above
            SpotLights.draw(spot, level, sourceBlock[s], sourceX[s], sourceTop[s], sourceZ[s], 0.0, 1.0, 0.0, BEAM_FOOT * sourceSize[s],
                    BEAM_SPREAD, light(color, strength * BEAM_POWER), sourceDistance[s], now);
            if (prism) castPrism(spot, halo, level, s, strength, now);
        }
        sourceCount = 0;
    }

    /**
     * The Prism's light: from the halo's heart through each pane of its stained glass, a patch of the pane's colour thrown
     * onto the walls, floor and ceiling and turning with the halo, flaring as a streak passes through the pane; and where
     * each ray of its fan lands, a small spot of the ray's colour.
     */
    private static void castPrism(VfxDraw spot, VfxDraw halo, ClientLevel level, int s, float strength, long now) {
        float time = sourceTime[s], roll = PrismLensVisuals.haloRoll(time);
        float faceX = sourceFaceX[s], faceZ = sourceFaceZ[s], size = sourceSize[s];
        // the pane a streak of light is passing through now, if any, glints
        int glint = -1;
        float shine = 0.0F;
        for (int shard = 0; shard < PrismLensVisuals.HALO_SHARDS; shard++) {
            float pane = PrismLensVisuals.shardGlint(time, shard);
            if (pane > shine) {
                shine = pane;
                glint = shard;
            }
        }
        HaloLight.draw(halo, level, sourceBlock[s], haloX[s], haloY[s], haloZ[s], faceX, faceZ, roll, glint, shine,
                STRENGTH * SHARD_POWER * strength, sourceDistance[s], now);
        for (int ray = 0; ray < PrismLensVisuals.FAN_RAYS; ray++) {
            float angle = PrismLensVisuals.fanAngle(time, ray);
            int color = fullBright(ARGB.opaque(PrismLensVisuals.fanColor(time, ray)));
            SpotLights.draw(spot, level, sourceBlock[s], sourceX[s], sourceTop[s], sourceZ[s], Mth.cos(angle) * PrismLensVisuals.FAN_REACH,
                    PrismLensVisuals.FAN_RISE, Mth.sin(angle) * PrismLensVisuals.FAN_REACH, FAN_FOOT * size, FAN_SPREAD,
                    light(color, strength * FAN_POWER * PrismLensVisuals.fanPulse(time, ray)), sourceDistance[s], now);
        }
    }

    /** A light's colour with how strong it is in its alpha, evened out for how bright its colour is. */
    private static int light(int color, float strength) {
        return ARGB.color(Math.min(255, (int) (255.0F * STRENGTH * strength * balance(color))), color);
    }

    /** A colour at full brightness, its hue kept: a dark colour of light would barely show however strong it is. */
    static int fullBright(int color) {
        int brightest = Math.max(ARGB.red(color), Math.max(ARGB.green(color), ARGB.blue(color)));
        if (brightest == 0) return 0xFFFFFFFF;
        float scale = 255.0F / brightest;
        return ARGB.color(255, Math.round(ARGB.red(color) * scale), Math.round(ARGB.green(color) * scale), Math.round(ARGB.blue(color) * scale));
    }

    /**
     * How much brighter or dimmer a colour of light is drawn, so every colour looks about as strong: added light looks as
     * bright as the colour is, so yellow and green would wash a floor out while red and purple barely showed.
     */
    static float balance(int color) {
        float luminance = (0.2126F * ARGB.red(color) + 0.7152F * ARGB.green(color) + 0.0722F * ARGB.blue(color)) / 255.0F;
        return Mth.clamp((float) Math.pow(BALANCED / Math.max(luminance, 0.05F), 0.75), 0.55F, 1.9F);
    }

    /** How the lens moves its light now: the Inferno's flicker, the Blood Moon's heartbeat, the Void Rift's breath. */
    private static float beat(@Nullable Lens lens, float time, long block) {
        if (lens == null) return 1.0F;
        // each pedestal out of step with the others
        float seed = (float) (Long.hashCode(block) & 1023) / 1023.0F * Mth.TWO_PI;
        return switch (lens) {
            case EMBER -> 0.8F + 0.12F * Mth.sin(time * 0.9F + seed) + 0.08F * Mth.sin(time * 2.3F + seed * 1.7F);
            case SANGUINE -> {
                float phase = (time / 30.0F + seed) % 1.0F;
                float pulse = (float) (Math.exp(-Mth.square(phase / 0.06F)) + 0.6 * Math.exp(-Mth.square((phase - 0.18F) / 0.06F)));
                yield 0.8F + 0.3F * pulse;
            }
            case ENDER -> 0.85F + 0.15F * Mth.sin(time * 0.05F + seed);
            // now and then the projection stutters
            case HOLOGRAM -> (Mth.floor(time / 3.0F) + (int) (seed * 7)) % 17 == 0 ? 0.55F : 1.0F;
            default -> 1.0F;
        };
    }

    /**
     * Draws the lens's glow on the pedestals around it as it was worked out when their faces were found, tinting them too.
     */
    private static void drawStill(VfxDraw draw, VfxDraw tint, Lit lit, int color, float strength) {
        float[] falls = lit.falls;
        for (int f = 0; f < lit.count; f++) {
            double kind = lit.places[f * PLACE + 6];
            int at = f * FALL;
            quad(draw, tint, lit, f, kind, falls[at], falls[at + 1], falls[at + 2], falls[at + 3], color, strength * falls[at + 4]);
        }
    }

    private static void quad(VfxDraw draw, VfxDraw tint, Lit lit, int f, double kind, float u0, float v0, float u1, float v1,
                             int color, float strength) {
        int alpha = Math.min(255, (int) (255.0F * STRENGTH * strength));
        if (alpha <= 0) return;
        Vec3[] corners = lit.corners;
        Vec3 a = corners[f * 4], b = corners[f * 4 + 1], c = corners[f * 4 + 2], d = corners[f * 4 + 3];
        draw.quad(a, b, c, d, u0, v0, u1, v1, ARGB.color(alpha, color));
        tint.quad(a, b, c, d, u0, v0, u1, v1, ARGB.color((int) Math.min(255.0F * MAX_TINT, alpha * TINT_STRENGTH), color));
    }

    /**
     * How light from a point falls on a face: a glow centred where it falls straight, wider and fainter farther off, and
     * brighter where it falls square on. Writes glow corners u0, v0, u1, v1 and the strength to {@code out} at {@code to}.
     *
     * @return whether it lights the face at all
     */
    private static boolean fall(double[] places, int face, double x, double y, double z, float[] out, int to) {
        int at = face * PLACE;
        Direction side = DIRECTIONS[(int) places[at]];
        double s0 = places[at + 1], t0 = places[at + 2], s1 = places[at + 3], t1 = places[at + 4], plane = places[at + 5];
        Direction.Axis axis = side.getAxis();
        double away = (axis.choose(x, y, z) - plane) * side.getAxisDirection().getStep();
        // a face turned away from the light, or out of its reach
        if (away <= 0.05 || away >= REACH) return false;
        Direction.Axis sAxis = axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        Direction.Axis tAxis = axis == Direction.Axis.Y ? Direction.Axis.Z : Direction.Axis.Y;
        double spotS = sAxis.choose(x, y, z), spotT = tAxis.choose(x, y, z);
        double radius = NEAR_RADIUS + away * NEAR_WIDENS;
        double nearS = Mth.clamp(spotS, s0, s1), nearT = Mth.clamp(spotT, t0, t1);
        if (Mth.square(nearS - spotS) + Mth.square(nearT - spotT) >= radius * radius) return false;
        double fade = 1.0 - away / REACH;
        // how square on it falls on the face's middle: grazing light is weaker
        double square = away / Math.sqrt(away * away + Mth.square((s0 + s1) * 0.5 - spotS) + Mth.square((t0 + t1) * 0.5 - spotT));
        out[to] = (float) (0.5 + (s0 - spotS) / (2.0 * radius));
        out[to + 1] = (float) (0.5 + (t0 - spotT) / (2.0 * radius));
        out[to + 2] = (float) (0.5 + (s1 - spotS) / (2.0 * radius));
        out[to + 3] = (float) (0.5 + (t1 - spotT) / (2.0 * radius));
        out[to + 4] = (float) (fade * fade * (0.3 + 0.7 * square));
        return true;
    }

    /** A point on a face: its plane along the face's axis, and (s, t) across it. */
    private static Vec3 corner(Direction.Axis axis, double plane, double s, double t) {
        return switch (axis) {
            case X -> new Vec3(plane, t, s);
            case Y -> new Vec3(s, plane, t);
            case Z -> new Vec3(s, t, plane);
        };
    }

    /**
     * What a pedestal lights, found again when it's old or the light has moved: if this frame still has searches left, or
     * it was never found; otherwise what it found last time.
     */
    private static @Nullable Lit lit(ClientLevel level, long block, double x, double y, double z, long now) {
        Lit known = LIT.get(block);
        boolean fresh = known != null && now - known.foundAt < REFRESH_MILLIS && now >= known.foundAt
                && Math.abs(known.x - x) < 0.01 && Math.abs(known.y - y) < 0.01 && Math.abs(known.z - z) < 0.01;
        if (fresh || (known != null && searchesLeft <= 0)) return known;
        if (searchesLeft <= 0) return null;
        searchesLeft--;
        Lit found = new Finder(level, block, x, y, z).find(now);
        if (LIT.size() > 256) LIT.clear();
        LIT.put(block, found);
        return found;
    }

    /**
     * Finds what a pedestal's glow reaches: the faces of its own model, and of its neighbours' that face it with nothing
     * in between.
     */
    private static final class Finder {
        private final ClientLevel level;
        private final long block;
        private final BlockPos center;
        private final double x, y, z;
        private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(), next = new BlockPos.MutableBlockPos(),
                ray = new BlockPos.MutableBlockPos();
        private int capacity = 64, count;
        private Vec3[] corners = new Vec3[capacity * 4];
        private double[] places = new double[capacity * PLACE];
        private float[] falls = new float[capacity * FALL];

        Finder(ClientLevel level, long block, double x, double y, double z) {
            this.level = level;
            this.block = block;
            this.center = BlockPos.of(block);
            this.x = x;
            this.y = y;
            this.z = z;
        }

        Lit find(long now) {
            for (int dx = -REACH; dx <= REACH; dx++) {
                for (int dy = -REACH; dy <= REACH; dy++) {
                    for (int dz = -REACH; dz <= REACH; dz++) {
                        this.pos.set(this.center.getX() + dx, this.center.getY() + dy, this.center.getZ() + dz);
                        BlockState state = this.level.getBlockState(this.pos);
                        if (state.getBlock() instanceof PedestalBlock) this.pedestal();
                    }
                }
            }
            return new Lit(this.x, this.y, this.z, now, this.count, this.corners, this.places, this.falls);
        }

        /** A pedestal's model, box by box: each box's faces that face the light, or all of them on the lit pedestal itself. */
        private void pedestal() {
            boolean own = this.pos.asLong() == this.block;
            int bx = this.pos.getX(), by = this.pos.getY(), bz = this.pos.getZ();
            for (AABB box : MODEL) {
                // of its own body, its light only reaches the cap and the rim round its well: the cap shades the rest
                if (own && box.minY < CAP_BOTTOM) continue;
                for (Direction side : DIRECTIONS) {
                    Direction.Axis axis = side.getAxis();
                    boolean positive = side.getAxisDirection() == Direction.AxisDirection.POSITIVE;
                    double plane = axis.choose(bx, by, bz) + box.max(axis) * (positive ? 1 : 0) + box.min(axis) * (positive ? 0 : 1);
                    double away = (axis.choose(this.x, this.y, this.z) - plane) * side.getAxisDirection().getStep();
                    if (!own && away <= 0.05) continue;
                    if (!own) {
                        double cx = bx + (box.minX + box.maxX) * 0.5 + side.getStepX() * (box.getXsize() * 0.5 + 0.01);
                        double cy = by + (box.minY + box.maxY) * 0.5 + side.getStepY() * (box.getYsize() * 0.5 + 0.01);
                        double cz = bz + (box.minZ + box.maxZ) * 0.5 + side.getStepZ() * (box.getZsize() * 0.5 + 0.01);
                        if (!clear(this.level, this.x, this.y, this.z, cx, cy, cz, this.pos.asLong(), this.ray)) continue;
                    }
                    Direction.Axis sAxis = sAxis(axis), tAxis = tAxis(axis);
                    double s0 = sAxis.choose(bx, by, bz) + box.min(sAxis), t0 = tAxis.choose(bx, by, bz) + box.min(tAxis);
                    this.add(side, s0, t0, s0 + box.max(sAxis) - box.min(sAxis), t0 + box.max(tAxis) - box.min(tAxis), plane, own ? OWN : PEDESTAL);
                }
            }
        }

        private void add(Direction side, double s0, double t0, double s1, double t1, double plane, double kind) {
            if (this.count == this.capacity) {
                this.capacity *= 2;
                this.corners = Arrays.copyOf(this.corners, this.capacity * 4);
                this.places = Arrays.copyOf(this.places, this.capacity * PLACE);
                this.falls = Arrays.copyOf(this.falls, this.capacity * FALL);
            }
            int at = this.count * PLACE;
            this.places[at] = side.ordinal();
            this.places[at + 1] = s0;
            this.places[at + 2] = t0;
            this.places[at + 3] = s1;
            this.places[at + 4] = t1;
            this.places[at + 5] = plane;
            this.places[at + 6] = kind;
            if (!fall(this.places, this.count, this.x, this.y, this.z, this.falls, this.count * FALL)) return;
            Direction.Axis axis = side.getAxis();
            double lifted = plane + LIFT * side.getAxisDirection().getStep();
            int c = this.count * 4;
            this.corners[c] = corner(axis, lifted, s0, t0);
            this.corners[c + 1] = corner(axis, lifted, s0, t1);
            this.corners[c + 2] = corner(axis, lifted, s1, t1);
            this.corners[c + 3] = corner(axis, lifted, s1, t0);
            this.count++;
        }
    }

    /** The direction across a face its s runs along, and its t. */
    static Direction.Axis sAxis(Direction.Axis axis) {
        return axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
    }

    static Direction.Axis tAxis(Direction.Axis axis) {
        return axis == Direction.Axis.Y ? Direction.Axis.Z : Direction.Axis.Y;
    }

    /** Whether nothing solid stands between two points (the pedestal itself aside): walks the blocks the line crosses. */
    private static boolean clear(ClientLevel level, double x0, double y0, double z0, double x1, double y1, double z1, long pedestal,
                                 BlockPos.MutableBlockPos pos) {
        int x = Mth.floor(x0), y = Mth.floor(y0), z = Mth.floor(z0);
        int endX = Mth.floor(x1), endY = Mth.floor(y1), endZ = Mth.floor(z1);
        double dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
        int stepX = (int) Math.signum(dx), stepY = (int) Math.signum(dy), stepZ = (int) Math.signum(dz);
        double deltaX = stepX == 0 ? Double.MAX_VALUE : Math.abs(1.0 / dx), deltaY = stepY == 0 ? Double.MAX_VALUE : Math.abs(1.0 / dy),
                deltaZ = stepZ == 0 ? Double.MAX_VALUE : Math.abs(1.0 / dz);
        double maxX = stepX == 0 ? Double.MAX_VALUE : (stepX > 0 ? x + 1 - x0 : x0 - x) * deltaX;
        double maxY = stepY == 0 ? Double.MAX_VALUE : (stepY > 0 ? y + 1 - y0 : y0 - y) * deltaY;
        double maxZ = stepZ == 0 ? Double.MAX_VALUE : (stepZ > 0 ? z + 1 - z0 : z0 - z) * deltaZ;
        for (int steps = 0; steps < REACH * 4; steps++) {
            if (x == endX && y == endY && z == endZ) return true;
            if (maxX < maxY && maxX < maxZ) {
                x += stepX;
                maxX += deltaX;
            } else if (maxY < maxZ) {
                y += stepY;
                maxY += deltaY;
            } else {
                z += stepZ;
                maxZ += deltaZ;
            }
            if (x == endX && y == endY && z == endZ) return true;
            pos.set(x, y, z);
            if (pos.asLong() != pedestal && solid(level, pos)) return false;
        }
        return true;
    }

    /** Whether a block is full and opaque: it takes the light on its faces and stops it going through. */
    static boolean solid(ClientLevel level, BlockPos pos) {
        return level.getBlockState(pos).isSolidRender();
    }
}
