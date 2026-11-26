package net.ixdarklord.trophypedestals.pedestal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

import java.util.Map;

public final class MobDisplayData {
    public static final String FOLDER = "trophypedestals/mob_displays";

    private MobDisplayData() {}

    public record Display(float scale, float size, float offset) {}

    public static int parse(JsonObject json, Map<EntityType<?>, Display> into) {
        int read = 0;
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            if (id == null || !entry.getValue().isJsonObject()) continue;
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
            if (type == null) continue;
            JsonObject display = entry.getValue().getAsJsonObject();
            float scale = number(display, "scale", 1.0F), size = number(display, "size", 0.0F), offset = number(display, "offset", 0.0F);
            into.put(type, new Display(Math.max(0.0F, scale), Math.max(0.0F, size), offset));
            read++;
        }
        return read;
    }

    private static float number(JsonObject json, String key, float fallback) {
        JsonElement value = json.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber() ? value.getAsFloat() : fallback;
    }
}
