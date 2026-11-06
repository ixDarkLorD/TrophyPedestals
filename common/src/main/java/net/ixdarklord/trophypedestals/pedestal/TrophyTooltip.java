package net.ixdarklord.trophypedestals.pedestal;

import net.ixdarklord.trophypedestals.compat.component.Components;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * A trophy's tooltip line under its name: what it's for (its achievement, or its mob) and its rank as a badge after it. The
 * game puts an item's tooltip image straight after its name; it's drawn on the client by {@code TrophyTooltipLine}.
 */
public record TrophyTooltip(ItemStack stack) implements TooltipComponent {
    /** The line for this stack, if it's a trophy (an achievement's or a mob's). */
    public static Optional<TooltipComponent> of(ItemStack stack) {
        return Components.has(stack, Pedestals.RECORD.get()) || Components.has(stack, Pedestals.MOB_TROPHY.get()) ? Optional.of(new TrophyTooltip(stack)) : Optional.empty();
    }
}
