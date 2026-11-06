package net.ixdarklord.trophypedestals.compat;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.mojang.datafixers.Products.P1;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;

/**
 * 1.20.1's stand-ins for 1.20.5+'s loot codecs: loot functions here are read and written as JSON by a serializer, so a
 * function's codec (its own fields, and its {@code "conditions"}) is wrapped as one ({@link #serializer}), and its
 * conditions are read by the game's own JSON condition reader.
 */
public final class LootCodecs {
    private static final Gson CONDITIONS = Deserializers.createConditionSerializer().create();
    private static final Codec<LootItemCondition> CONDITION = ExtraCodecs.JSON.comapFlatMap(json -> {
        try {
            return DataResult.success(CONDITIONS.fromJson(json, LootItemCondition.class));
        } catch (RuntimeException e) {
            return DataResult.error(e::toString);
        }
    }, CONDITIONS::toJsonTree);

    private LootCodecs() {}

    /**
     * A conditional function's {@code "conditions"}, as 1.20.5+'s {@code commonFields} reads them. (They aren't written
     * back: a function's conditions aren't visible outside it here, and nothing writes a function's JSON in play.)
     */
    public static <T extends LootItemConditionalFunction> P1<RecordCodecBuilder.Mu<T>, List<LootItemCondition>> commonFields(RecordCodecBuilder.Instance<T> instance) {
        return instance.group(CONDITION.listOf().optionalFieldOf("conditions", List.of()).forGetter(function -> List.of()));
    }

    /** A loot function type's JSON serializer, reading and writing the function with its codec. */
    public static <T extends LootItemFunction> Serializer<T> serializer(MapCodec<T> codec) {
        return new Serializer<>() {
            @Override
            public void serialize(JsonObject json, T value, JsonSerializationContext context) {
                codec.encoder().encodeStart(JsonOps.INSTANCE, value).result()
                        .filter(JsonElement::isJsonObject)
                        .ifPresent(encoded -> encoded.getAsJsonObject().entrySet().forEach(entry -> json.add(entry.getKey(), entry.getValue())));
            }

            @Override
            public T deserialize(JsonObject json, JsonDeserializationContext context) {
                return codec.codec().parse(JsonOps.INSTANCE, json).getOrThrow(false, error -> {
                    throw new JsonParseException(error);
                });
            }
        };
    }
}
