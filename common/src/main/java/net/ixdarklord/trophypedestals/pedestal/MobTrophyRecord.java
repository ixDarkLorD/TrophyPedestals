package net.ixdarklord.trophypedestals.pedestal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;

/** What a mob trophy records: the mob slain, who slew it and when, and how grand the trophy looks. */
public record MobTrophyRecord(Identifier entity, String slayer, long slainAt, Identifier rarity, CompoundTag look, Optional<Component> title,
                              int kills, boolean gilded) {
    public static final Codec<MobTrophyRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("entity").forGetter(MobTrophyRecord::entity),
            Codec.STRING.optionalFieldOf("slayer", "").forGetter(MobTrophyRecord::slayer),
            Codec.LONG.fieldOf("slain_at").forGetter(MobTrophyRecord::slainAt),
            PedestalRarities.ID_CODEC.optionalFieldOf("rarity", PedestalRarities.COMMON.id()).forGetter(MobTrophyRecord::rarity),
            CompoundTag.CODEC.optionalFieldOf("look", new CompoundTag()).forGetter(MobTrophyRecord::look),
            ComponentSerialization.CODEC.optionalFieldOf("title").forGetter(MobTrophyRecord::title),
            Codec.INT.optionalFieldOf("kills", 0).forGetter(MobTrophyRecord::kills),
            Codec.BOOL.optionalFieldOf("gilded", false).forGetter(MobTrophyRecord::gilded)
    ).apply(instance, MobTrophyRecord::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobTrophyRecord> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, MobTrophyRecord::entity,
            ByteBufCodecs.STRING_UTF8, MobTrophyRecord::slayer,
            ByteBufCodecs.VAR_LONG, MobTrophyRecord::slainAt,
            Identifier.STREAM_CODEC, MobTrophyRecord::rarity,
            ByteBufCodecs.COMPOUND_TAG, MobTrophyRecord::look,
            ComponentSerialization.OPTIONAL_STREAM_CODEC, MobTrophyRecord::title,
            ByteBufCodecs.VAR_INT, MobTrophyRecord::kills,
            ByteBufCodecs.BOOL, MobTrophyRecord::gilded,
            MobTrophyRecord::new);

    public MobTrophyRecord(Identifier entity, String slayer, long slainAt, Identifier rarity) {
        this(entity, slayer, slainAt, rarity, new CompoundTag());
    }

    public MobTrophyRecord(Identifier entity, String slayer, long slainAt, Identifier rarity, CompoundTag look) {
        this(entity, slayer, slainAt, rarity, look, Optional.empty());
    }

    public MobTrophyRecord(Identifier entity, String slayer, long slainAt, Identifier rarity, CompoundTag look, Optional<Component> title) {
        this(entity, slayer, slainAt, rarity, look, title, 0, false);
    }

    public MobTrophyRecord withTitle(Optional<Component> title) {
        return new MobTrophyRecord(entity, slayer, slainAt, rarity, look, title, kills, gilded);
    }

    public MobTrophyRecord withKills(int kills, Identifier rarity) {
        return new MobTrophyRecord(entity, slayer, slainAt, rarity, look, title, kills, gilded);
    }

    public MobTrophyRecord withGilded(boolean gilded) {
        return new MobTrophyRecord(entity, slayer, slainAt, rarity, look, title, kills, gilded);
    }

    public Component displayName() {
        return title.orElseGet(() -> gilded ? Component.translatable("trophypedestals.pedestal.gilded", mobName()) : mobName());
    }

    public Component slainLine() {
        return kills > 0 ? Component.translatable("trophypedestals.pedestal.slain_by_count", slayer, kills) : slainBy();
    }

    public @Nullable EntityType<?> type() {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(entity).orElse(null);
    }

    public Component mobName() {
        String given = look.read("CustomName", ComponentSerialization.CODEC).map(Component::getString).orElse("");
        if (!given.isBlank()) return Component.literal(given);
        EntityType<?> type = type();
        return type == null ? Component.literal(entity.toString()) : type.getDescription();
    }

    public ItemStack icon() {
        EntityType<?> type = type();
        if (type == Statues.FIGURE && look.contains("profile")) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            look.read("profile", ResolvableProfile.CODEC).ifPresent(profile -> head.set(DataComponents.PROFILE, profile));
            return head;
        }
        Optional<ItemStack> egg = type == null ? Optional.empty() : SpawnEggItem.byId(type).map(item -> new ItemStack(item.value()));
        return egg.orElseGet(() -> new ItemStack(Items.SPAWNER));
    }

    public boolean slain() {
        return !slayer.isBlank();
    }

    public Component slainBy() {
        return Component.translatable("trophypedestals.pedestal.slain_by", slayer);
    }

    public Component date() {
        DateTimeFormatter format = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withZone(ZoneId.systemDefault());
        return Component.literal(format.format(Instant.ofEpochMilli(slainAt)));
    }
}
