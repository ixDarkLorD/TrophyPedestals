package net.ixdarklord.trophypedestals.pedestal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;

/**
 * What a trophy pedestal records: what was achieved (its {@code kind}, a {@link net.ixdarklord.trophypedestals.api.TrophyKind}
 * another mod registers, such as {@code tierwise:tier}, and its {@code subject}, such as the tier's id), who achieved it
 * (a player, a team, or the whole server), when, and its rank. Kept on the pedestal item as a data component, and on
 * the placed block's entity.
 *
 * @param name       the player's or team's name when it was unlocked (empty for the server)
 * @param unlockedAt when, in milliseconds since the epoch
 * @param rarity     its rarity's id ({@link PedestalRarities}); trophies from before rarities are common
 * @param title      the name it shows in place of its kind's name for its subject (a data pack's trophy, an
 *                   advancement's title, an engraved name), if it has one
 * @param icon       the item it shows turning over it in place of its kind's icon, if it has one
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
            ComponentSerialization.CODEC.optionalFieldOf("title").forGetter(PedestalRecord::title),
            ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(PedestalRecord::icon)
    ).apply(instance, PedestalRecord::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PedestalRecord> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);

    /** A record with its kind's own name and icon for its subject. */
    public PedestalRecord(ResourceLocation kind, ResourceLocation subject, Unlocker unlocker, String name, long unlockedAt, ResourceLocation rarity) {
        this(kind, subject, unlocker, name, unlockedAt, rarity, Optional.empty(), Optional.empty());
    }

    /** The same record with this title (or none: its kind's name). */
    public PedestalRecord withTitle(Optional<Component> title) {
        return new PedestalRecord(kind, subject, unlocker, name, unlockedAt, rarity, title, icon);
    }

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
