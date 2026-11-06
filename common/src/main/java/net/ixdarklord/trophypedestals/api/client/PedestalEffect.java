package net.ixdarklord.trophypedestals.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.compat.render.CameraRenderState;
import net.ixdarklord.trophypedestals.compat.render.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * More effects for trophies of one rarity, on top of what its {@link PedestalRarity} properties draw. Registered with
 * {@link PedestalEffects#register}.
 */
public interface PedestalEffect {
    /**
     * Draws more, each frame, for every trophy of the rarity in view. The pose is the pedestal block's corner: the
     * block spans 0 to 1, the well's floor is 0.875 up and the icon turns about 1.3 up over its middle.
     *
     * @param time the world's game time in ticks, with the frame's partial tick
     */
    default void submit(PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, PedestalRarity rarity, float time) {}

    /** Each client tick while the player looks at a trophy of the rarity (and particles aren't set to Minimal). */
    default void tick(Level level, BlockPos pos, PedestalRarity rarity) {}
}
