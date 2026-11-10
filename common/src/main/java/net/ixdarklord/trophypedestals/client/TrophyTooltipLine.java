package net.ixdarklord.trophypedestals.client;

import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.compat.ARGB;
import net.ixdarklord.trophypedestals.pedestal.MobTrophyRecord;
import net.ixdarklord.trophypedestals.pedestal.PedestalRecord;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.ixdarklord.trophypedestals.pedestal.TrophyTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * A ranked trophy's rank, as a badge after its name on the tooltip's first line, in the rank's color and bevelled like
 * the mod's panels: a dark outer rim with its corners cut, a lit top and left edge, a shaded bottom and right edge, a
 * plate darkening downwards, and on it a small faceted gem and the rank's name in capitals.
 * <pre>
 *  Iron Age Trophy ╭─────────╮
 *                  │ ◆ RARE  │
 *                  ╰─────────╯
 * </pre>
 * The game puts it under the name, so it takes no height there: it's drawn back up onto the name's line, and widens the
 * tooltip to fit it there.
 */
public final class TrophyTooltipLine implements ClientTooltipComponent {
    // The badge: its height, the gap between the name and it, where its gem sits from its left edge (and how wide it
    // is), where its text starts, and the room after its text.
    private static final int HEIGHT = 11, GAP = 4, GEM_X = 3, GEM_SIZE = 5, LABEL_X = GEM_X + GEM_SIZE + 2, PADDING = 3;
    // How far the badge's capitals (plain, without a shadow) sit down inside it, just under its lit edge.
    private static final int TEXT_Y = 2;
    // How far up the name's line is: the name's own height and the gap the game leaves under it.
    private static final int NAME_LINE = 10 + 2;
    // How far the badge's top sits above the name's: a pixel, clear of the tooltip's frame, so its capitals sit a pixel
    // under the name's and line up with it and its shadow.
    private static final int ABOVE_NAME = 1;

    private final Component name;
    private final @Nullable PedestalRarity rarity;
    private final @Nullable FormattedCharSequence label;

    public TrophyTooltipLine(TrophyTooltip tooltip) {
        ItemStack stack = tooltip.stack();
        PedestalRecord record = stack.get(Pedestals.RECORD.get());
        MobTrophyRecord mob = stack.get(Pedestals.MOB_TROPHY.get());
        // (The name as the game shows it on the first line.)
        name = stack.getHoverName().copy().withStyle(stack.getRarity().color());
        ResourceLocation id = record != null ? record.rarity() : mob != null ? mob.rarity() : null;
        rarity = id == null || PedestalRarities.isNone(id) ? null : PedestalRarities.get(id);
        label = rarity == null ? null : Component.literal(rarity.displayName().getString().toUpperCase(Locale.ROOT)).getVisualOrderText();
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getWidth(Font font) {
        return label == null ? 0 : font.width(name) + GAP + badgeWidth(font);
    }

    private int badgeWidth(Font font) {
        return LABEL_X + font.width(label) - 1 + PADDING;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        if (rarity == null || label == null) return;
        int color = ARGB.opaque(rarity.color());
        int x0 = x + font.width(name) + GAP, x1 = x0 + badgeWidth(font), y0 = y - NAME_LINE - ABOVE_NAME, y1 = y0 + HEIGHT;
        int rim = ARGB.scaleRGB(color, 0.22F), lit = ARGB.scaleRGB(color, 0.62F), shade = ARGB.scaleRGB(color, 0.34F);
        // The outer rim, its corners left out.
        graphics.fill(x0 + 1, y0, x1 - 1, y0 + 1, rim);
        graphics.fill(x0 + 1, y1 - 1, x1 - 1, y1, rim);
        graphics.fill(x0, y0 + 1, x0 + 1, y1 - 1, rim);
        graphics.fill(x1 - 1, y0 + 1, x1, y1 - 1, rim);
        // The bevel: lit along the top and left, shaded along the bottom and right.
        graphics.fill(x0 + 1, y0 + 1, x1 - 1, y0 + 2, lit);
        graphics.fill(x0 + 1, y0 + 2, x0 + 2, y1 - 1, lit);
        graphics.fill(x0 + 2, y1 - 2, x1 - 1, y1 - 1, shade);
        graphics.fill(x1 - 2, y0 + 2, x1 - 1, y1 - 2, shade);
        // The plate, darkening downwards.
        graphics.fillGradient(x0 + 2, y0 + 2, x1 - 2, y1 - 2, ARGB.color(245, ARGB.scaleRGB(color, 0.40F)), ARGB.color(245, ARGB.scaleRGB(color, 0.20F)));
        gem(graphics, x0 + GEM_X, y0 + (HEIGHT - GEM_SIZE) / 2, color);
        graphics.drawString(font, label, x0 + LABEL_X, y0 + TEXT_Y, ARGB.srgbLerp(0.45F, color, 0xFFFFFFFF), false);
    }

    // The gem: a diamond five pixels across, lit from the upper left. Each letter is a shade: H highlight, W its white
    // glint, L light, C the rank's color, D deep.
    private static final String[] GEM = {
            "  H  ",
            " HLC ",
            "HWLCD",
            " LCD ",
            "  D  "};

    private static void gem(GuiGraphics graphics, int x, int y, int color) {
        for (int row = 0; row < GEM.length; row++) {
            for (int column = 0; column < GEM[row].length(); column++) {
                int pixel = switch (GEM[row].charAt(column)) {
                    case 'H' -> ARGB.srgbLerp(0.6F, color, 0xFFFFFFFF);
                    case 'W' -> 0xFFFFFFFF;
                    case 'L' -> ARGB.srgbLerp(0.3F, color, 0xFFFFFFFF);
                    case 'C' -> color;
                    case 'D' -> ARGB.scaleRGB(color, 0.55F);
                    default -> 0;
                };
                if (pixel != 0) graphics.fill(x + column, y + row, x + column + 1, y + row + 1, pixel);
            }
        }
    }
}
