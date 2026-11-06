package net.ixdarklord.trophypedestals.compat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 1.20.1's stand-in for 1.20.2+'s GUI sprites: a sprite id ({@code trophypedestals:pedestal/panel}) is drawn from its
 * texture ({@code textures/gui/sprites/pedestal/panel.png}), nine-sliced as its {@code .mcmeta} says (its corners kept,
 * its edges and middle stretched), or stretched whole. What each texture is is read once.
 */
public final class GuiSprites {
    private static final Map<ResourceLocation, Sprite> SPRITES = new ConcurrentHashMap<>();

    private GuiSprites() {}

    public static void blit(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height) {
        Sprite read = SPRITES.computeIfAbsent(sprite, GuiSprites::read);
        if (read.width <= 0 || width <= 0 || height <= 0) return;
        int border = read.border;
        if (border <= 0 || width < 2 * border || height < 2 * border) {
            graphics.blit(read.texture, x, y, width, height, 0.0F, 0.0F, read.width, read.height, read.width, read.height);
            return;
        }
        int innerU = read.width - 2 * border, innerV = read.height - 2 * border;
        int innerW = width - 2 * border, innerH = height - 2 * border;
        int[] xs = {x, x + border, x + width - border}, ws = {border, innerW, border};
        int[] ys = {y, y + border, y + height - border}, hs = {border, innerH, border};
        int[] us = {0, border, read.width - border}, uws = {border, innerU, border};
        int[] vs = {0, border, read.height - border}, vhs = {border, innerV, border};
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                if (ws[column] <= 0 || hs[row] <= 0) continue;
                graphics.blit(read.texture, xs[column], ys[row], ws[column], hs[row], us[column], vs[row], uws[column], vhs[row], read.width, read.height);
            }
        }
    }

    // The sprite's texture, its size, and its nine-slice border (0 for none).
    private static Sprite read(ResourceLocation sprite) {
        ResourceLocation texture = new ResourceLocation(sprite.getNamespace(), "textures/gui/sprites/" + sprite.getPath() + ".png");
        int width = 0, height = 0, border = 0;
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resource.isPresent()) {
                try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
                    width = image.getWidth();
                    height = image.getHeight();
                }
                Optional<Resource> meta = Minecraft.getInstance().getResourceManager()
                        .getResource(new ResourceLocation(texture.getNamespace(), texture.getPath() + ".mcmeta"));
                if (meta.isPresent()) {
                    try (Reader reader = meta.get().openAsReader()) {
                        JsonObject read = JsonParser.parseReader(reader).getAsJsonObject();
                        JsonObject scaling = read.has("gui") ? read.getAsJsonObject("gui").getAsJsonObject("scaling") : null;
                        if (scaling != null && "nine_slice".equals(scaling.get("type").getAsString())) border = scaling.get("border").getAsInt();
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            TrophyPedestalsMod.LOGGER.warn("Can't read the GUI sprite {}", texture, e);
        }
        return new Sprite(texture, width, height, border);
    }

    private record Sprite(ResourceLocation texture, int width, int height, int border) {}
}
