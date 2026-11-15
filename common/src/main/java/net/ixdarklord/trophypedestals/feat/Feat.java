package net.ixdarklord.trophypedestals.feat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;

import java.util.Locale;
import java.util.Optional;

/**
 * A trophy a data pack gives for a feat, read from {@code data/<namespace>/trophypedestals/feats/<path>.json}: what
 * earns it ({@code trigger}), and how it looks.
 */
public record Feat(Trigger trigger, Optional<Component> title, Optional<ResourceLocation> icon, ResourceLocation rank, Optional<ResourceLocation> pedestal, boolean once) {
    public static final Codec<Feat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Trigger.CODEC.fieldOf("trigger").forGetter(Feat::trigger),
            ExtraCodecs.COMPONENT.optionalFieldOf("title").forGetter(Feat::title),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(Feat::icon),
            PedestalRarities.ID_CODEC.optionalFieldOf("rank", PedestalRarities.COMMON.id()).forGetter(Feat::rank),
            ResourceLocation.CODEC.optionalFieldOf("pedestal").forGetter(Feat::pedestal),
            Codec.BOOL.optionalFieldOf("once", true).forGetter(Feat::once)
    ).apply(instance, Feat::new));

    public sealed interface Trigger permits AdvancementTrigger, StatTrigger, FirstKillTrigger, CollectionTrigger {
        Codec<Trigger> CODEC = Codec.STRING.partialDispatch("type", trigger -> DataResult.success(trigger.type()), type -> switch (type) {
            case "advancement" -> DataResult.success(AdvancementTrigger.CODEC.codec());
            case "stat" -> DataResult.success(StatTrigger.CODEC.codec());
            case "first_kill" -> DataResult.success(FirstKillTrigger.CODEC.codec());
            case "collection" -> DataResult.success(CollectionTrigger.CODEC.codec());
            default -> DataResult.error(() -> "Unknown trigger type \"" + type + "\" (advancement, stat, first_kill or collection)");
        });

        String type();
    }

    public record AdvancementTrigger(ResourceLocation advancement) implements Trigger {
        static final MapCodec<AdvancementTrigger> CODEC = ResourceLocation.CODEC.fieldOf("advancement").xmap(AdvancementTrigger::new, AdvancementTrigger::advancement);

        @Override
        public String type() {
            return "advancement";
        }
    }

    public record StatTrigger(ResourceLocation stat, ResourceLocation value, int atLeast) implements Trigger {
        static final MapCodec<StatTrigger> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.optionalFieldOf("stat", new ResourceLocation("custom")).forGetter(StatTrigger::stat),
                ResourceLocation.CODEC.fieldOf("value").forGetter(StatTrigger::value),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("at_least").forGetter(StatTrigger::atLeast)
        ).apply(instance, StatTrigger::new));

        @Override
        public String type() {
            return "stat";
        }
    }

    public record CollectionTrigger(int atLeast, boolean gilded) implements Trigger {
        static final MapCodec<CollectionTrigger> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("at_least").forGetter(CollectionTrigger::atLeast),
                Codec.BOOL.optionalFieldOf("gilded", false).forGetter(CollectionTrigger::gilded)
        ).apply(instance, CollectionTrigger::new));

        @Override
        public String type() {
            return "collection";
        }
    }

    public enum Scope implements StringRepresentable {
        SERVER,
        PLAYER;

        static final Codec<Scope> CODEC = StringRepresentable.fromEnum(Scope::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

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
