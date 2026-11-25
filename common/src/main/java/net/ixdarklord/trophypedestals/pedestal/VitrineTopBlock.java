package net.ixdarklord.trophypedestals.pedestal;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VitrineTopBlock extends Block {
    public static final MapCodec<VitrineTopBlock> CODEC = simpleCodec(VitrineTopBlock::new);
    private static final VoxelShape SHAPE = box(2.0, 0.0, 2.0, 14.0, (14.0F / 16.0F + PedestalBlock.CASE_HEIGHT - 1.0F) * 16.0, 14.0);

    public VitrineTopBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public static boolean over(LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof PedestalBlock && below.getValue(PedestalBlock.CASED);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (direction == Direction.DOWN && !over(level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return over(level, pos);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof PedestalBlock ? below.getCloneItemStack(level, pos.below(), includeData) : ItemStack.EMPTY;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos below = pos.below();
        BlockState pedestal = level.getBlockState(below);
        if (!(pedestal.getBlock() instanceof PedestalBlock)) return InteractionResult.PASS;
        return pedestal.useWithoutItem(level, player, new BlockHitResult(hit.getLocation(), hit.getDirection(), below, hit.isInside()));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos below = pos.below();
        BlockState pedestal = level.getBlockState(below);
        if (!(pedestal.getBlock() instanceof PedestalBlock)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        return pedestal.useItemOn(stack, level, player, hand, new BlockHitResult(hit.getLocation(), hit.getDirection(), below, hit.isInside()));
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos.below()) instanceof PedestalBlockEntity pedestal && pedestal.glass()) {
            ItemStack glass = pedestal.glassItem();
            pedestal.setGlass(false, null);
            if (!player.isCreative()) popResource(level, pos, glass);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
