package net.ixdarklord.trophypedestals.pedestal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.minecraft.network.chat.Component;
import net.ixdarklord.coolcatcore.api.network.codec.ByteBufCodecs;
import net.ixdarklord.coolcatcore.api.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

/**
 * What a trophy pedestal records: what was achieved (its {@code kind}, a {@link net.ixdarklord.trophypedestals.api.TrophyKind}
 * another mod registers, such as {@code tierwise:tier}, and its {@code subject}, such as the tier's id), who achieved it
 * (a player, a team, or the whole server), when, and its rank. Kept on the pedestal item as a data component, and on
 * the placed block's entity.
 *
 * @param name       the player's or team's name when it was unlocked (empty for the server)
 * @param unlockedAt when, in milliseconds since the epoch
 * @param rarity     its rarity's id ({@link PedestalRarities}); trophies from before rarities are common
 */
public record PedestalRecord(ResourceLocation kind, ResourceLocation subject, Unlocker unlocker, String name, long unlockedAt, ResourceLocation rarity) {
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
            PedestalRarities.ID_CODEC.optionalFieldOf("rarity", PedestalRarities.COMMON.id()).forGetter(PedestalRecord::rarity)
    ).apply(instance, PedestalRecord::new));

    public static final StreamCodec<ByteBuf, PedestalRecord> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.RESOURCE_LOCATION, PedestalRecord::kind,
            ByteBufCodecs.RESOURCE_LOCATION, PedestalRecord::subject,
            ByteBufCodecs.VAR_INT.map(index -> Unlocker.values()[index], Unlocker::ordinal), PedestalRecord::unlocker,
            ByteBufCodecs.STRING_UTF8, PedestalRecord::name,
            ByteBufCodecs.VAR_LONG, PedestalRecord::unlockedAt,
            ByteBufCodecs.RESOURCE_LOCATION, PedestalRecord::rarity,
            PedestalRecord::new);

    /** "Unlocked by Steve", "Unlocked by Team Red", or "Unlocked by the whole server". */
    public Component unlockedBy() {
        return unlocker == Unlocker.SERVER
                ? Component.translatable("trophypedestals.pedestal.unlocked_by_server")
                : Component.translatable("trophypedestals.pedestal.unlocked_by", name);
    }

    /** When it was unlocked, in the reader's own date and time format and time zone. */
    public Component date() {
        DateTimeFormatter format = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withZone(ZoneId.systemDefault());
        return Component.literal(format.format(Instant.ofEpochMilli(unlockedAt)));
    }
}
