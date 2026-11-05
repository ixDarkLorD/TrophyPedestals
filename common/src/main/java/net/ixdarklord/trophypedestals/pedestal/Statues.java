package net.ixdarklord.trophypedestals.pedestal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;

/**
 * A player's statue on a pedestal: a mannequin (vanilla's player-shaped figure) wearing their skin, as the look of a
 * mob trophy ({@link MobLooks}). A tier's trophy unlocked by a player can show theirs in place of its icon, and a
 * player another player kills can drop a trophy of theirs.
 */
public final class Statues {
    /** The figure a statue is: vanilla's mannequin. */
    public static final EntityType<?> FIGURE = EntityType.MANNEQUIN;

    private Statues() {}

    /** A statue of the player by this name: their skin's looked up as the statue is drawn (the default skin until then). */
    public static CompoundTag look(String name) {
        return look(ResolvableProfile.createUnresolved(name));
    }

    /** A statue of this player, wearing the skin they have now, named after them. */
    public static CompoundTag look(Player player) {
        CompoundTag look = look(ResolvableProfile.createResolved(player.getGameProfile()));
        look.putString("CustomName", player.getName().getString());
        return look;
    }

    private static CompoundTag look(ResolvableProfile profile) {
        CompoundTag look = new CompoundTag();
        Tag encoded = ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, profile).result().orElse(null);
        if (encoded != null) look.put("profile", encoded);
        // Shown without the mannequin's own label, and standing still.
        look.putBoolean("hide_description", true);
        look.putBoolean("immovable", true);
        return look;
    }
}
