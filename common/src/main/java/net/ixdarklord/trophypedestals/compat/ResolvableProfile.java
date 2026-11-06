package net.ixdarklord.trophypedestals.compat;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 1.20.1's stand-in for 26.1's {@code ResolvableProfile}: a player by name (to be looked up) or by their whole profile,
 * saved in the same shape ({@code name}, {@code id}, {@code properties}).
 */
public record ResolvableProfile(Optional<String> name, Optional<UUID> id, List<Property> properties) {
    private static final Codec<Property> PROPERTY = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(Property::getName),
            Codec.STRING.fieldOf("value").forGetter(Property::getValue),
            Codec.STRING.optionalFieldOf("signature").forGetter(property -> Optional.ofNullable(property.getSignature()))
    ).apply(instance, (name, value, signature) -> new Property(name, value, signature.orElse(null))));

    public static final Codec<ResolvableProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.optionalFieldOf("name").forGetter(ResolvableProfile::name),
            UUIDUtil.CODEC.optionalFieldOf("id").forGetter(ResolvableProfile::id),
            PROPERTY.listOf().optionalFieldOf("properties", List.of()).forGetter(ResolvableProfile::properties)
    ).apply(instance, ResolvableProfile::new));

    /** A player by name, looked up when they're shown. */
    public static ResolvableProfile createUnresolved(String name) {
        return new ResolvableProfile(Optional.of(name), Optional.empty(), List.of());
    }

    public static ResolvableProfile createResolved(GameProfile profile) {
        return new ResolvableProfile(Optional.ofNullable(profile.getName()), Optional.ofNullable(profile.getId()),
                List.copyOf(profile.getProperties().values()));
    }

    /** The profile as it is (a name-only one has no id and no skin). */
    public GameProfile gameProfile() {
        GameProfile profile = new GameProfile(id.orElse(Util.NIL_UUID), name.orElse(""));
        for (Property property : properties) profile.getProperties().put(property.getName(), property);
        return profile;
    }

    /** The whole profile, looked up (by name or id) if it isn't whole yet. */
    public CompletableFuture<ResolvableProfile> resolve() {
        if (!properties.isEmpty() || id.isPresent() == name.isPresent()) return CompletableFuture.completedFuture(this);
        CompletableFuture<ResolvableProfile> resolved = new CompletableFuture<>();
        SkullBlockEntity.updateGameprofile(gameProfile(), found -> resolved.complete(found == null ? this : createResolved(found)));
        return resolved;
    }
}
