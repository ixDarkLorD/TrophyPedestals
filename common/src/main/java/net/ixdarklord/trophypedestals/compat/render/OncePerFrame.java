package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.WeakHashMap;

/** Draws a block entity once a frame. */
public final class OncePerFrame {
    private static final Map<BlockEntity, Long> DRAWN = new WeakHashMap<>();

    private OncePerFrame() {}

    public static boolean first(BlockEntity blockEntity, float partialTick) {
        long frame = Minecraft.getInstance().getFrameTimeNs() * 31 + Float.floatToRawIntBits(partialTick);
        Long last = DRAWN.put(blockEntity, frame);
        return last == null || last != frame;
    }
}
