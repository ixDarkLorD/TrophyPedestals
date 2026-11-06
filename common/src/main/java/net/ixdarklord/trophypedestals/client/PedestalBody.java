package net.ixdarklord.trophypedestals.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.compat.ARGB;
import net.ixdarklord.trophypedestals.compat.render.SubmitNodeCollector;
import net.ixdarklord.trophypedestals.compat.render.VertexConsumer;
import net.ixdarklord.trophypedestals.pedestal.TrophyDisplayBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A custom pedestal's body, drawn with the textures of the block it's made of: the pedestal's own shape (the faces of
 * {@code models/block/pedestal_template_chiseled.json}, its gold carving included), its top, bottom and sides taken from
 * that block's model (and tinted as the block is, a leaf's green, say), its gold trim and the dark lens in its well as on
 * every pedestal. Each face is shaded as a block's would be. The shape and each material's textures are read once, and
 * again after the resources reload.
 */
public final class PedestalBody {
    private static final ResourceLocation TEMPLATE = TrophyPedestalsMod.id("models/block/pedestal_template_fluted.json");
    private static final ResourceLocation GOLD = TrophyPedestalsMod.id("block/pedestal/gold_trim");
    private static final ResourceLocation WELL_IMPRINT = TrophyPedestalsMod.id("block/pedestal/well_imprint");
    private static final ResourceLocation LENS = TrophyPedestalsMod.id("block/pedestal/lens");
    private static final ResourceLocation FLUTES = TrophyPedestalsMod.id("block/pedestal/column_flutes");
    // A pedestal with no material, or one no longer in the game, is drawn as stone.
    private static final Block FALLBACK = Blocks.STONE;
    // Faces turned to the light: as a block's are shaded.
    private static final float SHADE_UP = 1.0F, SHADE_DOWN = 0.5F, SHADE_NORTH_SOUTH = 0.8F, SHADE_EAST_WEST = 0.6F;

    // What the shape and textures were read for: the models of the last resource load (known by its missing model).
    private static @Nullable BakedModel readFor;
    private static List<Face> faces = List.of();
    private static final Map<Block, Look> LOOKS = new ConcurrentHashMap<>();
    // The gold faces of the trophy displays' models, by model, for trims of other blocks.
    private static final Map<ResourceLocation, List<Face>> TRIMS = new ConcurrentHashMap<>();

    private PedestalBody() {}

    /** Draws a custom pedestal's body, of this material (stone for none), in this light. */
    public static void submit(@Nullable ResourceLocation material, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        reread();
        if (faces.isEmpty()) return;
        Block block = material == null ? FALLBACK : BuiltInRegistries.BLOCK.getOptional(material).orElse(FALLBACK);
        Look look = LOOKS.computeIfAbsent(block, PedestalBody::look);
        collector.submitCustomGeometry(poseStack, RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS), (pose, consumer) -> {
            for (Face face : faces) if (!face.texture.overlay()) face(consumer, pose, face, look, light);
        });
        // The well's shading and the column's flutes are see-through, drawn over the material.
        collector.submitCustomGeometry(poseStack, Sheets.translucentCullBlockSheet(), (pose, consumer) -> {
            for (Face face : faces) if (face.texture.overlay()) face(consumer, pose, face, look, light);
        });
    }

    // After the resources reload, the shapes and textures are read again.
    private static void reread() {
        BakedModel models = Minecraft.getInstance().getModelManager().getMissingModel();
        if (models == readFor) return;
        readFor = models;
        faces = readFaces(TEMPLATE);
        LOOKS.clear();
        TRIMS.clear();
    }

    /**
     * Draws a trophy display's trim made of a block: the gold faces of its model ({@code model}, a model file's id, as
     * {@code trophypedestals:models/block/trophy_shelf.json}) in that block's textures (its top on the faces turned up, its
     * bottom on those turned down, a side on the rest), each pixel where the gold's would be, in this light.
     */
    public static void submitTrim(ResourceLocation model, ResourceLocation material, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        reread();
        List<Face> trim = TRIMS.computeIfAbsent(model, PedestalBody::readFaces);
        if (trim.isEmpty()) return;
        Block block = BuiltInRegistries.BLOCK.getOptional(material).orElse(FALLBACK);
        Look look = LOOKS.computeIfAbsent(block, PedestalBody::look);
        // (The gold trim, a trimmed item's when it's gold, in the gold trim's own texture.)
        boolean gold = material.equals(TrophyDisplayBlock.GOLD);
        collector.submitCustomGeometry(poseStack, RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS), (pose, consumer) -> {
            for (Face face : trim) {
                if (face.texture != Texture.GOLD) continue;
                Texture as = gold ? Texture.GOLD : face.direction == Direction.UP ? Texture.TOP : face.direction == Direction.DOWN ? Texture.BOTTOM : Texture.SIDE;
                face(consumer, pose, face, look.sprite(as), look.tint(as), light);
            }
        });
    }

    private static void face(VertexConsumer consumer, PoseStack.Pose pose, Face face, Look look, int light) {
        face(consumer, pose, face, look.sprite(face.texture), look.tint(face.texture), light);
    }

    private static void face(VertexConsumer consumer, PoseStack.Pose pose, Face face, TextureAtlasSprite sprite, int tint, int light) {
        int color = ARGB.color(255, ARGB.scaleRGB(tint, face.shade));
        for (int i = 0; i < 4; i++) {
            consumer.addVertex(pose, face.x[i], face.y[i], face.z[i]).setColor(color).setUv(sprite.getU(16.0F * face.u[i]), sprite.getV(16.0F * face.v[i]))
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
        }
    }

    // A material's textures and tints, from its block's model: its top's, bottom's and a side's, or its particle texture
    // for a face it doesn't have (a model that isn't a whole block).
    private static Look look(Block block) {
        BlockState state = block.defaultBlockState();
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        TextureAtlasSprite particle = model.getParticleIcon();
        BakedQuad top = quad(model, state, Direction.UP), bottom = quad(model, state, Direction.DOWN), side = quad(model, state, Direction.NORTH);
        if (side == null) side = quad(model, state, Direction.EAST);
        TextureAtlas atlas = Minecraft.getInstance().getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
        return new Look(sprite(top, particle), sprite(bottom, particle), sprite(side, particle),
                tint(top, state), tint(bottom, state), tint(side, state), atlas.getSprite(GOLD), atlas.getSprite(WELL_IMPRINT),
                atlas.getSprite(LENS), atlas.getSprite(FLUTES));
    }

    private static @Nullable BakedQuad quad(BakedModel model, BlockState state, Direction direction) {
        List<BakedQuad> quads = model.getQuads(state, direction, RandomSource.create(42L));
        return quads.isEmpty() ? null : quads.get(0);
    }

    private static TextureAtlasSprite sprite(@Nullable BakedQuad quad, TextureAtlasSprite fallback) {
        return quad == null ? fallback : quad.getSprite();
    }

    // The face's tint, as the block's own colors give it (white for one that isn't tinted).
    private static int tint(@Nullable BakedQuad quad, BlockState state) {
        if (quad == null || !quad.isTinted()) return 0xFFFFFF;
        int color = Minecraft.getInstance().getBlockColors().getColor(state, null, null, quad.getTintIndex());
        return color == -1 ? 0xFFFFFF : color & 0xFFFFFF;
    }

    // A model's shape: every face of its boxes (those of the textures drawn here), turned as each box is, in the block's
    // units.
    private static List<Face> readFaces(ResourceLocation template) {
        try (Reader reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(template).openAsReader()) {
            JsonObject model = JsonParser.parseReader(reader).getAsJsonObject();
            List<Face> read = new ArrayList<>();
            for (JsonElement element : model.getAsJsonArray("elements")) box(element.getAsJsonObject(), read);
            return List.copyOf(read);
        } catch (IOException | RuntimeException e) {
            TrophyPedestalsMod.LOGGER.warn("Can't read the shape from {}; what's drawn from it isn't", template, e);
            return List.of();
        }
    }

    private static void box(JsonObject element, List<Face> out) {
        float[] from = triple(element.getAsJsonArray("from")), to = triple(element.getAsJsonArray("to"));
        JsonObject rotation = element.has("rotation") ? element.getAsJsonObject("rotation") : null;
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject("faces").entrySet()) {
            Direction direction = Direction.byName(entry.getKey());
            JsonObject face = entry.getValue().getAsJsonObject();
            Texture texture = Texture.of(face.get("texture").getAsString());
            if (direction == null || texture == null) continue;
            float[] uv = face.has("uv") ? quad(face.getAsJsonArray("uv")) : new float[] {0, 0, 16, 16};
            out.add(face(direction, from, to, uv, texture, rotation));
        }
    }

    // A face's corners (top left, bottom left, bottom right, top right, as seen from outside) and their texture
    // coordinates, as a block model lays them.
    private static Face face(Direction direction, float[] f, float[] t, float[] uv, Texture texture, @Nullable JsonObject rotation) {
        float[][] corners = switch (direction) {
            case NORTH -> new float[][] {{t[0], t[1], f[2]}, {t[0], f[1], f[2]}, {f[0], f[1], f[2]}, {f[0], t[1], f[2]}};
            case SOUTH -> new float[][] {{f[0], t[1], t[2]}, {f[0], f[1], t[2]}, {t[0], f[1], t[2]}, {t[0], t[1], t[2]}};
            case WEST -> new float[][] {{f[0], t[1], f[2]}, {f[0], f[1], f[2]}, {f[0], f[1], t[2]}, {f[0], t[1], t[2]}};
            case EAST -> new float[][] {{t[0], t[1], t[2]}, {t[0], f[1], t[2]}, {t[0], f[1], f[2]}, {t[0], t[1], f[2]}};
            case UP -> new float[][] {{f[0], t[1], f[2]}, {f[0], t[1], t[2]}, {t[0], t[1], t[2]}, {t[0], t[1], f[2]}};
            case DOWN -> new float[][] {{f[0], f[1], t[2]}, {f[0], f[1], f[2]}, {t[0], f[1], f[2]}, {t[0], f[1], t[2]}};
        };
        float[] us = {uv[0], uv[0], uv[2], uv[2]}, vs = {uv[1], uv[3], uv[3], uv[1]};
        Face face = new Face(texture, direction, switch (direction) {
            case UP -> SHADE_UP;
            case DOWN -> SHADE_DOWN;
            case NORTH, SOUTH -> SHADE_NORTH_SOUTH;
            case EAST, WEST -> SHADE_EAST_WEST;
        });
        Vector3f corner = new Vector3f();
        for (int i = 0; i < 4; i++) {
            corner.set(corners[i][0], corners[i][1], corners[i][2]);
            if (rotation != null) turn(corner, rotation);
            face.x[i] = corner.x / 16.0F;
            face.y[i] = corner.y / 16.0F;
            face.z[i] = corner.z / 16.0F;
            face.u[i] = us[i] / 16.0F;
            face.v[i] = vs[i] / 16.0F;
        }
        return face;
    }

    // Turns a corner about the box's rotation origin and axis, as a block model turns an element.
    private static void turn(Vector3f corner, JsonObject rotation) {
        float[] origin = triple(rotation.getAsJsonArray("origin"));
        float angle = rotation.get("angle").getAsFloat() * Mth.DEG_TO_RAD;
        corner.sub(origin[0], origin[1], origin[2]);
        switch (rotation.get("axis").getAsString()) {
            case "x" -> corner.rotateX(angle);
            case "y" -> corner.rotateY(angle);
            default -> corner.rotateZ(angle);
        }
        corner.add(origin[0], origin[1], origin[2]);
    }

    private static float[] triple(JsonArray array) {
        return new float[] {array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
    }

    private static float[] quad(JsonArray array) {
        return new float[] {array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat(), array.get(3).getAsFloat()};
    }

    // The template's textures: the material's faces, the gold trim, the well's shading, the lens's glass and the column's
    // flutes.
    private enum Texture {
        TOP, BOTTOM, SIDE, COLUMN, GOLD, WELL_IMPRINT, LENS, FLUTES;

        // Whether it's see-through shading over the material, drawn after it.
        boolean overlay() {
            return this == WELL_IMPRINT || this == FLUTES;
        }

        static @Nullable Texture of(String key) {
            return switch (key) {
                case "#top", "#well" -> TOP;
                case "#bottom" -> BOTTOM;
                case "#side" -> SIDE;
                case "#column" -> COLUMN;
                case "#gold" -> GOLD;
                case "#well_imprint" -> WELL_IMPRINT;
                case "#lens" -> LENS;
                case "#column_flutes" -> FLUTES;
                default -> null;
            };
        }
    }

    private static final class Face {
        final Texture texture;
        final Direction direction;
        final float shade;
        final float[] x = new float[4], y = new float[4], z = new float[4], u = new float[4], v = new float[4];

        Face(Texture texture, Direction direction, float shade) {
            this.texture = texture;
            this.direction = direction;
            this.shade = shade;
        }
    }

    private record Look(TextureAtlasSprite top, TextureAtlasSprite bottom, TextureAtlasSprite side, int topTint, int bottomTint, int sideTint,
                        TextureAtlasSprite gold, TextureAtlasSprite wellImprint, TextureAtlasSprite lens, TextureAtlasSprite flutes) {
        TextureAtlasSprite sprite(Texture texture) {
            return switch (texture) {
                case TOP -> top;
                case BOTTOM -> bottom;
                case SIDE, COLUMN -> side;
                case GOLD -> gold;
                case WELL_IMPRINT -> wellImprint;
                case LENS -> lens;
                case FLUTES -> flutes;
            };
        }

        int tint(Texture texture) {
            return switch (texture) {
                case TOP -> topTint;
                case BOTTOM -> bottomTint;
                case SIDE, COLUMN -> sideTint;
                case GOLD, WELL_IMPRINT, LENS, FLUTES -> 0xFFFFFF;
            };
        }
    }
}
