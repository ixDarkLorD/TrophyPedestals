package net.ixdarklord.trophypedestals.pedestal;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeableLeatherItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum Lens {
    CLEAR(0xFFF6E0, 0xFFFFFF, "clear_lens_glass"),
    SANGUINE(0xA3141F, 0xFF5A52, "blood_moon_lens"),
    PRISM(0xFF7AD9, 0xFFFFFF),
    TIDE(0x3FD6C8, 0xB8FFF4, "coral_reef_lens"),
    APPRAISAL(0xF4F1E8, 0xFFFFFF, "curators_lens"),
    EMBER(0xFF7A1F, 0xFFD08A, "inferno_lens"),
    FROST(0x8FD8FF, 0xE8F8FF, "blizzard_lens"),
    ENDER(0x7B34D1, 0xCFA0FF, "void_rift_lens"),
    HOLOGRAM(0x3FE8FF, 0xC8FAFF, "cipher_lens");

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

    public ResourceLocation glass() {
        return glass;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT) + "_lens";
    }

    public static @Nullable Lens of(ItemStack stack) {
        return stack.getItem() instanceof LensItem lens ? lens.lens() : null;
    }

    public static boolean isDyed(ItemStack stack) {
        return stack.getItem() instanceof DyeableLeatherItem dyeable && dyeable.hasCustomColor(stack);
    }

    public static int dyed(ItemStack stack) {
        return isDyed(stack) ? ((DyeableLeatherItem) stack.getItem()).getColor(stack) & 0xFFFFFF : 0xFFFFFF;
    }
}
