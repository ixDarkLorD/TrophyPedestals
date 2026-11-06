package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws a block entity once a frame. 1.21.1 lists a block entity whose renderer draws off-screen both with its chunk
 * and with the level's global ones, and draws both lists every frame: drawn twice, a pedestal's light (added to the
 * scene) would be twice as bright as on 26.1, which draws it once.
 */
public final class OncePerFrame {
    private static final Map<BlockEntity, Long> DRAWN = new WeakHashMap<>();

    private OncePerFrame() {}

    /** Whether this is the block entity's first draw this frame (and so whether to draw it). */
    public static boolean first(BlockEntity blockEntity, float partialTick) {
        // The last frame's length in nanoseconds, with the partial tick: the same all through one frame's drawing, and
        // all but never the same from one frame to the next.
        long frame = Minecraft.getInstance().getFrameTimeNs() * 31 + Float.floatToRawIntBits(partialTick);
        Long last = DRAWN.put(blockEntity, frame);
        return last == null || last != frame;
    }
}
