package net.ixdarklord.trophypedestals.pedestal;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * The custom pedestal's item, named after the block it's made of ("Amethyst Block Pedestal"), or just "Pedestal" with
 * no material.
 */
public class CustomPedestalItem extends PedestalItem {
    public CustomPedestalItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (stack.hasNonDefault(DataComponents.ITEM_NAME)) return super.getName(stack);
        Block material = material(stack);
        return material == null ? super.getName(stack) : Component.translatable("block.trophypedestals.custom_pedestal.named", material.getName());
    }

    public static @Nullable Block material(ItemStack stack) {
        Identifier id = stack.get(Pedestals.PEDESTAL_MATERIAL.get());
        return id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
    }
}
