package net.ixdarklord.trophypedestals.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxBlend;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxDraw;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxFrame;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxLayer;
import net.ixdarklord.coolcatcanvas.api.client.vfx.VfxMaterial;
import net.ixdarklord.coolcatcanvas.api.client.vfx.WorldVfx;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.compat.ARGB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Where a pedestal's light goes: its glows, outlines, beam, halos, crown shards and flares. */
public final class PedestalLight {
    private static final VfxMaterial FADE = new VfxMaterial(TrophyPedestalsMod.id("textures/misc/corner_fade.png"), VfxBlend.ADDITIVE, 0.0F);
    private static final int FADE_SIZE = 64;
    private static final VfxMaterial LENS = new VfxMaterial(VfxMaterial.GLOW, VfxBlend.ADDITIVE, 0.0F);
    private static final Pattern FRAME_NAME = Pattern.compile("(.+)_(\\d+)");
    private static final Logger LOGGER = LogUtils.getLogger();
    /** How many frames each animation has: {@code name_0}, {@code name_1} and so on share the flipbook {@code flipbook/name.png}. */
    private static final Map<String, Integer> FLIPBOOKS = flipbooks();
    /** Each sprite's flipbook, or null for one with a texture of its own. */
    private static final String[] BOOK_NAMES = Arrays.stream(Sprite.values()).map(PedestalLight::bookName).toArray(String[]::new);
    private static final Flipbook[] BOOKS = new Flipbook[BOOK_NAMES.length];
    /** Where each sprite lies in its flipbook, as u0, v0, u1, v1; null for one with a texture of its own. */
    private static final float[][] FRAME_UVS = new float[BOOK_NAMES.length][];
    private static boolean booksRead, watchingReloads;
    private static final VfxMaterial[] SPRITES = Arrays.stream(Sprite.values())
            .map(sprite -> {
                String book = bookName(sprite);
                String path = book != null ? "flipbook/" + book : sprite.texture;
                VfxMaterial material = new VfxMaterial(TrophyPedestalsMod.id("textures/vfx/" + path + ".png"), sprite.blend, 0.0F).withSmoothing(sprite.smooth);
                return sprite.shader != null ? material.withShader(TrophyPedestalsMod.id(sprite.shader)) : material;
            })
            .toArray(VfxMaterial[]::new);
    private static final Vector3f CAMERA_RIGHT = new Vector3f(), CAMERA_UP = new Vector3f();
    /**
     * The trims and frost lying on a pedestal's own surfaces. They're drawn before everything floating in the air: the block
     * already hides what's behind them, so this only keeps them from painting over a fish, a frond or a crystal in front.
     * The Prism Lens's gems go after everything else, glows included, so that their trails of light stay behind them, and so does the Sanguine Lens's moon, over its eyes and ravens, with the eye that opens in it on top; its fog and the ravens on the near side go after all of that.
     */
    private static final int[] DRAW_STEP = new int[Sprite.values().length];

    static {
        for (Sprite sprite : Sprite.values()) {
            String name = sprite.name();
            boolean onPedestal = name.equals("ICICLES") || name.equals("COALS") || name.equals("END_BAND") || name.startsWith("FIRE_") || name.startsWith("HOLO_")
                    || name.startsWith("END_GLOW_") || name.startsWith("APPRAISAL_TRIM_") || name.startsWith("APPRAISAL_GLOW_") || name.startsWith("FROST_SIDE")
                    || name.startsWith("FROST_LIP") || name.startsWith("FROST_WELL") || name.startsWith("FROST_LEDGE") || name.startsWith("FROST_RIM");
            DRAW_STEP[sprite.ordinal()] = onPedestal ? -1 : name.startsWith("RAVEN_FRONT_") || name.equals("BLOOD_FOG") ? 2 : name.startsWith("PRISM_CRYSTAL") || name.startsWith("BLOOD_MOON_") || name.startsWith("BLOOD_BEAM_") || name.startsWith("BLOOD_CORONA_") || name.startsWith("BLOOD_GAZE_") || name.equals("BLOOD_FLASH") || name.equals("BLOOD_GLARE") ? 1 : 0;
        }
    }

    private static final int SPRITE = 17, KIND_FLAT = 0, KIND_BILLBOARD = 1, KIND_QUAD = 2;
    private static final int QUAD = 5;
    private static final int MAX_QUADS = 1 << 16;

    private static double[] quadCorners = new double[12 * 1024];
    private static float[] quads = new float[QUAD * 1024];
    private static int quadCount;
    private static double[] lensCenters = new double[3 * 64];
    private static float[] lenses = new float[2 * 64];
    private static int lensCount;
    private static final VfxMaterial AURA = new VfxMaterial(VfxMaterial.GLOW, VfxBlend.ADDITIVE, 0.0F);
    private static double[] auraCenters = new double[3 * 32];
    private static float[] auras = new float[2 * 32];
    private static int auraCount;
    private static double[] spriteCenters = new double[3 * 64];
    private static float[] sprites = new float[SPRITE * 64];
    private static int spriteCount;
    private static double[] flareCenters = new double[3 * 64];
    private static float[] flares = new float[3 * 64];
    private static int flareCount;
    private static VfxMaterial flareMaterial;
    // Which pedestal each shape belongs to, so that shapes can be drawn in order around the Ender rifts, which hide what's behind them.
    private static int[] quadOwners = new int[1024], lensOwners = new int[64], auraOwners = new int[32], spriteOwners = new int[64];
    private static double[] ownerCenters = new double[3 * 32];
    private static boolean[] ownerRifts = new boolean[32];
    private static int[] ownerLayers = new int[32];
    private static int ownerCount, currentOwner;
    private static double[] riftDistances = new double[8];
    private static int riftCount;

    private PedestalLight() {
    }

    private static Map<String, Integer> flipbooks() {
        Map<String, Integer> counts = new HashMap<>();
        for (Sprite sprite : Sprite.values()) {
            // the near ravens are the same frames again, drawn at another step
            if (sprite.name().startsWith("RAVEN_FRONT_")) continue;
            Matcher frame = FRAME_NAME.matcher(sprite.texture);
            if (frame.matches()) counts.merge(frame.group(1), 1, Integer::sum);
        }
        return counts;
    }

    private static @Nullable String bookName(Sprite sprite) {
        Matcher frame = FRAME_NAME.matcher(sprite.texture);
        return frame.matches() && FLIPBOOKS.getOrDefault(frame.group(1), 0) > 1 ? frame.group(1) : null;
    }

    /**
     * A flipbook as its {@code .png.mcmeta} describes it, in vanilla's animation format. The effects aren't on an atlas, so the
     * game won't play them itself, and many need their own timing: each raven beats its wings out of step with the others.
     *
     * @param ticks the frame shown on each tick of one loop
     */
    private record Flipbook(int columns, int rows, int cells, int[] ticks) {
        static Flipbook read(ResourceManager manager, String name, int frames) {
            ResourceLocation id = TrophyPedestalsMod.id("textures/vfx/flipbook/" + name + ".png");
            try {
                Resource resource = manager.getResourceOrThrow(id);
                int width, height;
                try (InputStream in = resource.open()) {
                    ByteBuffer header = ByteBuffer.wrap(in.readNBytes(24));
                    width = header.getInt(16);
                    height = header.getInt(20);
                }
                AnimationMetadataSection animation = resource.metadata().getSection(AnimationMetadataSection.SERIALIZER).orElse(null);
                if (animation == null) return strip(frames);
                FrameSize size = animation.calculateFrameSize(width, height);
                int columns = Math.max(1, width / size.width()), rows = Math.max(1, height / size.height()), shown = Math.min(frames, columns * rows);
                List<Integer> ticks = new ArrayList<>();
                animation.forEachFrame((index, time) -> {
                    if (index >= shown) return;
                    for (int tick = time; tick > 0; tick--) ticks.add(index);
                });
                if (ticks.isEmpty()) {
                    for (int frame = 0; frame < shown; frame++) {
                        for (int tick = animation.getDefaultFrameTime(); tick > 0; tick--) ticks.add(frame);
                    }
                }
                return new Flipbook(columns, rows, columns * rows, ticks.stream().mapToInt(Integer::intValue).toArray());
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("Couldn't read the flipbook {}", id, e);
                return strip(frames);
            }
        }

        private static Flipbook strip(int frames) {
            int[] ticks = new int[frames];
            Arrays.setAll(ticks, frame -> frame);
            return new Flipbook(1, frames, frames, ticks);
        }

        /** A frame's place: left to right, then top to bottom, as vanilla reads them. */
        float[] uvs(int frame, boolean smooth) {
            int index = frame % this.cells;
            // pulled in a touch from the frame's edges, so that its neighbours don't bleed in
            float inset = smooth ? 0.01F : 0.001F;
            return new float[]{(index % this.columns + inset) / this.columns, (index / this.columns + inset) / this.rows,
                    (index % this.columns + 1 - inset) / this.columns, (index / this.columns + 1 - inset) / this.rows};
        }
    }

    /** Reads the flipbooks the first time they're needed, and again after the resource packs change. */
    private static void readFlipbooks() {
        if (booksRead) return;
        booksRead = true;
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        if (!watchingReloads && manager instanceof ReloadableResourceManager reloadable) {
            watchingReloads = true;
            reloadable.registerReloadListener((ResourceManagerReloadListener) reloaded -> booksRead = false);
        }
        Map<String, Flipbook> books = new HashMap<>();
        for (Sprite sprite : Sprite.values()) {
            String name = BOOK_NAMES[sprite.ordinal()];
            if (name == null) continue;
            Flipbook book = books.computeIfAbsent(name, key -> Flipbook.read(manager, key, FLIPBOOKS.get(key)));
            Matcher frame = FRAME_NAME.matcher(sprite.texture);
            BOOKS[sprite.ordinal()] = book;
            FRAME_UVS[sprite.ordinal()] = book.uvs(frame.matches() ? Integer.parseInt(frame.group(2)) : 0, sprite.smooth);
        }
    }

    /** Draws a sprite's frame out of its flipbook, in the same corners as the drawer's own square shapes would take. */
    private static void drawFrame(VfxDraw draw, int kind, double x, double y, double z, int at, float[] uv, int color) {
        Vec3 a, b, c, d;
        if (kind == KIND_FLAT || kind == KIND_BILLBOARD) {
            float half = sprites[at + 2];
            if (half <= 0.0F) return;
            float cos = Mth.cos(sprites[at + 3]) * half, sin = Mth.sin(sprites[at + 3]) * half;
            if (kind == KIND_FLAT) {
                a = new Vec3(x - cos - sin, y, z + sin - cos);
                b = new Vec3(x - cos + sin, y, z + sin + cos);
                c = new Vec3(x + cos + sin, y, z - sin + cos);
                d = new Vec3(x + cos - sin, y, z - sin - cos);
            } else {
                Vector3f right = CAMERA_RIGHT, up = CAMERA_UP;
                double rx = right.x * cos + up.x * sin, ry = right.y * cos + up.y * sin, rz = right.z * cos + up.z * sin;
                double ux = up.x * cos - right.x * sin, uy = up.y * cos - right.y * sin, uz = up.z * cos - right.z * sin;
                a = new Vec3(x - rx + ux, y - ry + uy, z - rz + uz);
                b = new Vec3(x - rx - ux, y - ry - uy, z - rz - uz);
                c = new Vec3(x + rx - ux, y + ry - uy, z + rz - uz);
                d = new Vec3(x + rx + ux, y + ry + uy, z + rz + uz);
            }
        } else {
            a = new Vec3(x + sprites[at + 5], y + sprites[at + 6], z + sprites[at + 7]);
            b = new Vec3(x + sprites[at + 8], y + sprites[at + 9], z + sprites[at + 10]);
            c = new Vec3(x + sprites[at + 11], y + sprites[at + 12], z + sprites[at + 13]);
            d = new Vec3(x + sprites[at + 14], y + sprites[at + 15], z + sprites[at + 16]);
        }
        draw.quad(a, b, c, d, uv[0], uv[1], uv[2], uv[3], color);
    }

    public static void init(ResourceLocation flareTexture) {
        flareMaterial = new VfxMaterial(flareTexture, VfxBlend.ALPHA, 0.0F);
        WorldVfx.register(TrophyPedestalsMod.id("pedestal_light"), PedestalLight::render);
    }

    private static void render(VfxFrame frame) {
        readFlipbooks();
        Vec3 camera = frame.cameraPosition();
        orderAroundRifts(camera);
        if (quadCount > 0) {
            VfxDraw draw = null;
            int drawn = -1;
            for (int i = 0; i < quadCount; i++) {
                int layer = ownerLayers[quadOwners[i]];
                if (draw == null || layer != drawn) {
                    draw = frame.draw(FADE, layerAt(layer));
                    drawn = layer;
                }
                int at = i * QUAD, c = i * 12;
                double[] p = quadCorners;
                draw.quad(new Vec3(p[c], p[c + 1], p[c + 2]), new Vec3(p[c + 3], p[c + 4], p[c + 5]),
                        new Vec3(p[c + 6], p[c + 7], p[c + 8]), new Vec3(p[c + 9], p[c + 10], p[c + 11]),
                        quads[at + 1], quads[at + 3], quads[at + 2], quads[at + 4], Float.floatToRawIntBits(quads[at]));
            }
        }
        if (lensCount > 0) {
            VfxDraw draw = null;
            int drawn = -1;
            for (int i = 0; i < lensCount; i++) {
                int layer = ownerLayers[lensOwners[i]];
                if (draw == null || layer != drawn) {
                    draw = frame.draw(LENS, layerAt(layer));
                    drawn = layer;
                }
                int at = i * 2, c = i * 3;
                draw.flat(lensCenters[c], lensCenters[c + 1], lensCenters[c + 2], lenses[at], 0.0F, Float.floatToRawIntBits(lenses[at + 1]));
            }
        }
        if (auraCount > 0) {
            VfxDraw draw = null;
            int drawn = -1;
            for (int i = 0; i < auraCount; i++) {
                int layer = ownerLayers[auraOwners[i]];
                if (draw == null || layer != drawn) {
                    draw = frame.draw(AURA, layerAt(layer));
                    drawn = layer;
                }
                int at = i * 2, c = i * 3;
                draw.billboard(auraCenters[c], auraCenters[c + 1], auraCenters[c + 2], auras[at], 0.0F, Float.floatToRawIntBits(auras[at + 1]));
            }
        }
        if (spriteCount > 0) {
            Quaternionf rotation = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
            rotation.transform(1.0F, 0.0F, 0.0F, CAMERA_RIGHT);
            rotation.transform(0.0F, 1.0F, 0.0F, CAMERA_UP);
        }
        for (int texture = 0; texture < SPRITES.length && spriteCount > 0; texture++) {
            VfxDraw draw = null;
            int drawn = -1;
            for (int i = 0; i < spriteCount; i++) {
                int at = i * SPRITE, c = i * 3;
                if (sprites[at + 1] != texture) continue;
                int layer = ownerLayers[spriteOwners[i]];
                if (draw == null || layer != drawn) {
                    draw = frame.draw(SPRITES[texture], layerAt(layer, DRAW_STEP[texture]));
                    drawn = layer;
                }
                double x = spriteCenters[c], y = spriteCenters[c + 1], z = spriteCenters[c + 2];
                int color = Float.floatToRawIntBits(sprites[at + 4]);
                if (FRAME_UVS[texture] != null) {
                    drawFrame(draw, (int) sprites[at], x, y, z, at, FRAME_UVS[texture], color);
                    continue;
                }
                switch ((int) sprites[at]) {
                    case KIND_FLAT -> draw.flat(x, y, z, sprites[at + 2], sprites[at + 3], color);
                    case KIND_BILLBOARD -> draw.billboard(x, y, z, sprites[at + 2], sprites[at + 3], color);
                    default -> draw.quad(new Vec3(x + sprites[at + 5], y + sprites[at + 6], z + sprites[at + 7]),
                            new Vec3(x + sprites[at + 8], y + sprites[at + 9], z + sprites[at + 10]),
                            new Vec3(x + sprites[at + 11], y + sprites[at + 12], z + sprites[at + 13]),
                            new Vec3(x + sprites[at + 14], y + sprites[at + 15], z + sprites[at + 16]), color);
                }
            }
        }
        if (flareCount > 0) {
            VfxDraw draw = null;
            int drawn = -1;
            for (int i = 0; i < flareCount; i++) {
                int at = i * 3, c = i * 3;
                int layer = 2 * riftsBeyond(distance(camera, flareCenters[c], flareCenters[c + 1], flareCenters[c + 2]));
                if (draw == null || layer != drawn) {
                    draw = frame.draw(flareMaterial, layerAt(layer));
                    drawn = layer;
                }
                draw.billboard(flareCenters[c], flareCenters[c + 1], flareCenters[c + 2], flares[at], flares[at + 1], Float.floatToRawIntBits(flares[at + 2]));
            }
        }
        quadCount = lensCount = flareCount = spriteCount = auraCount = ownerCount = 0;
    }

    /**
     * World effects write no depth, so a rift only hides what's behind it if that is drawn first. Each pedestal gets a place
     * in the order by how many rifts lie beyond it: the effects of farther pedestals, then a rift and its own effects, then
     * whatever is nearer. With no rift in the frame everything stays in one layer, batched as usual.
     */
    private static void orderAroundRifts(Vec3 camera) {
        riftCount = 0;
        if (ownerLayers.length < ownerCount) ownerLayers = new int[ownerCenters.length / 3];
        for (int owner = 0; owner < ownerCount; owner++) {
            if (!ownerRifts[owner]) continue;
            if (riftCount == riftDistances.length) riftDistances = Arrays.copyOf(riftDistances, riftCount * 2);
            riftDistances[riftCount++] = ownerDistance(camera, owner);
        }
        Arrays.sort(riftDistances, 0, riftCount);
        for (int owner = 0; owner < ownerCount; owner++) {
            ownerLayers[owner] = riftCount == 0 ? 0 : 2 * riftsBeyond(ownerDistance(camera, owner)) + (ownerRifts[owner] ? 1 : 0);
        }
    }

    private static double ownerDistance(Vec3 camera, int owner) {
        int c = owner * 3;
        return distance(camera, ownerCenters[c], ownerCenters[c + 1], ownerCenters[c + 2]);
    }

    private static double distance(Vec3 camera, double x, double y, double z) {
        return camera.distanceToSqr(x, y, z);
    }

    /** How many of this frame's rifts are farther from the camera than {@code distance}. */
    private static int riftsBeyond(double distance) {
        int low = 0, high = riftCount;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (riftDistances[middle] > distance) high = middle;
            else low = middle + 1;
        }
        return riftCount - low;
    }

    /** Below the default layer, so the particles drawn there stay on top as they always have. */
    private static VfxLayer layerAt(int place) {
        return layerAt(place, 0);
    }

    /** As above, with {@code step} -1 for what's drawn just before the rest of its place in the order, and 1 and 2 for just after. */
    private static VfxLayer layerAt(int place, int step) {
        int order = (riftCount == 0 ? 0 : place - (2 * riftCount + 1)) * 4 + step;
        return order == 0 ? VfxLayer.DEFAULT : VfxLayer.of(order);
    }

    private static int owner(double x, double y, double z) {
        if (ownerCount > 0) {
            int last = (ownerCount - 1) * 3;
            if (ownerCenters[last] == x && ownerCenters[last + 1] == y && ownerCenters[last + 2] == z) return ownerCount - 1;
        }
        if ((ownerCount + 1) * 3 > ownerCenters.length) {
            ownerCenters = Arrays.copyOf(ownerCenters, ownerCenters.length * 2);
            ownerRifts = Arrays.copyOf(ownerRifts, ownerRifts.length * 2);
        }
        int c = ownerCount * 3;
        ownerCenters[c] = x;
        ownerCenters[c + 1] = y;
        ownerCenters[c + 2] = z;
        ownerRifts[ownerCount] = false;
        return ownerCount++;
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

    private static final String END_SHADER = "vfx/end_rift";
    private static final String ITEM_SHEEN = "vfx/item_sheen";

    /** The textures the lenses draw around a pedestal's item: glowing (added to what's behind) or painted over it. */
    public enum Sprite {
        RIPPLE("ripple", VfxBlend.ADDITIVE),
        RUNES("runes", VfxBlend.ADDITIVE),
        GRID("grid", VfxBlend.ADDITIVE),
        HOLO_RING("holo_ring", VfxBlend.ADDITIVE),
        HOLO_BAND("holo_band", VfxBlend.ADDITIVE),
        FROST("blizzard", VfxBlend.ADDITIVE),
        MIST("halo", VfxBlend.ADDITIVE),
        FLAME("flame", VfxBlend.ADDITIVE),
        COLD_FALL_0("cold_fall_0", VfxBlend.ALPHA, true),
        COLD_FALL_1("cold_fall_1", VfxBlend.ALPHA, true),
        COLD_FALL_2("cold_fall_2", VfxBlend.ALPHA, true),
        COLD_FALL_3("cold_fall_3", VfxBlend.ALPHA, true),
        COLD_FALL_4("cold_fall_4", VfxBlend.ALPHA, true),
        COLD_FALL_5("cold_fall_5", VfxBlend.ALPHA, true),
        COLD_FALL_6("cold_fall_6", VfxBlend.ALPHA, true),
        COLD_FALL_7("cold_fall_7", VfxBlend.ALPHA, true),
        COLD_FALL_8("cold_fall_8", VfxBlend.ALPHA, true),
        COLD_FALL_9("cold_fall_9", VfxBlend.ALPHA, true),
        COLD_FALL_10("cold_fall_10", VfxBlend.ALPHA, true),
        COLD_FALL_11("cold_fall_11", VfxBlend.ALPHA, true),
        COLD_FALL_12("cold_fall_12", VfxBlend.ALPHA, true),
        COLD_FALL_13("cold_fall_13", VfxBlend.ALPHA, true),
        COLD_FALL_14("cold_fall_14", VfxBlend.ALPHA, true),
        COLD_FALL_15("cold_fall_15", VfxBlend.ALPHA, true),
        FROST_SIDE("blizzard_side", VfxBlend.ALPHA),
        FROST_LIP("blizzard_lip", VfxBlend.ALPHA),
        FROST_WELL("blizzard_well", VfxBlend.ALPHA),
        FROST_LEDGE("blizzard_ledge", VfxBlend.ALPHA),
        FROST_RIM("blizzard_rim", VfxBlend.ALPHA),
        ICICLES("icicles", VfxBlend.ALPHA),
        END_RIFT_0("end_rift_0", VfxBlend.ALPHA, false, END_SHADER),
        END_RIFT_1("end_rift_1", VfxBlend.ALPHA, false, END_SHADER),
        END_RIFT_2("end_rift_2", VfxBlend.ALPHA, false, END_SHADER),
        END_RIFT_3("end_rift_3", VfxBlend.ALPHA, false, END_SHADER),
        END_RIFT_4("end_rift_4", VfxBlend.ALPHA, false, END_SHADER),
        END_RIFT_5("end_rift_5", VfxBlend.ALPHA, false, END_SHADER),
        END_RIFT_6("end_rift_6", VfxBlend.ALPHA, false, END_SHADER),
        END_RIFT_7("end_rift_7", VfxBlend.ALPHA, false, END_SHADER),
        END_RIFT_8("end_rift_8", VfxBlend.ALPHA, false, END_SHADER),
        END_RIFT_9("end_rift_9", VfxBlend.ALPHA, false, END_SHADER),
        END_RIFT_10("end_rift_10", VfxBlend.ALPHA, false, END_SHADER),
        END_RIFT_11("end_rift_11", VfxBlend.ALPHA, false, END_SHADER),
        END_RIFT_12("end_rift_12", VfxBlend.ALPHA, false, END_SHADER),
        END_RIFT_13("end_rift_13", VfxBlend.ALPHA, false, END_SHADER),
        END_RIFT_14("end_rift_14", VfxBlend.ALPHA, false, END_SHADER),
        END_RIFT_15("end_rift_15", VfxBlend.ALPHA, false, END_SHADER),
        END_RIFT_LENS("end_rift_lens", VfxBlend.ALPHA, false, END_SHADER),
        END_GATE_0("end_gate_0", VfxBlend.ALPHA),
        END_GATE_1("end_gate_1", VfxBlend.ALPHA),
        END_GATE_2("end_gate_2", VfxBlend.ALPHA),
        END_GATE_3("end_gate_3", VfxBlend.ALPHA),
        END_GATE_4("end_gate_4", VfxBlend.ALPHA),
        END_GATE_5("end_gate_5", VfxBlend.ALPHA),
        END_GATE_6("end_gate_6", VfxBlend.ALPHA),
        END_GATE_7("end_gate_7", VfxBlend.ALPHA),
        END_GATE_8("end_gate_8", VfxBlend.ALPHA),
        END_GATE_9("end_gate_9", VfxBlend.ALPHA),
        END_GATE_10("end_gate_10", VfxBlend.ALPHA),
        END_GATE_11("end_gate_11", VfxBlend.ALPHA),
        END_GATE_12("end_gate_12", VfxBlend.ALPHA),
        END_GATE_13("end_gate_13", VfxBlend.ALPHA),
        END_GATE_14("end_gate_14", VfxBlend.ALPHA),
        END_GATE_15("end_gate_15", VfxBlend.ALPHA),
        END_LENS_0("end_lens_0", VfxBlend.ALPHA),
        END_LENS_1("end_lens_1", VfxBlend.ALPHA),
        END_LENS_2("end_lens_2", VfxBlend.ALPHA),
        END_LENS_3("end_lens_3", VfxBlend.ALPHA),
        END_LENS_4("end_lens_4", VfxBlend.ALPHA),
        END_LENS_5("end_lens_5", VfxBlend.ALPHA),
        END_LENS_6("end_lens_6", VfxBlend.ALPHA),
        END_LENS_7("end_lens_7", VfxBlend.ALPHA),
        END_LENS_8("end_lens_8", VfxBlend.ALPHA),
        END_LENS_9("end_lens_9", VfxBlend.ALPHA),
        END_LENS_10("end_lens_10", VfxBlend.ALPHA),
        END_LENS_11("end_lens_11", VfxBlend.ALPHA),
        END_LENS_12("end_lens_12", VfxBlend.ALPHA),
        END_LENS_13("end_lens_13", VfxBlend.ALPHA),
        END_LENS_14("end_lens_14", VfxBlend.ALPHA),
        END_LENS_15("end_lens_15", VfxBlend.ALPHA),
        END_BLOOM("curators_bloom", VfxBlend.ADDITIVE, true),
        END_GLARE("curators_glare", VfxBlend.ADDITIVE, true),
        END_ISLAND_0("end_island_0", VfxBlend.ALPHA),
        END_ISLAND_1("end_island_1", VfxBlend.ALPHA),
        END_ISLAND_2("end_island_2", VfxBlend.ALPHA),
        END_BEAM_0("end_beam_0", VfxBlend.ADDITIVE),
        END_BEAM_1("end_beam_1", VfxBlend.ADDITIVE),
        END_BEAM_2("end_beam_2", VfxBlend.ADDITIVE),
        END_BEAM_3("end_beam_3", VfxBlend.ADDITIVE),
        END_BEAM_4("end_beam_4", VfxBlend.ADDITIVE),
        END_BEAM_5("end_beam_5", VfxBlend.ADDITIVE),
        END_BEAM_6("end_beam_6", VfxBlend.ADDITIVE),
        END_BEAM_7("end_beam_7", VfxBlend.ADDITIVE),
        SUNBURST("sunburst", VfxBlend.ADDITIVE),
        SOCKET_GLOW("socket_glow", VfxBlend.ADDITIVE),
        LENS_PRISM_0("lens_prism_0", VfxBlend.ADDITIVE),
        LENS_PRISM_1("lens_prism_1", VfxBlend.ADDITIVE),
        LENS_PRISM_2("lens_prism_2", VfxBlend.ADDITIVE),
        LENS_PRISM_3("lens_prism_3", VfxBlend.ADDITIVE),
        LENS_PRISM_4("lens_prism_4", VfxBlend.ADDITIVE),
        LENS_PRISM_5("lens_prism_5", VfxBlend.ADDITIVE),
        LENS_PRISM_6("lens_prism_6", VfxBlend.ADDITIVE),
        LENS_PRISM_7("lens_prism_7", VfxBlend.ADDITIVE),
        LENS_PRISM_8("lens_prism_8", VfxBlend.ADDITIVE),
        LENS_PRISM_9("lens_prism_9", VfxBlend.ADDITIVE),
        LENS_PRISM_10("lens_prism_10", VfxBlend.ADDITIVE),
        LENS_PRISM_11("lens_prism_11", VfxBlend.ADDITIVE),
        LENS_PRISM_12("lens_prism_12", VfxBlend.ADDITIVE),
        LENS_PRISM_13("lens_prism_13", VfxBlend.ADDITIVE),
        LENS_PRISM_14("lens_prism_14", VfxBlend.ADDITIVE),
        LENS_PRISM_15("lens_prism_15", VfxBlend.ADDITIVE),
        LENS_EYE_0("lens_eye_0", VfxBlend.ADDITIVE),
        LENS_EYE_1("lens_eye_1", VfxBlend.ADDITIVE),
        LENS_EYE_2("lens_eye_2", VfxBlend.ADDITIVE),
        LENS_EYE_3("lens_eye_3", VfxBlend.ADDITIVE),
        LENS_EYE_4("lens_eye_4", VfxBlend.ADDITIVE),
        LENS_EYE_5("lens_eye_5", VfxBlend.ADDITIVE),
        LENS_EYE_6("lens_eye_6", VfxBlend.ADDITIVE),
        LENS_EYE_7("lens_eye_7", VfxBlend.ADDITIVE),
        LENS_EYE_8("lens_eye_8", VfxBlend.ADDITIVE),
        LENS_EYE_9("lens_eye_9", VfxBlend.ADDITIVE),
        LENS_EYE_10("lens_eye_10", VfxBlend.ADDITIVE),
        LENS_EYE_11("lens_eye_11", VfxBlend.ADDITIVE),
        LENS_EYE_12("lens_eye_12", VfxBlend.ADDITIVE),
        LENS_EYE_13("lens_eye_13", VfxBlend.ADDITIVE),
        LENS_EYE_14("lens_eye_14", VfxBlend.ADDITIVE),
        LENS_EYE_15("lens_eye_15", VfxBlend.ADDITIVE),
        LENS_FACETS_0("lens_facets_0", VfxBlend.ADDITIVE),
        LENS_FACETS_1("lens_facets_1", VfxBlend.ADDITIVE),
        LENS_FACETS_2("lens_facets_2", VfxBlend.ADDITIVE),
        LENS_FACETS_3("lens_facets_3", VfxBlend.ADDITIVE),
        LENS_FACETS_4("lens_facets_4", VfxBlend.ADDITIVE),
        LENS_FACETS_5("lens_facets_5", VfxBlend.ADDITIVE),
        LENS_FACETS_6("lens_facets_6", VfxBlend.ADDITIVE),
        LENS_FACETS_7("lens_facets_7", VfxBlend.ADDITIVE),
        LENS_FACETS_8("lens_facets_8", VfxBlend.ADDITIVE),
        LENS_FACETS_9("lens_facets_9", VfxBlend.ADDITIVE),
        LENS_FACETS_10("lens_facets_10", VfxBlend.ADDITIVE),
        LENS_FACETS_11("lens_facets_11", VfxBlend.ADDITIVE),
        LENS_FACETS_12("lens_facets_12", VfxBlend.ADDITIVE),
        LENS_FACETS_13("lens_facets_13", VfxBlend.ADDITIVE),
        LENS_FACETS_14("lens_facets_14", VfxBlend.ADDITIVE),
        LENS_FACETS_15("lens_facets_15", VfxBlend.ADDITIVE),
        LENS_FROST_0("lens_blizzard_0", VfxBlend.ADDITIVE),
        LENS_FROST_1("lens_blizzard_1", VfxBlend.ADDITIVE),
        LENS_FROST_2("lens_blizzard_2", VfxBlend.ADDITIVE),
        LENS_FROST_3("lens_blizzard_3", VfxBlend.ADDITIVE),
        LENS_FROST_4("lens_blizzard_4", VfxBlend.ADDITIVE),
        LENS_FROST_5("lens_blizzard_5", VfxBlend.ADDITIVE),
        LENS_FROST_6("lens_blizzard_6", VfxBlend.ADDITIVE),
        LENS_FROST_7("lens_blizzard_7", VfxBlend.ADDITIVE),
        LENS_FROST_8("lens_blizzard_8", VfxBlend.ADDITIVE),
        LENS_FROST_9("lens_blizzard_9", VfxBlend.ADDITIVE),
        LENS_FROST_10("lens_blizzard_10", VfxBlend.ADDITIVE),
        LENS_FROST_11("lens_blizzard_11", VfxBlend.ADDITIVE),
        LENS_FROST_12("lens_blizzard_12", VfxBlend.ADDITIVE),
        LENS_FROST_13("lens_blizzard_13", VfxBlend.ADDITIVE),
        LENS_FROST_14("lens_blizzard_14", VfxBlend.ADDITIVE),
        LENS_FROST_15("lens_blizzard_15", VfxBlend.ADDITIVE),
        FROST_CRYSTAL_0("blizzard_crystal_0", VfxBlend.ALPHA),
        FROST_CRYSTAL_1("blizzard_crystal_1", VfxBlend.ALPHA),
        FROST_CRYSTAL_2("blizzard_crystal_2", VfxBlend.ALPHA),
        APPRAISAL_CROWN_0("curators_crown_0", VfxBlend.ALPHA),
        APPRAISAL_CROWN_1("curators_crown_1", VfxBlend.ALPHA),
        APPRAISAL_CROWN_2("curators_crown_2", VfxBlend.ALPHA),
        APPRAISAL_CROWN_3("curators_crown_3", VfxBlend.ALPHA),
        APPRAISAL_CROWN_4("curators_crown_4", VfxBlend.ALPHA),
        APPRAISAL_CROWN_5("curators_crown_5", VfxBlend.ALPHA),
        APPRAISAL_CROWN_6("curators_crown_6", VfxBlend.ALPHA),
        APPRAISAL_CROWN_7("curators_crown_7", VfxBlend.ALPHA),
        APPRAISAL_CROWN_8("curators_crown_8", VfxBlend.ALPHA),
        APPRAISAL_CROWN_9("curators_crown_9", VfxBlend.ALPHA),
        APPRAISAL_CROWN_10("curators_crown_10", VfxBlend.ALPHA),
        APPRAISAL_CROWN_11("curators_crown_11", VfxBlend.ALPHA),
        APPRAISAL_CROWN_12("curators_crown_12", VfxBlend.ALPHA),
        APPRAISAL_CROWN_13("curators_crown_13", VfxBlend.ALPHA),
        APPRAISAL_CROWN_14("curators_crown_14", VfxBlend.ALPHA),
        APPRAISAL_CROWN_15("curators_crown_15", VfxBlend.ALPHA),
        BLOOD_POOL_0("blood_pool_0", VfxBlend.ALPHA),
        BLOOD_POOL_1("blood_pool_1", VfxBlend.ALPHA),
        BLOOD_POOL_2("blood_pool_2", VfxBlend.ALPHA),
        BLOOD_POOL_3("blood_pool_3", VfxBlend.ALPHA),
        BLOOD_POOL_4("blood_pool_4", VfxBlend.ALPHA),
        BLOOD_POOL_5("blood_pool_5", VfxBlend.ALPHA),
        BLOOD_POOL_6("blood_pool_6", VfxBlend.ALPHA),
        BLOOD_POOL_7("blood_pool_7", VfxBlend.ALPHA),
        BLOOD_POOL_8("blood_pool_8", VfxBlend.ALPHA),
        BLOOD_POOL_9("blood_pool_9", VfxBlend.ALPHA),
        BLOOD_POOL_10("blood_pool_10", VfxBlend.ALPHA),
        BLOOD_POOL_11("blood_pool_11", VfxBlend.ALPHA),
        BLOOD_POOL_12("blood_pool_12", VfxBlend.ALPHA),
        BLOOD_POOL_13("blood_pool_13", VfxBlend.ALPHA),
        BLOOD_POOL_14("blood_pool_14", VfxBlend.ALPHA),
        BLOOD_POOL_15("blood_pool_15", VfxBlend.ALPHA),
        BLOOD_FOG("blood_fog", VfxBlend.ALPHA, true),
        BLOOD_BEAM_0("blood_beam_0", VfxBlend.ALPHA),
        BLOOD_BEAM_1("blood_beam_1", VfxBlend.ALPHA),
        BLOOD_BEAM_2("blood_beam_2", VfxBlend.ALPHA),
        BLOOD_BEAM_3("blood_beam_3", VfxBlend.ALPHA),
        BLOOD_BEAM_4("blood_beam_4", VfxBlend.ALPHA),
        BLOOD_BEAM_5("blood_beam_5", VfxBlend.ALPHA),
        BLOOD_BEAM_6("blood_beam_6", VfxBlend.ALPHA),
        BLOOD_BEAM_7("blood_beam_7", VfxBlend.ALPHA),
        BLOOD_BEAM_8("blood_beam_8", VfxBlend.ALPHA),
        BLOOD_BEAM_9("blood_beam_9", VfxBlend.ALPHA),
        BLOOD_BEAM_10("blood_beam_10", VfxBlend.ALPHA),
        BLOOD_BEAM_11("blood_beam_11", VfxBlend.ALPHA),
        BLOOD_BEAM_12("blood_beam_12", VfxBlend.ALPHA),
        BLOOD_BEAM_13("blood_beam_13", VfxBlend.ALPHA),
        BLOOD_BEAM_14("blood_beam_14", VfxBlend.ALPHA),
        BLOOD_BEAM_15("blood_beam_15", VfxBlend.ALPHA),
        BLOOD_MOON_0("blood_moon_0", VfxBlend.ALPHA),
        BLOOD_MOON_1("blood_moon_1", VfxBlend.ALPHA),
        BLOOD_MOON_2("blood_moon_2", VfxBlend.ALPHA),
        BLOOD_MOON_3("blood_moon_3", VfxBlend.ALPHA),
        BLOOD_MOON_4("blood_moon_4", VfxBlend.ALPHA),
        BLOOD_MOON_5("blood_moon_5", VfxBlend.ALPHA),
        BLOOD_MOON_6("blood_moon_6", VfxBlend.ALPHA),
        BLOOD_MOON_7("blood_moon_7", VfxBlend.ALPHA),
        BLOOD_MOON_8("blood_moon_8", VfxBlend.ALPHA),
        BLOOD_MOON_9("blood_moon_9", VfxBlend.ALPHA),
        BLOOD_MOON_10("blood_moon_10", VfxBlend.ALPHA),
        BLOOD_MOON_11("blood_moon_11", VfxBlend.ALPHA),
        BLOOD_MOON_12("blood_moon_12", VfxBlend.ALPHA),
        BLOOD_MOON_13("blood_moon_13", VfxBlend.ALPHA),
        BLOOD_MOON_14("blood_moon_14", VfxBlend.ALPHA),
        BLOOD_MOON_15("blood_moon_15", VfxBlend.ALPHA),
        BLOOD_CORONA_0("blood_corona_0", VfxBlend.ADDITIVE),
        BLOOD_CORONA_1("blood_corona_1", VfxBlend.ADDITIVE),
        BLOOD_CORONA_2("blood_corona_2", VfxBlend.ADDITIVE),
        BLOOD_CORONA_3("blood_corona_3", VfxBlend.ADDITIVE),
        BLOOD_CORONA_4("blood_corona_4", VfxBlend.ADDITIVE),
        BLOOD_CORONA_5("blood_corona_5", VfxBlend.ADDITIVE),
        BLOOD_CORONA_6("blood_corona_6", VfxBlend.ADDITIVE),
        BLOOD_CORONA_7("blood_corona_7", VfxBlend.ADDITIVE),
        BLOOD_CORONA_8("blood_corona_8", VfxBlend.ADDITIVE),
        BLOOD_CORONA_9("blood_corona_9", VfxBlend.ADDITIVE),
        BLOOD_CORONA_10("blood_corona_10", VfxBlend.ADDITIVE),
        BLOOD_CORONA_11("blood_corona_11", VfxBlend.ADDITIVE),
        BLOOD_GLARE("curators_glare", VfxBlend.ADDITIVE, true),
        BLOOD_FLASH("curators_bloom", VfxBlend.ADDITIVE, true),
        BLOOD_GAZE_0("blood_gaze_0", VfxBlend.ALPHA),
        BLOOD_GAZE_1("blood_gaze_1", VfxBlend.ALPHA),
        BLOOD_GAZE_2("blood_gaze_2", VfxBlend.ALPHA),
        BLOOD_GAZE_3("blood_gaze_3", VfxBlend.ALPHA),
        BLOOD_GAZE_4("blood_gaze_4", VfxBlend.ALPHA),
        BLOOD_GAZE_5("blood_gaze_5", VfxBlend.ALPHA),
        BLOOD_GAZE_6("blood_gaze_6", VfxBlend.ALPHA),
        BLOOD_GAZE_7("blood_gaze_7", VfxBlend.ALPHA),
        BLOOD_GAZE_8("blood_gaze_8", VfxBlend.ALPHA),
        BLOOD_GAZE_9("blood_gaze_9", VfxBlend.ALPHA),
        BLOOD_GAZE_10("blood_gaze_10", VfxBlend.ALPHA),
        BLOOD_GAZE_11("blood_gaze_11", VfxBlend.ALPHA),
        BLOOD_EYES_0("blood_eyes_0", VfxBlend.ALPHA),
        BLOOD_EYES_1("blood_eyes_1", VfxBlend.ALPHA),
        BLOOD_EYES_2("blood_eyes_2", VfxBlend.ALPHA),
        BLOOD_EYES_3("blood_eyes_3", VfxBlend.ALPHA),
        BLOOD_EYES_4("blood_eyes_4", VfxBlend.ALPHA),
        BLOOD_EYES_5("blood_eyes_5", VfxBlend.ALPHA),
        BLOOD_EYES_6("blood_eyes_6", VfxBlend.ALPHA),
        BLOOD_EYES_7("blood_eyes_7", VfxBlend.ALPHA),
        RAVEN_0("raven_0", VfxBlend.ALPHA),
        RAVEN_1("raven_1", VfxBlend.ALPHA),
        RAVEN_2("raven_2", VfxBlend.ALPHA),
        RAVEN_3("raven_3", VfxBlend.ALPHA),
        RAVEN_4("raven_4", VfxBlend.ALPHA),
        RAVEN_5("raven_5", VfxBlend.ALPHA),
        RAVEN_FRONT_0("raven_0", VfxBlend.ALPHA),
        RAVEN_FRONT_1("raven_1", VfxBlend.ALPHA),
        RAVEN_FRONT_2("raven_2", VfxBlend.ALPHA),
        RAVEN_FRONT_3("raven_3", VfxBlend.ALPHA),
        RAVEN_FRONT_4("raven_4", VfxBlend.ALPHA),
        RAVEN_FRONT_5("raven_5", VfxBlend.ALPHA),
        LENS_CLEAR_ARC("lens_clear_arc", VfxBlend.ADDITIVE, true),
        LENS_CAUSTIC_0("lens_caustic_0", VfxBlend.ADDITIVE),
        LENS_CAUSTIC_1("lens_caustic_1", VfxBlend.ADDITIVE),
        LENS_CAUSTIC_2("lens_caustic_2", VfxBlend.ADDITIVE),
        LENS_CAUSTIC_3("lens_caustic_3", VfxBlend.ADDITIVE),
        LENS_CAUSTIC_4("lens_caustic_4", VfxBlend.ADDITIVE),
        LENS_CAUSTIC_5("lens_caustic_5", VfxBlend.ADDITIVE),
        LENS_CAUSTIC_6("lens_caustic_6", VfxBlend.ADDITIVE),
        LENS_CAUSTIC_7("lens_caustic_7", VfxBlend.ADDITIVE),
        LENS_CAUSTIC_8("lens_caustic_8", VfxBlend.ADDITIVE),
        LENS_CAUSTIC_9("lens_caustic_9", VfxBlend.ADDITIVE),
        LENS_CAUSTIC_10("lens_caustic_10", VfxBlend.ADDITIVE),
        LENS_CAUSTIC_11("lens_caustic_11", VfxBlend.ADDITIVE),
        LENS_CAUSTIC_12("lens_caustic_12", VfxBlend.ADDITIVE),
        LENS_CAUSTIC_13("lens_caustic_13", VfxBlend.ADDITIVE),
        LENS_CAUSTIC_14("lens_caustic_14", VfxBlend.ADDITIVE),
        LENS_CAUSTIC_15("lens_caustic_15", VfxBlend.ADDITIVE),
        TIDE_KELP_0("coral_reef_kelp_0", VfxBlend.ALPHA),
        TIDE_KELP_1("coral_reef_kelp_1", VfxBlend.ALPHA),
        TIDE_KELP_2("coral_reef_kelp_2", VfxBlend.ALPHA),
        TIDE_KELP_3("coral_reef_kelp_3", VfxBlend.ALPHA),
        TIDE_KELP_4("coral_reef_kelp_4", VfxBlend.ALPHA),
        TIDE_KELP_5("coral_reef_kelp_5", VfxBlend.ALPHA),
        TIDE_KELP_6("coral_reef_kelp_6", VfxBlend.ALPHA),
        TIDE_KELP_7("coral_reef_kelp_7", VfxBlend.ALPHA),
        TIDE_FISH_0("coral_reef_fish_0", VfxBlend.ALPHA),
        TIDE_FISH_1("coral_reef_fish_1", VfxBlend.ALPHA),
        TIDE_FISH_2("coral_reef_fish_2", VfxBlend.ALPHA),
        TIDE_FISH_3("coral_reef_fish_3", VfxBlend.ALPHA),
        PRISM_HALO_0("prism_halo_0", VfxBlend.ALPHA),
        PRISM_HALO_1("prism_halo_1", VfxBlend.ALPHA),
        PRISM_HALO_2("prism_halo_2", VfxBlend.ALPHA),
        PRISM_HALO_3("prism_halo_3", VfxBlend.ALPHA),
        PRISM_HALO_4("prism_halo_4", VfxBlend.ALPHA),
        PRISM_HALO_5("prism_halo_5", VfxBlend.ALPHA),
        PRISM_HALO_6("prism_halo_6", VfxBlend.ALPHA),
        PRISM_HALO_7("prism_halo_7", VfxBlend.ALPHA),
        PRISM_HALO_8("prism_halo_8", VfxBlend.ALPHA),
        PRISM_HALO_9("prism_halo_9", VfxBlend.ALPHA),
        PRISM_HALO_10("prism_halo_10", VfxBlend.ALPHA),
        PRISM_HALO_11("prism_halo_11", VfxBlend.ALPHA),
        PRISM_HALO_12("prism_halo_12", VfxBlend.ALPHA),
        PRISM_HALO_13("prism_halo_13", VfxBlend.ALPHA),
        PRISM_HALO_14("prism_halo_14", VfxBlend.ALPHA),
        PRISM_HALO_15("prism_halo_15", VfxBlend.ALPHA),
        PRISM_HALO_16("prism_halo_16", VfxBlend.ALPHA),
        PRISM_HALO_17("prism_halo_17", VfxBlend.ALPHA),
        PRISM_HALO_18("prism_halo_18", VfxBlend.ALPHA),
        PRISM_HALO_19("prism_halo_19", VfxBlend.ALPHA),
        PRISM_HALO_20("prism_halo_20", VfxBlend.ALPHA),
        PRISM_HALO_21("prism_halo_21", VfxBlend.ALPHA),
        PRISM_HALO_22("prism_halo_22", VfxBlend.ALPHA),
        PRISM_HALO_23("prism_halo_23", VfxBlend.ALPHA),
        PRISM_HALO_FLASH_0("prism_halo_flash_0", VfxBlend.ADDITIVE),
        PRISM_HALO_FLASH_1("prism_halo_flash_1", VfxBlend.ADDITIVE),
        PRISM_CRYSTAL("prism_crystal_0", VfxBlend.ALPHA),
        PRISM_CRYSTAL_1("prism_crystal_1", VfxBlend.ALPHA),
        PRISM_CRYSTAL_2("prism_crystal_2", VfxBlend.ALPHA),
        PRISM_CRYSTAL_3("prism_crystal_3", VfxBlend.ALPHA),
        PRISM_CRYSTAL_4("prism_crystal_4", VfxBlend.ALPHA),
        APPRAISAL_BANNER_0("curators_banner_0", VfxBlend.ALPHA),
        APPRAISAL_BANNER_1("curators_banner_1", VfxBlend.ALPHA),
        APPRAISAL_BANNER_2("curators_banner_2", VfxBlend.ALPHA),
        APPRAISAL_BANNER_3("curators_banner_3", VfxBlend.ALPHA),
        APPRAISAL_BANNER_4("curators_banner_4", VfxBlend.ALPHA),
        APPRAISAL_BANNER_5("curators_banner_5", VfxBlend.ALPHA),
        APPRAISAL_BANNER_6("curators_banner_6", VfxBlend.ALPHA),
        APPRAISAL_BANNER_7("curators_banner_7", VfxBlend.ALPHA),
        APPRAISAL_PEBBLE_0("curators_pebble_0", VfxBlend.ALPHA),
        APPRAISAL_PEBBLE_1("curators_pebble_1", VfxBlend.ALPHA),
        APPRAISAL_PEBBLE_2("curators_pebble_2", VfxBlend.ALPHA),
        APPRAISAL_ORB_0("curators_orb_0", VfxBlend.ALPHA),
        APPRAISAL_ORB_1("curators_orb_1", VfxBlend.ALPHA),
        APPRAISAL_ORB_2("curators_orb_2", VfxBlend.ALPHA),
        APPRAISAL_GEM_0("curators_gem_0", VfxBlend.ALPHA),
        APPRAISAL_GEM_1("curators_gem_1", VfxBlend.ALPHA),
        APPRAISAL_GEM_2("curators_gem_2", VfxBlend.ALPHA),
        APPRAISAL_GEM_3("curators_gem_3", VfxBlend.ALPHA),
        APPRAISAL_GEM_4("curators_gem_4", VfxBlend.ALPHA),
        APPRAISAL_GEM_5("curators_gem_5", VfxBlend.ALPHA),
        APPRAISAL_GEM_6("curators_gem_6", VfxBlend.ALPHA),
        APPRAISAL_GEM_7("curators_gem_7", VfxBlend.ALPHA),
        APPRAISAL_HALO("curators_halo", VfxBlend.ADDITIVE),
        APPRAISAL_BLOOM("curators_bloom", VfxBlend.ADDITIVE, true),
        APPRAISAL_GLARE("curators_glare", VfxBlend.ADDITIVE, true),
        GLEAM_0("gleam_0", VfxBlend.ADDITIVE, false, ITEM_SHEEN),
        GLEAM_1("gleam_1", VfxBlend.ADDITIVE, false, ITEM_SHEEN),
        GLEAM_2("gleam_2", VfxBlend.ADDITIVE, false, ITEM_SHEEN),
        GLEAM_3("gleam_3", VfxBlend.ADDITIVE, false, ITEM_SHEEN),
        GLEAM_4("gleam_4", VfxBlend.ADDITIVE, false, ITEM_SHEEN),
        GLEAM_5("gleam_5", VfxBlend.ADDITIVE, false, ITEM_SHEEN),
        GLEAM_6("gleam_6", VfxBlend.ADDITIVE, false, ITEM_SHEEN),
        GLEAM_7("gleam_7", VfxBlend.ADDITIVE, false, ITEM_SHEEN),
        GLEAM_8("gleam_8", VfxBlend.ADDITIVE, false, ITEM_SHEEN),
        GLEAM_9("gleam_9", VfxBlend.ADDITIVE, false, ITEM_SHEEN),
        GLEAM_10("gleam_10", VfxBlend.ADDITIVE, false, ITEM_SHEEN),
        GLEAM_11("gleam_11", VfxBlend.ADDITIVE, false, ITEM_SHEEN),
        STAR_GLINT("twinkle", VfxBlend.ADDITIVE),
        AURORA_0("aurora_0", VfxBlend.ADDITIVE, true),
        AURORA_1("aurora_1", VfxBlend.ADDITIVE, true),
        AURORA_2("aurora_2", VfxBlend.ADDITIVE, true),
        AURORA_3("aurora_3", VfxBlend.ADDITIVE, true),
        AURORA_4("aurora_4", VfxBlend.ADDITIVE, true),
        AURORA_5("aurora_5", VfxBlend.ADDITIVE, true),
        AURORA_6("aurora_6", VfxBlend.ADDITIVE, true),
        AURORA_7("aurora_7", VfxBlend.ADDITIVE, true),
        APPRAISAL_TRIM_0("curators_trim_0", VfxBlend.ALPHA),
        APPRAISAL_TRIM_1("curators_trim_1", VfxBlend.ALPHA),
        APPRAISAL_TRIM_2("curators_trim_2", VfxBlend.ALPHA),
        APPRAISAL_TRIM_3("curators_trim_3", VfxBlend.ALPHA),
        APPRAISAL_GLOW_0("curators_glow_0", VfxBlend.ADDITIVE),
        APPRAISAL_GLOW_1("curators_glow_1", VfxBlend.ADDITIVE),
        APPRAISAL_GLOW_2("curators_glow_2", VfxBlend.ADDITIVE),
        APPRAISAL_GLOW_3("curators_glow_3", VfxBlend.ADDITIVE),
        HEAT_0("heat_0", VfxBlend.ADDITIVE, true),
        HEAT_1("heat_1", VfxBlend.ADDITIVE, true),
        HEAT_2("heat_2", VfxBlend.ADDITIVE, true),
        HEAT_3("heat_3", VfxBlend.ADDITIVE, true),
        HEAT_4("heat_4", VfxBlend.ADDITIVE, true),
        HEAT_5("heat_5", VfxBlend.ADDITIVE, true),
        HEAT_6("heat_6", VfxBlend.ADDITIVE, true),
        HEAT_7("heat_7", VfxBlend.ADDITIVE, true),
        HEAT_8("heat_8", VfxBlend.ADDITIVE, true),
        HEAT_9("heat_9", VfxBlend.ADDITIVE, true),
        HEAT_10("heat_10", VfxBlend.ADDITIVE, true),
        HEAT_11("heat_11", VfxBlend.ADDITIVE, true),
        HEAT_12("heat_12", VfxBlend.ADDITIVE, true),
        HEAT_13("heat_13", VfxBlend.ADDITIVE, true),
        HEAT_14("heat_14", VfxBlend.ADDITIVE, true),
        HEAT_15("heat_15", VfxBlend.ADDITIVE, true),
        COALS("coals", VfxBlend.ALPHA),
        FIRE_BASE("fire_base", VfxBlend.ALPHA),
        FIRE_0("fire_0", VfxBlend.ADDITIVE),
        FIRE_1("fire_1", VfxBlend.ADDITIVE),
        FIRE_2("fire_2", VfxBlend.ADDITIVE),
        FIRE_3("fire_3", VfxBlend.ADDITIVE),
        FIRE_4("fire_4", VfxBlend.ADDITIVE),
        FIRE_5("fire_5", VfxBlend.ADDITIVE),
        FIRE_6("fire_6", VfxBlend.ADDITIVE),
        FIRE_7("fire_7", VfxBlend.ADDITIVE),
        HOLO_0("holo_0", VfxBlend.ADDITIVE),
        HOLO_1("holo_1", VfxBlend.ADDITIVE),
        HOLO_2("holo_2", VfxBlend.ADDITIVE),
        HOLO_3("holo_3", VfxBlend.ADDITIVE),
        HOLO_4("holo_4", VfxBlend.ADDITIVE),
        HOLO_5("holo_5", VfxBlend.ADDITIVE),
        HOLO_6("holo_6", VfxBlend.ADDITIVE),
        HOLO_7("holo_7", VfxBlend.ADDITIVE),
        HOLO_8("holo_8", VfxBlend.ADDITIVE),
        HOLO_9("holo_9", VfxBlend.ADDITIVE),
        HOLO_10("holo_10", VfxBlend.ADDITIVE),
        HOLO_11("holo_11", VfxBlend.ADDITIVE),
        HOLO_12("holo_12", VfxBlend.ADDITIVE),
        HOLO_13("holo_13", VfxBlend.ADDITIVE),
        HOLO_14("holo_14", VfxBlend.ADDITIVE),
        HOLO_15("holo_15", VfxBlend.ADDITIVE),
        END_BAND("end_band", VfxBlend.ALPHA),
        END_GLOW_0("end_glow_0", VfxBlend.ADDITIVE),
        END_GLOW_1("end_glow_1", VfxBlend.ADDITIVE),
        END_GLOW_2("end_glow_2", VfxBlend.ADDITIVE),
        END_GLOW_3("end_glow_3", VfxBlend.ADDITIVE);

        final String texture;
        final VfxBlend blend;
        final boolean smooth;
        final @Nullable String shader;

        Sprite(String texture, VfxBlend blend) {
            this(texture, blend, false);
        }

        Sprite(String texture, VfxBlend blend, boolean smooth) {
            this(texture, blend, smooth, null);
        }

        Sprite(String texture, VfxBlend blend, boolean smooth, @Nullable String shader) {
            this.texture = texture;
            this.blend = blend;
            this.smooth = smooth;
            this.shader = shader;
        }

        /** Frame {@code frame} of an animation whose frames follow this one, in order. */
        public Sprite frame(int frame) {
            return values()[ordinal() + frame];
        }

        /** The texture this sprite is drawn from. */
        public ResourceLocation sheet() {
            return SPRITES[ordinal()].texture();
        }

        /** Where this frame lies in its flipbook, as u0, v0, u1, v1. */
        public float[] uv() {
            readFlipbooks();
            return FRAME_UVS[ordinal()];
        }

        /** The frame of this animation showing at {@code ticks}, by the timing in its {@code .mcmeta}. Call it on the first frame. */
        public Sprite playing(float ticks) {
            return this.playing(ticks, 0.0F);
        }

        /** As above, running {@code share} of a loop ahead, to keep copies of an animation out of step. */
        public Sprite playing(float ticks, float share) {
            readFlipbooks();
            Flipbook book = BOOKS[ordinal()];
            if (book == null) return this;
            return this.frame(book.ticks[Math.floorMod(Mth.floor(ticks + share * book.ticks.length), book.ticks.length)]);
        }
    }

    public interface Glow {
        void quad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                  float dx, float dy, float dz, int a, int b, int c, int d);

        void lens(float y, float radius, int color, int segments);

        /** A flat textured square lying over the pedestal's middle, turned by {@code roll} radians. */
        default void flat(Sprite texture, float y, float radius, float roll, int color) {
        }

        /** A textured square facing the camera, turned by {@code roll} radians. */
        default void billboard(Sprite texture, float x, float y, float z, float half, float roll, int color) {
        }

        /** A textured quad through four corners: top-left, bottom-left, bottom-right, top-right. */
        default void sprite(Sprite texture, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                            float dx, float dy, float dz, int color) {
        }

        default void aura(float y, float half, int color) {
        }

        /** Says this pedestal draws a rift, which hides the effects of pedestals behind it. */
        default void occludes() {
        }
    }

    static Glow world(BlockPos pos) {
        return world(pos.getX(), pos.getY(), pos.getZ(), 1.0F);
    }

    static Glow world(double x, double y, double z, float scale) {
        WORLD.x = x;
        WORLD.y = y;
        WORLD.z = z;
        WORLD.scale = scale;
        currentOwner = owner(x + 0.5 * scale, y + 0.5 * scale, z + 0.5 * scale);
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
                quadOwners = Arrays.copyOf(quadOwners, quadOwners.length * 2);
            }
            quadOwners[quadCount] = currentOwner;
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
                lensOwners = Arrays.copyOf(lensOwners, lensOwners.length * 2);
            }
            lensOwners[lensCount] = currentOwner;
            int c = lensCount * 3, at = lensCount++ * 2;
            lensCenters[c] = x + 0.5 * scale;
            lensCenters[c + 1] = this.y + y * scale;
            lensCenters[c + 2] = z + 0.5 * scale;
            lenses[at] = radius * scale;
            lenses[at + 1] = Float.intBitsToFloat(color);
        }

        @Override
        public void aura(float y, float half, int color) {
            if (ARGB.alpha(color) == 0) return;
            if ((auraCount + 1) * 2 > auras.length) {
                auras = Arrays.copyOf(auras, auras.length * 2);
                auraCenters = Arrays.copyOf(auraCenters, auraCenters.length * 2);
                auraOwners = Arrays.copyOf(auraOwners, auraOwners.length * 2);
            }
            auraOwners[auraCount] = currentOwner;
            int c = auraCount * 3, at = auraCount++ * 2;
            auraCenters[c] = x + 0.5 * scale;
            auraCenters[c + 1] = this.y + y * scale;
            auraCenters[c + 2] = z + 0.5 * scale;
            auras[at] = half * scale;
            auras[at + 1] = Float.intBitsToFloat(color);
        }

        @Override
        public void flat(Sprite texture, float y, float radius, float roll, int color) {
            int at = sprite(texture, KIND_FLAT, 0.5F, y, 0.5F, color);
            if (at < 0) return;
            sprites[at + 2] = radius * scale;
            sprites[at + 3] = roll;
        }

        @Override
        public void billboard(Sprite texture, float x, float y, float z, float half, float roll, int color) {
            int at = sprite(texture, KIND_BILLBOARD, x, y, z, color);
            if (at < 0) return;
            sprites[at + 2] = half * scale;
            sprites[at + 3] = roll;
        }

        @Override
        public void sprite(Sprite texture, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz,
                           float dx, float dy, float dz, int color) {
            int at = sprite(texture, KIND_QUAD, ax, ay, az, color);
            if (at < 0) return;
            float[] corners = {0.0F, 0.0F, 0.0F, bx - ax, by - ay, bz - az, cx - ax, cy - ay, cz - az, dx - ax, dy - ay, dz - az};
            for (int i = 0; i < corners.length; i++) sprites[at + 5 + i] = corners[i] * scale;
        }

        @Override
        public void occludes() {
            ownerRifts[currentOwner] = true;
        }

        private int sprite(Sprite texture, int kind, float x, float y, float z, int color) {
            if (ARGB.alpha(color) == 0) return -1;
            if ((spriteCount + 1) * SPRITE > sprites.length) {
                sprites = Arrays.copyOf(sprites, sprites.length * 2);
                spriteCenters = Arrays.copyOf(spriteCenters, spriteCenters.length * 2);
                spriteOwners = Arrays.copyOf(spriteOwners, spriteOwners.length * 2);
            }
            spriteOwners[spriteCount] = currentOwner;
            int c = spriteCount * 3, at = spriteCount++ * SPRITE;
            spriteCenters[c] = this.x + x * scale;
            spriteCenters[c + 1] = this.y + y * scale;
            spriteCenters[c + 2] = this.z + z * scale;
            sprites[at] = kind;
            sprites[at + 1] = texture.ordinal();
            sprites[at + 4] = Float.intBitsToFloat(color);
            return at;
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
