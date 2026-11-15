package net.ixdarklord.trophypedestals.pedestal;

import net.ixdarklord.trophypedestals.compat.component.Components;
import net.ixdarklord.trophypedestals.compat.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/** A pedestal's item. */
public class PedestalItem extends BlockItem {
    public PedestalItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Component named = Components.get(stack, DataComponents.ITEM_NAME);
        return named != null ? named : super.getName(stack);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return TrophyTooltip.of(stack);
    }
}
