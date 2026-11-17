package net.ixdarklord.trophypedestals.compat.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.ixdarklord.coolcatcore.api.client.registry.ExtraModels;
import net.ixdarklord.coolcatcore.api.client.registry.ItemRendererRegistry;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.compat.component.Components;
import net.ixdarklord.trophypedestals.compat.component.DataComponentType;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 26.1's item definitions (the mod's {@code assets/trophypedestals/items/*.json}, which 1.20.1 doesn't read), drawn on
 * 1.20.1: every pedestal, cabinet and shelf item has a code-drawn item model ({@code builtin/entity}), drawn here as
 * its definition says.
 */
public final class ItemDefinitions {
    private static final String FOLDER = "items";
    private static volatile Map<ResourceLocation, Node> definitions = Map.of();

    private ItemDefinitions() {}

    public static void init() {
        ExtraModels.register(ItemDefinitions::read);
        Stream.concat(Stream.concat(Pedestals.ITEMS_BY_STONE.values().stream(), Pedestals.DISPLAY_ITEMS.stream()), Stream.of(Pedestals.CUSTOM_ITEM))
                .forEach(item -> ItemRendererRegistry.register(item, ItemDefinitions::render));
    }

    private static List<ResourceLocation> read(ResourceManager resources) {
        Map<ResourceLocation, Node> read = new HashMap<>();
        Set<ResourceLocation> models = new LinkedHashSet<>();
        Map<ResourceLocation, Resource> files = resources.listResources(FOLDER,
                path -> path.getNamespace().equals(TrophyPedestalsMod.MOD_ID) && path.getPath().endsWith(".json"));
        files.forEach((file, resource) -> {
            String path = file.getPath();
            ResourceLocation item = new ResourceLocation(file.getNamespace(), path.substring(FOLDER.length() + 1, path.length() - ".json".length()));
            try (Reader reader = resource.openAsReader()) {
                read.put(item, node(JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("model"), models));
            } catch (IOException | RuntimeException e) {
                TrophyPedestalsMod.LOGGER.warn("Can't read the item definition {}; that item isn't drawn", file, e);
            }
        });
        definitions = Map.copyOf(read);
        return new ArrayList<>(models);
    }

    private static Node node(JsonObject json, Set<ResourceLocation> models) {
        String type = new ResourceLocation(json.get("type").getAsString()).toString();
        switch (type) {
            case "minecraft:model" -> {
                ResourceLocation model = new ResourceLocation(json.get("model").getAsString());
                models.add(model);
                return new Model(model);
            }
            case "minecraft:special" -> {
                ResourceLocation base = new ResourceLocation(json.get("base").getAsString());
                models.add(base);
                JsonObject special = json.getAsJsonObject("model");
                ResourceLocation id = new ResourceLocation(special.get("type").getAsString());
                MapCodec<? extends SpecialModelRenderer.Unbaked<?>> codec = SpecialModelRendererRegistry.get(id);
                if (codec == null) throw new IllegalArgumentException("unknown special model type " + id);
                SpecialModelRenderer.Unbaked<?> unbaked = codec.codec().parse(JsonOps.INSTANCE, special).getOrThrow(false, error -> {});
                SpecialModelRenderer<?> renderer = unbaked.bake(SpecialModelRenderer.BakingContext.INSTANCE);
                return renderer == null ? Empty.INSTANCE : new Special<>(base, renderer);
            }
            case "minecraft:composite" -> {
                List<Node> parts = new ArrayList<>();
                for (JsonElement part : json.getAsJsonArray("models")) parts.add(node(part.getAsJsonObject(), models));
                return new Composite(List.copyOf(parts));
            }
            case "minecraft:condition" -> {
                if (!json.get("property").getAsString().endsWith("has_component")) throw new IllegalArgumentException("unknown condition " + json.get("property"));
                return new HasComponent(new ResourceLocation(json.get("component").getAsString()),
                        node(json.getAsJsonObject("on_true"), models), node(json.getAsJsonObject("on_false"), models));
            }
            case "minecraft:select" -> {
                if (!json.get("property").getAsString().endsWith("display_context")) throw new IllegalArgumentException("unknown select " + json.get("property"));
                Map<ItemDisplayContext, Node> cases = new EnumMap<>(ItemDisplayContext.class);
                for (JsonElement element : json.getAsJsonArray("cases")) {
                    JsonObject entry = element.getAsJsonObject();
                    Node node = node(entry.getAsJsonObject("model"), models);
                    JsonElement when = entry.get("when");
                    JsonArray contexts = when.isJsonArray() ? when.getAsJsonArray() : new JsonArray();
                    if (!when.isJsonArray()) contexts.add(when);
                    for (JsonElement context : contexts) {
                        for (ItemDisplayContext known : ItemDisplayContext.values()) {
                            if (known.getSerializedName().equals(context.getAsString())) cases.put(known, node);
                        }
                    }
                }
                return new Select(cases, json.has("fallback") ? node(json.getAsJsonObject("fallback"), models) : Empty.INSTANCE);
            }
            default -> throw new IllegalArgumentException("unknown item model type " + type);
        }
    }

    private static void render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        Node definition = definitions.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        if (definition == null) return;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        definition.render(stack, context, poseStack, buffers, light, overlay);
        poseStack.popPose();
    }

    private static boolean leftHand(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
    }

    private interface Node {
        void render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay);
    }

    private enum Empty implements Node {
        INSTANCE;

        @Override
        public void render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {}
    }

    private record Model(ResourceLocation model) implements Node {
        @Override
        public void render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
            BakedModel baked = ExtraModels.get(model);
            MultiBufferSource translucent = type -> buffers.getBuffer(
                    type == Sheets.solidBlockSheet() || type == Sheets.cutoutBlockSheet() ? Sheets.translucentItemSheet() : type);
            Minecraft.getInstance().getItemRenderer().render(stack, context, leftHand(context), poseStack, translucent, light, overlay, baked);
        }
    }

    private record Special<T>(ResourceLocation base, SpecialModelRenderer<T> renderer) implements Node {
        @Override
        public void render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
            poseStack.pushPose();
            ExtraModels.get(base).getTransforms().getTransform(context).apply(leftHand(context), poseStack);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            SubmitNodeCollector collector = new SubmitNodeCollector(buffers);
            renderer.submit(renderer.extractArgument(stack), poseStack, collector, light, overlay, stack.hasFoil(), 0);
            collector.flush(true);
            poseStack.popPose();
        }
    }

    private record Composite(List<Node> parts) implements Node {
        @Override
        public void render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
            for (Node part : parts) part.render(stack, context, poseStack, buffers, light, overlay);
        }
    }

    private record HasComponent(ResourceLocation component, Node onTrue, Node onFalse) implements Node {
        @Override
        public void render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
            DataComponentType<?> type = Components.byId(component.toString());
            (type != null && Components.has(stack, type) ? onTrue : onFalse).render(stack, context, poseStack, buffers, light, overlay);
        }
    }

    private record Select(Map<ItemDisplayContext, Node> cases, Node fallback) implements Node {
        @Override
        public void render(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
            cases.getOrDefault(context, fallback).render(stack, context, poseStack, buffers, light, overlay);
        }
    }
}
