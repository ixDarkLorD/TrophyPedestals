package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 1.20.1's stand-in for 26.1's {@code BlockEntityRenderState}: what every block entity's drawing starts from. 1.20.1
 * draws a block entity in one call, so the renderers here fill a state and draw it straight away (light from that call).
 */
public class BlockEntityRenderState {
    public BlockPos blockPos = BlockPos.ZERO;
    public BlockState blockState = Blocks.AIR.defaultBlockState();
    public int lightCoords;

    public static void extractBase(BlockEntity blockEntity, BlockEntityRenderState state, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        state.blockPos = blockEntity.getBlockPos();
        state.blockState = blockEntity.getBlockState();
    }
}
