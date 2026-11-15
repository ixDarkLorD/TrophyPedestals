package net.ixdarklord.trophypedestals.pedestal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.coolcatcore.api.network.codec.ByteBufCodecs;
import net.ixdarklord.coolcatcore.api.network.codec.StreamCodec;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;

/**
 * What a trophy pedestal records: what was achieved (its {@code kind}, a
 * {@link net.ixdarklord.trophypedestals.api.TrophyKind} another mod registers, such as {@code tierwise:tier}, and its
 * {@code subject}, such as the tier's id), who achieved it (a player, a team, or the whole server), when, and its rank.
 */
public record PedestalRecord(ResourceLocation kind, ResourceLocation subject, Unlocker unlocker, String name, long unlockedAt, ResourceLocation rarity,
                             Optional<Component> title, Optional<ResourceLocation> icon) {
    public enum Unlocker implements StringRepresentable {
        PLAYER, TEAM, SERVER;

        public static final Codec<Unlocker> CODEC = StringRepresentable.fromEnum(Unlocker::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final Codec<PedestalRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("kind").forGetter(PedestalRecord::kind),
            ResourceLocation.CODEC.fieldOf("subject").forGetter(PedestalRecord::subject),
            Unlocker.CODEC.fieldOf("unlocker").forGetter(PedestalRecord::unlocker),
            Codec.STRING.optionalFieldOf("name", "").forGetter(PedestalRecord::name),
            Codec.LONG.fieldOf("unlocked_at").forGetter(PedestalRecord::unlockedAt),
            PedestalRarities.ID_CODEC.optionalFieldOf("rarity", PedestalRarities.COMMON.id()).forGetter(PedestalRecord::rarity),
            ExtraCodecs.COMPONENT.optionalFieldOf("title").forGetter(PedestalRecord::title),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(PedestalRecord::icon)
    ).apply(instance, PedestalRecord::new));

    public static final StreamCodec<FriendlyByteBuf, PedestalRecord> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC).cast();

    public PedestalRecord(ResourceLocation kind, ResourceLocation subject, Unlocker unlocker, String name, long unlockedAt, ResourceLocation rarity) {
        this(kind, subject, unlocker, name, unlockedAt, rarity, Optional.empty(), Optional.empty());
    }

    public PedestalRecord withTitle(Optional<Component> title) {
        return new PedestalRecord(kind, subject, unlocker, name, unlockedAt, rarity, title, icon);
    }

    public Component unlockedBy() {
        return unlocker == Unlocker.SERVER
                ? Component.translatable("trophypedestals.pedestal.unlocked_by_server")
                : Component.translatable("trophypedestals.pedestal.unlocked_by", name);
    }

    public Component date() {
        DateTimeFormatter format = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withZone(ZoneId.systemDefault());
        return Component.literal(format.format(Instant.ofEpochMilli(unlockedAt)));
    }
}
