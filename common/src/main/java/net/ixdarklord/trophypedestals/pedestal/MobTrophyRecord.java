package net.ixdarklord.trophypedestals.pedestal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.coolcatcore.api.network.codec.ByteBufCodecs;
import net.ixdarklord.coolcatcore.api.network.codec.StreamCodec;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.compat.ResolvableProfile;
import net.ixdarklord.trophypedestals.compat.ValueInput;
import net.ixdarklord.trophypedestals.compat.component.Components;
import net.ixdarklord.trophypedestals.compat.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Optional;

/** What a mob trophy records: the mob slain, who slew it and when, and how grand the trophy looks. */
public record MobTrophyRecord(ResourceLocation entity, String slayer, long slainAt, ResourceLocation rarity, CompoundTag look, Optional<Component> title,
                              int kills, boolean gilded) {
    public static final Codec<MobTrophyRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("entity").forGetter(MobTrophyRecord::entity),
            Codec.STRING.optionalFieldOf("slayer", "").forGetter(MobTrophyRecord::slayer),
            Codec.LONG.fieldOf("slain_at").forGetter(MobTrophyRecord::slainAt),
            PedestalRarities.ID_CODEC.optionalFieldOf("rarity", PedestalRarities.COMMON.id()).forGetter(MobTrophyRecord::rarity),
            CompoundTag.CODEC.optionalFieldOf("look", new CompoundTag()).forGetter(MobTrophyRecord::look),
            ExtraCodecs.COMPONENT.optionalFieldOf("title").forGetter(MobTrophyRecord::title),
            Codec.INT.optionalFieldOf("kills", 0).forGetter(MobTrophyRecord::kills),
            Codec.BOOL.optionalFieldOf("gilded", false).forGetter(MobTrophyRecord::gilded)
    ).apply(instance, MobTrophyRecord::new));

    public static final StreamCodec<FriendlyByteBuf, MobTrophyRecord> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC).cast();

    public MobTrophyRecord(ResourceLocation entity, String slayer, long slainAt, ResourceLocation rarity) {
        this(entity, slayer, slainAt, rarity, new CompoundTag());
    }

    public MobTrophyRecord(ResourceLocation entity, String slayer, long slainAt, ResourceLocation rarity, CompoundTag look) {
        this(entity, slayer, slainAt, rarity, look, Optional.empty());
    }

    public MobTrophyRecord(ResourceLocation entity, String slayer, long slainAt, ResourceLocation rarity, CompoundTag look, Optional<Component> title) {
        this(entity, slayer, slainAt, rarity, look, title, 0, false);
    }

    public MobTrophyRecord withTitle(Optional<Component> title) {
        return new MobTrophyRecord(entity, slayer, slainAt, rarity, look, title, kills, gilded);
    }

    public MobTrophyRecord withKills(int kills, ResourceLocation rarity) {
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
        String given = ValueInput.of(look).readName("CustomName").map(Component::getString).orElse("");
        if (!given.isBlank()) return Component.literal(given);
        EntityType<?> type = type();
        return type == null ? Component.literal(entity.toString()) : type.getDescription();
    }

    public ItemStack icon() {
        EntityType<?> type = type();
        if (type == Statues.FIGURE && look.contains("profile")) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            ValueInput.of(look).read("profile", ResolvableProfile.CODEC).ifPresent(profile -> Components.set(head, DataComponents.PROFILE, profile));
            return head;
        }
        Optional<ItemStack> egg = type == null ? Optional.empty() : Optional.ofNullable(SpawnEggItem.byId(type)).map(ItemStack::new);
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
