package net.ixdarklord.trophypedestals.pedestal;

import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.Optional;

/**
 * A player's statue on a pedestal: a mannequin (vanilla's player-shaped figure) wearing their skin, as the look of a
 * mob trophy ({@link MobLooks}).
 */
public final class Statues {
    public static final EntityType<?> FIGURE = EntityType.PLAYER;

    private Statues() {}

    public static CompoundTag look(String name) {
        return look(new ResolvableProfile(Optional.of(name), Optional.empty(), new PropertyMap()));
    }

    public static CompoundTag look(Player player) {
        CompoundTag look = look(new ResolvableProfile(player.getGameProfile()));
        look.putString("CustomName", Component.Serializer.toJson(Component.literal(player.getName().getString()), RegistryAccess.EMPTY));
        return look;
    }

    private static CompoundTag look(ResolvableProfile profile) {
        CompoundTag look = new CompoundTag();
        Tag encoded = ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, profile).result().orElse(null);
        if (encoded != null) look.put("profile", encoded);
        look.putBoolean("hide_description", true);
        look.putBoolean("immovable", true);
        return look;
    }
}
