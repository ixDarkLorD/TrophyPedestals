package net.ixdarklord.trophypedestals.client;

import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.pedestal.MobTrophyRecord;
import net.ixdarklord.trophypedestals.pedestal.PedestalRecord;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.ixdarklord.trophypedestals.pedestal.TrophyTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * A trophy's tooltip line: what it records (or its mob's name), then its rank as a badge in the rank's color, bevelled like
 * the mod's panels: a dark outer rim with its corners cut, a lit top and left edge, a shaded bottom and right edge, a
 * plate darkening downwards, and on it a small faceted gem and the rank's name in capitals, a pixel clear of the edges.
 * <pre>
 *  Stone Age  ╭─────────────╮
 *             │ ◆ UNCOMMON  │
 *             ╰─────────────╯
 * </pre>
 */
public final class TrophyTooltipLine implements ClientTooltipComponent {
    // The badge: its height, the gap between the name and it, where its gem sits from its left edge (and how wide it
    // is), where its text starts, and the room after its text.
    private static final int HEIGHT = 13, GAP = 4, GEM_X = 3, GEM_SIZE = 5, LABEL_X = GEM_X + GEM_SIZE + 2, PADDING = 3;
    // How far the line's text sits down: the badge's capitals (left plain, without a shadow) have a pixel of plate above
    // and below them, inside its bevel; the name lines up with them.
    private static final int TEXT_Y = 3;
    // How far the line is drawn up into the gap the tooltip leaves under its title, and kept clear under it, so it sits
    // close and even between the lines over and under it.
    private static final int RAISE = 2, BELOW = 1;

    private final Component name;
    private final @Nullable PedestalRarity rarity;
    private final @Nullable FormattedCharSequence label;

    public TrophyTooltipLine(TrophyTooltip tooltip) {
        PedestalRecord record = tooltip.stack().get(Pedestals.RECORD.get());
        MobTrophyRecord mob = tooltip.stack().get(Pedestals.MOB_TROPHY.get());
        if (record != null) {
            name = PedestalRenderer.name(record);
            rarity = PedestalRarities.isNone(record.rarity()) ? null : PedestalRarities.get(record.rarity());
        } else if (mob != null) {
            name = mob.mobName();
            rarity = PedestalRarities.isNone(mob.rarity()) ? null : PedestalRarities.get(mob.rarity());
        } else {
            name = Component.empty();
            rarity = null;
        }
        label = rarity == null ? null : Component.literal(rarity.displayName().getString().toUpperCase(Locale.ROOT)).getVisualOrderText();
    }

    @Override
    public int getHeight(Font font) {
        return HEIGHT - RAISE + BELOW;
    }

    @Override
    public int getWidth(Font font) {
        int width = font.width(name);
        return label == null ? width : width + GAP + badgeWidth(font);
    }

    private int badgeWidth(Font font) {
        return LABEL_X + font.width(label) - 1 + PADDING;
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        y -= RAISE;
        graphics.text(font, name, x, y + TEXT_Y, 0xFFFFFFFF, true);
        if (rarity == null || label == null) return;
        int color = ARGB.opaque(rarity.color());
        int x0 = x + font.width(name) + GAP, x1 = x0 + badgeWidth(font), y0 = y, y1 = y + HEIGHT;
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
        graphics.text(font, label, x0 + LABEL_X, y0 + TEXT_Y, ARGB.srgbLerp(0.45F, color, 0xFFFFFFFF), false);
    }

    // The gem: a diamond five pixels across, lit from the upper left. Each letter is a shade: H highlight, W its white
    // glint, L light, C the rank's color, D deep.
    private static final String[] GEM = {
            "  H  ",
            " HLC ",
            "HWLCD",
            " LCD ",
            "  D  "};

    private static void gem(GuiGraphicsExtractor graphics, int x, int y, int color) {
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
