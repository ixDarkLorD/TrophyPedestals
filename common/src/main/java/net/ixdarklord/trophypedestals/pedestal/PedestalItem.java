package net.ixdarklord.trophypedestals.pedestal;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * A pedestal's item. A ranked trophy's tooltip shows its rank as a badge after its name ({@link TrophyTooltip}).
 */
public class PedestalItem extends BlockItem {
    public PedestalItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return TrophyTooltip.of(stack);
    }
}
