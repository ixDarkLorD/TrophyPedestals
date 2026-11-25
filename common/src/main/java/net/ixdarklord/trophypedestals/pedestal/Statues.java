package net.ixdarklord.trophypedestals.pedestal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;

public final class Statues {
    public static final EntityType<?> FIGURE = EntityType.MANNEQUIN;

    private Statues() {}

    public static CompoundTag look(String name) {
        return look(ResolvableProfile.createUnresolved(name));
    }

    public static CompoundTag look(Player player) {
        CompoundTag look = look(ResolvableProfile.createResolved(player.getGameProfile()));
        look.putString("CustomName", player.getName().getString());
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
