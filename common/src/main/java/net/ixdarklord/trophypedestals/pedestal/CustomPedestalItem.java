package net.ixdarklord.trophypedestals.pedestal;

import net.ixdarklord.trophypedestals.compat.component.Components;
import net.ixdarklord.trophypedestals.compat.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public class CustomPedestalItem extends PedestalItem {
    public CustomPedestalItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (Components.has(stack, DataComponents.ITEM_NAME)) return super.getName(stack);
        Block material = material(stack);
        return material == null ? super.getName(stack) : Component.translatable("block.trophypedestals.custom_pedestal.named", material.getName());
    }

    public static @Nullable Block material(ItemStack stack) {
        ResourceLocation id = Components.get(stack, Pedestals.PEDESTAL_MATERIAL.get());
        return id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
    }
}
