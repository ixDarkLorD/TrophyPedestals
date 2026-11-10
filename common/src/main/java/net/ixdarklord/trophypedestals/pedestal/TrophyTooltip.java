package net.ixdarklord.trophypedestals.pedestal;

import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.compat.component.Components;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * A ranked trophy's rank, shown as a badge after its name in its tooltip. The game puts an item's tooltip image straight
 * after its name; it's drawn on the client by {@code TrophyTooltipLine}.
 */
public record TrophyTooltip(ItemStack stack) implements TooltipComponent {
    /** The badge for this stack, if it's a trophy (an achievement's or a mob's) with a rank. */
    public static Optional<TooltipComponent> of(ItemStack stack) {
        PedestalRecord record = Components.get(stack, Pedestals.RECORD.get());
        MobTrophyRecord mob = Components.get(stack, Pedestals.MOB_TROPHY.get());
        boolean ranked = record != null ? !PedestalRarities.isNone(record.rarity()) : mob != null && !PedestalRarities.isNone(mob.rarity());
        return ranked ? Optional.of(new TrophyTooltip(stack)) : Optional.empty();
    }
}
