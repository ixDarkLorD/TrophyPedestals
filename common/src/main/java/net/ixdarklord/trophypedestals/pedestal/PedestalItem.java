package net.ixdarklord.trophypedestals.pedestal;

import net.ixdarklord.trophypedestals.compat.component.Components;
import net.ixdarklord.trophypedestals.compat.component.DataComponents;
import net.ixdarklord.trophypedestals.compat.component.ItemContainerContents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        Consumer<Component> builder = lines::add;
        lensLine(stack, builder);
    }

    /** The line naming the lens a pedestal comes with. */
    static void lensLine(ItemStack stack, Consumer<Component> builder) {
        ItemContainerContents fitted = Components.get(stack, Pedestals.LENS.get());
        ItemStack lens = fitted == null ? ItemStack.EMPTY : fitted.copyOne();
        if (!lens.isEmpty()) {
            builder.accept(Component.translatable("trophypedestals.lens.fitted", lens.getHoverName()).withStyle(ChatFormatting.GRAY));
        }
    }
}
