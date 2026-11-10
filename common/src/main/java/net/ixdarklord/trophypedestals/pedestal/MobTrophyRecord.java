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

/**
 * What a mob trophy records: the mob slain, who slew it and when, and how grand the trophy looks. Kept on the pedestal
 * item as the {@code trophypedestals:mob_trophy} data component, and on the placed block's entity, which shows the mob turning
 * over the pedestal.
 *
 * @param entity  the mob's entity type
 * @param slayer  the player's name
 * @param slainAt when, in milliseconds since the epoch
 * @param rarity  its rarity's id ({@link PedestalRarities})
 * @param look    how the mob looked ({@link MobLooks}), or empty to show it as it spawns
 * @param title   the name it goes by in place of its mob's ("First Wither Slain", an engraved name), if it has one
 * @param kills   how many of its mob its slayer has killed, counted while it's placed (0 before it's counted)
 * @param gilded  whether it's a gilded trophy: a rare one, on a gold pedestal
 */
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

    /** A record of a mob shown as it spawns. */
    public MobTrophyRecord(Identifier entity, String slayer, long slainAt, Identifier rarity) {
        this(entity, slayer, slainAt, rarity, new CompoundTag());
    }

    /** A record going by its mob's name. */
    public MobTrophyRecord(Identifier entity, String slayer, long slainAt, Identifier rarity, CompoundTag look) {
        this(entity, slayer, slainAt, rarity, look, Optional.empty());
    }

    /** A record not yet counted, and not gilded. */
    public MobTrophyRecord(Identifier entity, String slayer, long slainAt, Identifier rarity, CompoundTag look, Optional<Component> title) {
        this(entity, slayer, slainAt, rarity, look, title, 0, false);
    }

    /** The same record with this title (or none: its mob's name). */
    public MobTrophyRecord withTitle(Optional<Component> title) {
        return new MobTrophyRecord(entity, slayer, slainAt, rarity, look, title, kills, gilded);
    }

    /** The same record counting this many kills, at this rank. */
    public MobTrophyRecord withKills(int kills, Identifier rarity) {
        return new MobTrophyRecord(entity, slayer, slainAt, rarity, look, title, kills, gilded);
    }

    /** The same record, gilded or not. */
    public MobTrophyRecord withGilded(boolean gilded) {
        return new MobTrophyRecord(entity, slayer, slainAt, rarity, look, title, kills, gilded);
    }

    /** The name it goes by: its title, or its mob's name ("Gilded Zombie" for a gilded one). */
    public Component displayName() {
        return title.orElseGet(() -> gilded ? Component.translatable("trophypedestals.pedestal.gilded", mobName()) : mobName());
    }

    /** "Slain by Steve", or "Slain by Steve (152 slain)" once its slayer's kills of its mob are counted. */
    public Component slainLine() {
        return kills > 0 ? Component.translatable("trophypedestals.pedestal.slain_by_count", slayer, kills) : slainBy();
    }

    /** The mob's entity type, or null if it's no longer in the game (a removed mod's mob). */
    public @Nullable EntityType<?> type() {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(entity).orElse(null);
    }

    /** The mob's name: the one it was given with a name tag, its kind's ("Zombie"), or its id if it's no longer in the game. */
    public Component mobName() {
        // (As plain text: a name's own styling and click actions stay off the trophy.)
        String given = look.read("CustomName", ComponentSerialization.CODEC).map(Component::getString).orElse("");
        if (!given.isBlank()) return Component.literal(given);
        EntityType<?> type = type();
        return type == null ? Component.literal(entity.toString()) : type.getDescription();
    }

    /**
     * The mob's icon, for where the mob itself isn't drawn (inventory slots, or a mob that can't be drawn outside a world):
     * its spawn egg, or a spawner for a mob with none (a boss, many mods' mobs) or no longer in the game.
     */
    public ItemStack icon() {
        EntityType<?> type = type();
        // A player's statue: their head.
        if (type == Statues.FIGURE && look.contains("profile")) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            look.read("profile", ResolvableProfile.CODEC).ifPresent(profile -> head.set(DataComponents.PROFILE, profile));
            return head;
        }
        Optional<ItemStack> egg = type == null ? Optional.empty() : SpawnEggItem.byId(type).map(item -> new ItemStack(item.value()));
        return egg.orElseGet(() -> new ItemStack(Items.SPAWNER));
    }

    /** "Slain by Steve". */
    /** Whether it records a kill: a creative tab's trophy (no slayer) shows neither who slew it nor when. */
    public boolean slain() {
        return !slayer.isBlank();
    }

    public Component slainBy() {
        return Component.translatable("trophypedestals.pedestal.slain_by", slayer);
    }

    /** When it was slain, in the reader's own date and time format and time zone. */
    public Component date() {
        DateTimeFormatter format = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withZone(ZoneId.systemDefault());
        return Component.literal(format.format(Instant.ofEpochMilli(slainAt)));
    }
}
