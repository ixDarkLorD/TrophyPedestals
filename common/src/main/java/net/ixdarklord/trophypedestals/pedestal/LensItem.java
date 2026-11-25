package net.ixdarklord.trophypedestals.pedestal;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class LensItem extends Item {
    private final Lens lens;

    public LensItem(Lens lens, Properties properties) {
        super(properties);
        this.lens = lens;
    }

    public Lens lens() {
        return lens;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.trophypedestals." + lens.id() + ".desc").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("trophypedestals.lens.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
