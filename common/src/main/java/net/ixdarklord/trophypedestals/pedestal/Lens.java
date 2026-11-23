package net.ixdarklord.trophypedestals.pedestal;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * What a lens fitted to a display pedestal does to its showcase: the light's colour and the effects around the item.
 * A new lens needs one texture, {@code textures/item/<name>_lens.png}: a 16x16 lens whose glass fills at least columns
 * and rows 3 to 12. The item shows all of it, and the pedestal's bezel shows that middle part as the lens's glass.
 */
public enum Lens {
    /** Its item is a gold rim over a separate layer of glass, the part its dye tints. */
    CLEAR(0xFFF6E0, 0xFFFFFF, "clear_lens_glass"),
    SANGUINE(0xA3141F, 0xFF5A52),
    PRISM(0xFF7AD9, 0xFFFFFF),
    TIDE(0x3FD6C8, 0xB8FFF4),
    /** Its light and marks follow the rarity of the item shown. */
    APPRAISAL(0xF4F1E8, 0xFFFFFF),
    EMBER(0xFF7A1F, 0xFFD08A),
    FROST(0x8FD8FF, 0xE8F8FF),
    ENDER(0x7B34D1, 0xCFA0FF),
    HOLOGRAM(0x3FE8FF, 0xC8FAFF);

    /** The light's colour and the lighter one it fades to. */
    public final int color, accent;
    private final ResourceLocation glass;

    Lens(int color, int accent) {
        this(color, accent, null);
    }

    Lens(int color, int accent, @Nullable String glassTexture) {
        this.color = color;
        this.accent = accent;
        this.glass = TrophyPedestalsMod.id("textures/item/" + (glassTexture != null ? glassTexture : id()) + ".png");
    }

    /** The texture the pedestal's bezel shows this lens's glass from: its item's own. */
    public ResourceLocation glass() {
        return glass;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT) + "_lens";
    }

    public static @Nullable Lens of(ItemStack stack) {
        return stack.getItem() instanceof LensItem lens ? lens.lens() : null;
    }

    /** Whether a lens has been dyed. */
    public static boolean isDyed(ItemStack stack) {
        return stack.has(DataComponents.DYED_COLOR);
    }

    /** The colour of a dyed lens, white until it's dyed. */
    public static int dyed(ItemStack stack) {
        return DyedItemColor.getOrDefault(stack, 0xFFFFFFFF) & 0xFFFFFF;
    }
}
