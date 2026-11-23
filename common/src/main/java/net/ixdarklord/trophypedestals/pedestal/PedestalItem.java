package net.ixdarklord.trophypedestals.pedestal;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** A pedestal's item. */
public class PedestalItem extends BlockItem {
    public PedestalItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return TrophyTooltip.of(stack);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        Consumer<Component> builder = lines::add;
        lensLine(stack, builder);
    }

    /** The line naming the lens a pedestal comes with. */
    static void lensLine(ItemStack stack, Consumer<Component> builder) {
        ItemContainerContents fitted = stack.get(Pedestals.LENS.get());
        ItemStack lens = fitted == null ? ItemStack.EMPTY : fitted.copyOne();
        if (!lens.isEmpty()) {
            builder.accept(Component.translatable("trophypedestals.lens.fitted", lens.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
    }
}
