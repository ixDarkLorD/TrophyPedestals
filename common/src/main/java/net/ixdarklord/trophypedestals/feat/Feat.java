package net.ixdarklord.trophypedestals.feat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;

import java.util.Locale;
import java.util.Optional;

/**
 * A trophy a data pack gives for a feat, read from {@code data/<namespace>/trophypedestals/feats/<path>.json}: what earns
 * it ({@code trigger}), and how it looks. Every field but the trigger is optional.
 * <pre>{@code
 * {
 *   "trigger": {"type": "stat", "stat": "minecraft:custom", "value": "minecraft:walk_one_cm", "at_least": 10000000},
 *   "title": "Wanderer",          // its name; a text component (default: the lang key trophypedestals.feat.<namespace>.<path>)
 *   "icon": "minecraft:leather_boots", // what turns over it
 *   "rank": "rare",               // its rank (default common)
 *   "pedestal": "trophypedestals:gold_pedestal", // the pedestal it's on (default deepslate)
 *   "once": true                  // once per player (the default), or every time
 * }
 * }</pre>
 * Triggers:
 * <ul>
 *   <li>{@code advancement}: a player earns {@code "advancement"}. Its title and icon are the advancement's unless given.</li>
 *   <li>{@code stat}: a player's statistic {@code "stat"} (its type, {@code minecraft:custom} for most) of {@code "value"}
 *   reaches {@code "at_least"}.</li>
 *   <li>{@code first_kill}: a player kills a mob of {@code "entity"} (an id or {@code #tag}) first, of everyone on the
 *   server ({@code "scope": "server"}, the default) or for themselves ({@code "player"}). The trophy shows the mob.</li>
 * </ul>
 */
public record Feat(Trigger trigger, Optional<Component> title, Optional<ResourceLocation> icon, ResourceLocation rank, Optional<ResourceLocation> pedestal, boolean once) {
    public static final Codec<Feat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Trigger.CODEC.fieldOf("trigger").forGetter(Feat::trigger),
            ComponentSerialization.CODEC.optionalFieldOf("title").forGetter(Feat::title),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(Feat::icon),
            PedestalRarities.ID_CODEC.optionalFieldOf("rank", PedestalRarities.COMMON.id()).forGetter(Feat::rank),
            ResourceLocation.CODEC.optionalFieldOf("pedestal").forGetter(Feat::pedestal),
            Codec.BOOL.optionalFieldOf("once", true).forGetter(Feat::once)
    ).apply(instance, Feat::new));

    /** What earns a feat's trophy. */
    public sealed interface Trigger permits AdvancementTrigger, StatTrigger, FirstKillTrigger {
        Codec<Trigger> CODEC = Codec.STRING.partialDispatch("type", trigger -> DataResult.success(trigger.type()), type -> switch (type) {
            case "advancement" -> DataResult.success(AdvancementTrigger.CODEC);
            case "stat" -> DataResult.success(StatTrigger.CODEC);
            case "first_kill" -> DataResult.success(FirstKillTrigger.CODEC);
            default -> DataResult.error(() -> "Unknown trigger type \"" + type + "\" (advancement, stat or first_kill)");
        });

        String type();
    }

    /** A player earns this advancement. */
    public record AdvancementTrigger(ResourceLocation advancement) implements Trigger {
        static final MapCodec<AdvancementTrigger> CODEC = ResourceLocation.CODEC.fieldOf("advancement").xmap(AdvancementTrigger::new, AdvancementTrigger::advancement);

        @Override
        public String type() {
            return "advancement";
        }
    }

    /** A player's statistic of this type and value reaches this much. */
    public record StatTrigger(ResourceLocation stat, ResourceLocation value, int atLeast) implements Trigger {
        static final MapCodec<StatTrigger> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("stat", ResourceLocation.withDefaultNamespace("custom")).forGetter(StatTrigger::stat),
                ResourceLocation.CODEC.fieldOf("value").forGetter(StatTrigger::value),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("at_least").forGetter(StatTrigger::atLeast)
        ).apply(instance, StatTrigger::new));

        @Override
        public String type() {
            return "stat";
        }
    }

    /** Who a first kill is first among. */
    public enum Scope implements StringRepresentable {
        /** Everyone on the server: one trophy, to whoever's first. */
        SERVER,
        /** Each player, for themselves. */
        PLAYER;

        static final Codec<Scope> CODEC = StringRepresentable.fromEnum(Scope::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** A player kills a mob of this kind first (of everyone, or for themselves). */
    public record FirstKillTrigger(Target entity, Scope scope) implements Trigger {
        static final MapCodec<FirstKillTrigger> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Target.CODEC.fieldOf("entity").forGetter(FirstKillTrigger::entity),
                Scope.CODEC.optionalFieldOf("scope", Scope.SERVER).forGetter(FirstKillTrigger::scope)
        ).apply(instance, FirstKillTrigger::new));

        @Override
        public String type() {
            return "first_kill";
        }
    }

    /** An entity type by id ({@code minecraft:wither}) or by tag ({@code #minecraft:raiders}). */
    public record Target(ResourceLocation id, boolean tag) {
        static final Codec<Target> CODEC = Codec.STRING.comapFlatMap(Target::parse, Target::toString);

        static DataResult<Target> parse(String text) {
            boolean tag = text.startsWith("#");
            return ResourceLocation.read(tag ? text.substring(1) : text).map(id -> new Target(id, tag));
        }

        public boolean matches(EntityType<?> type) {
            Holder<EntityType<?>> holder = type.builtInRegistryHolder();
            return tag ? holder.is(TagKey.create(Registries.ENTITY_TYPE, id)) : holder.is(id);
        }

        @Override
        public String toString() {
            return (tag ? "#" : "") + id;
        }
    }
}
