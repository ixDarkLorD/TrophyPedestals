package net.ixdarklord.trophypedestals.api;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * What a kind of trophy records, told by the mod it comes from: the name and icon of a trophy's subject (a tier, a
 * quest, an achievement of a mod's own), registered with {@link TrophyKinds}. A trophy records its kind and subject by
 * id ({@link net.ixdarklord.trophypedestals.pedestal.PedestalRecord}); placed, it shows the icon turning over it and
 * the name on its text frame, in its rank's looks.
 *
 * <p>Asked on both sides: {@code client} is true on the client (rendering the trophy), false on the server (naming the
 * item it's made as). The client's methods run every frame a trophy is drawn, so keep them cheap.
 */
public interface TrophyKind {
    /** The subject's name, as its trophy shows it ("Iron Age"). */
    Component name(ResourceLocation subject, boolean client);

    /** The color of its name on the trophy's text frame (RGB). White by default. */
    default int color(ResourceLocation subject, boolean client) {
        return 0xFFFFFF;
    }

    /** What its trophy shows turning over it. Empty shows nothing. */
    ItemStack icon(ResourceLocation subject, boolean client);

    /** The rank its trophies get unless one is named, or null for common. Asked on the server. */
    default @Nullable PedestalRarity rarity(ResourceLocation subject) {
        return null;
    }
}
