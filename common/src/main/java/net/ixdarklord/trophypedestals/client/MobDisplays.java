package net.ixdarklord.trophypedestals.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.api.TrophyEvents;
import net.ixdarklord.trophypedestals.pedestal.MobDisplayData;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class MobDisplays {
    private static volatile Map<EntityType<?>, MobDisplayData.Display> displays = Map.of();
    private static volatile boolean read;
    private static boolean watchingReloads;

    private MobDisplays() {}

    static MobDisplayData.@Nullable Display of(EntityType<?> type) {
        if (!read) load();
        return displays.get(type);
    }

    private static synchronized void load() {
        if (read) return;
        read = true;
        ResourceManager manager = Minecraft.getInstance().getResourceManager();
        if (!watchingReloads && manager instanceof ReloadableResourceManager reloadable) {
            watchingReloads = true;
            reloadable.registerReloadListener((ResourceManagerReloadListener) reloaded -> read = false);
        }
        Map<EntityType<?>, MobDisplayData.Display> loaded = new HashMap<>();
        Map<ResourceLocation, List<Resource>> files = new TreeMap<>(manager.listResourceStacks(MobDisplayData.FOLDER, path -> path.getPath().endsWith(".json")));
        for (Map.Entry<ResourceLocation, List<Resource>> file : files.entrySet()) {
            for (Resource layer : file.getValue()) {
                try (Reader reader = layer.openAsReader()) {
                    JsonElement json = JsonParser.parseReader(reader);
                    if (json.isJsonObject()) MobDisplayData.parse(json.getAsJsonObject(), loaded);
                } catch (Exception e) {
                    TrophyPedestalsMod.LOGGER.warn("Can't read trophy mob display sizes from {}", file.getKey(), e);
                }
            }
        }
        TrophyEvents.MOB_DISPLAY_SIZES.invoker().onLoad((mob, scale, size, offset) ->
                loaded.put(mob, new MobDisplayData.Display(Math.max(0.0F, scale), Math.max(0.0F, size), offset)));
        displays = Map.copyOf(loaded);
    }
}
