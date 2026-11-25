package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class ItemModelResolver {
    public static final ItemModelResolver INSTANCE = new ItemModelResolver();

    private ItemModelResolver() {}

    public void updateForTopItem(ItemStackRenderState state, ItemStack stack, ItemDisplayContext context, @Nullable Level level,
                                 @Nullable LivingEntity entity, int seed) {
        state.clear();
        if (stack.isEmpty()) return;
        state.stack = stack;
        state.context = context;
        state.model = Minecraft.getInstance().getItemRenderer().getModel(stack, level, entity, seed);
    }
}
