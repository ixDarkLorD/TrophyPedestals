package net.ixdarklord.trophypedestals.compat.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** 1.21.1's stand-in for 26.1's {@code BlockEntityRenderState}: what every block entity's drawing starts from. */
public class BlockEntityRenderState {
    public BlockPos blockPos = BlockPos.ZERO;
    public BlockState blockState = Blocks.AIR.defaultBlockState();
    public int lightCoords;
    public ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress;

    public static void extractBase(BlockEntity blockEntity, BlockEntityRenderState state, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        state.blockPos = blockEntity.getBlockPos();
        state.blockState = blockEntity.getBlockState();
    }
}
