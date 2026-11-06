package net.ixdarklord.trophypedestals.pedestal;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * The custom pedestal's item, named after the block it's made of ("Amethyst Block Pedestal"), or just "Pedestal" with no
 * material. A trophy keeps its own name ("Diamond Age Trophy").
 */
public class CustomPedestalItem extends PedestalItem {
    public CustomPedestalItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        // A name of its own (a trophy's) comes first.
        if (stack.has(DataComponents.ITEM_NAME)) return super.getName(stack);
        Block material = material(stack);
        return material == null ? super.getName(stack) : Component.translatable("block.trophypedestals.custom_pedestal.named", material.getName());
    }

    /** The block a custom pedestal item is made of, or null for none, or one that's no longer in the game. */
    public static @Nullable Block material(ItemStack stack) {
        ResourceLocation id = stack.get(Pedestals.PEDESTAL_MATERIAL.get());
        return id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
    }
}
