package net.ixdarklord.trophypedestals.compat.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class PedestalOverlays {
    private static final Map<String, ResourceLocation> TEXTURES = Map.of(
            "#column_flutes", ResourceLocation.tryParse("trophypedestals:block/pedestal/column_flutes"),
            "#well_imprint", ResourceLocation.tryParse("trophypedestals:block/pedestal/well_imprint"));
    private static final float SHADE_UP = 1.0F, SHADE_DOWN = 0.5F, SHADE_NORTH_SOUTH = 0.8F, SHADE_EAST_WEST = 0.6F;
    private static final Map<Block, List<Face>> FACES = new ConcurrentHashMap<>();

    private PedestalOverlays() {}

    public static void draw(BlockState state, PoseStack poseStack, MultiBufferSource buffers, int light) {
        List<Face> faces = FACES.computeIfAbsent(state.getBlock(), PedestalOverlays::read);
        if (faces.isEmpty()) return;
        new SubmitNodeCollector(buffers).submitCustomGeometry(poseStack, RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS), (pose, consumer) -> {
            for (Face face : faces) {
                TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(face.texture);
                int shade = Mth.floor(255.0F * face.shade);
                int color = 0xFF000000 | shade << 16 | shade << 8 | shade;
                for (int i = 0; i < 4; i++) {
                    consumer.addVertex(pose, face.x[i], face.y[i], face.z[i]).setColor(color)
                            .setUv(Mth.lerp(face.u[i], sprite.getU0(), sprite.getU1()), Mth.lerp(face.v[i], sprite.getV0(), sprite.getV1()))
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, 0.0F, 1.0F, 0.0F);
                }
            }
        });
    }

    private static List<Face> read(Block block) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        JsonObject model = json(ResourceLocation.tryParse(id.getNamespace() + ":models/block/" + id.getPath() + ".json"));
        if (model == null || !model.has("parent")) return List.of();
        ResourceLocation parent = ResourceLocation.tryParse(model.get("parent").getAsString());
        if (parent == null) return List.of();
        JsonObject template = json(ResourceLocation.tryParse(parent.getNamespace() + ":models/" + parent.getPath() + ".json"));
        if (template == null || !template.has("elements")) return List.of();
        List<Face> faces = new ArrayList<>();
        for (JsonElement element : template.getAsJsonArray("elements")) {
            JsonObject box = element.getAsJsonObject();
            float[] from = vector(box.getAsJsonArray("from")), to = vector(box.getAsJsonArray("to"));
            for (Map.Entry<String, JsonElement> entry : box.getAsJsonObject("faces").entrySet()) {
                ResourceLocation texture = TEXTURES.get(entry.getValue().getAsJsonObject().get("texture").getAsString());
                Direction direction = Direction.byName(entry.getKey());
                if (texture != null && direction != null) faces.add(face(direction, from, to, texture));
            }
        }
        return List.copyOf(faces);
    }

    private static JsonObject json(ResourceLocation file) {
        if (file == null) return null;
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(file);
        if (resource.isEmpty()) return null;
        try (Reader reader = resource.get().openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private static float[] vector(JsonArray array) {
        return new float[]{array.get(0).getAsFloat() / 16.0F, array.get(1).getAsFloat() / 16.0F, array.get(2).getAsFloat() / 16.0F};
    }

    private static Face face(Direction direction, float[] from, float[] to, ResourceLocation texture) {
        float x0 = from[0], y0 = from[1], z0 = from[2], x1 = to[0], y1 = to[1], z1 = to[2];
        return switch (direction) {
            case UP -> new Face(texture, SHADE_UP, new float[]{x0, x0, x1, x1}, new float[]{y1, y1, y1, y1}, new float[]{z0, z1, z1, z0},
                    new float[]{0, 0, 1, 1}, new float[]{0, 1, 1, 0});
            case DOWN -> new Face(texture, SHADE_DOWN, new float[]{x0, x1, x1, x0}, new float[]{y0, y0, y0, y0}, new float[]{z0, z0, z1, z1},
                    new float[]{0, 1, 1, 0}, new float[]{1, 1, 0, 0});
            case NORTH -> new Face(texture, SHADE_NORTH_SOUTH, new float[]{x1, x1, x0, x0}, new float[]{y1, y0, y0, y1}, new float[]{z0, z0, z0, z0},
                    new float[]{0, 0, 1, 1}, new float[]{0, 1, 1, 0});
            case SOUTH -> new Face(texture, SHADE_NORTH_SOUTH, new float[]{x0, x0, x1, x1}, new float[]{y1, y0, y0, y1}, new float[]{z1, z1, z1, z1},
                    new float[]{0, 0, 1, 1}, new float[]{0, 1, 1, 0});
            case WEST -> new Face(texture, SHADE_EAST_WEST, new float[]{x0, x0, x0, x0}, new float[]{y1, y0, y0, y1}, new float[]{z0, z0, z1, z1},
                    new float[]{0, 0, 1, 1}, new float[]{0, 1, 1, 0});
            case EAST -> new Face(texture, SHADE_EAST_WEST, new float[]{x1, x1, x1, x1}, new float[]{y1, y0, y0, y1}, new float[]{z1, z1, z0, z0},
                    new float[]{0, 0, 1, 1}, new float[]{0, 1, 1, 0});
        };
    }

    private record Face(ResourceLocation texture, float shade, float[] x, float[] y, float[] z, float[] u, float[] v) {}
}
