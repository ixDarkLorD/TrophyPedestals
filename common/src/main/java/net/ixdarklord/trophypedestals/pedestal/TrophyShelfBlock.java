package net.ixdarklord.trophypedestals.pedestal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class TrophyShelfBlock extends TrophyDisplayBlock {
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    private static final Map<Direction, VoxelShape> SOLID = shapes(new double[][] {{0, 6, 6, 16, 10, 16}, {0, 9, 14, 16, 12, 16}, {1, 1, 9, 3, 7, 16}, {13, 1, 9, 15, 7, 16}});
    private static final Map<Direction, VoxelShape> SHAPES = shapes(new double[][] {{0, 1, 6, 16, 16, 16}});
    private static final double[][] SPOTS = {{8.2, 10, 15.5, 16}, {0.5, 10, 7.8, 16}};

    public TrophyShelfBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GOLD_TRIM, false).setValue(LEFT, false).setValue(RIGHT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, GOLD_TRIM, LEFT, RIGHT);
    }

    @Override
    protected double[][] spots() {
        return SPOTS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SOLID.get(state.getValue(FACING));
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos wall = pos.relative(facing.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
    }

    private static boolean joins(BlockState state, BlockState other) {
        return other.getBlock() instanceof TrophyShelfBlock && other.getValue(FACING) == state.getValue(FACING);
    }

    private static BlockState joined(BlockState state, LevelReader level, BlockPos pos) {
        Direction right = right(state.getValue(FACING));
        return state.setValue(RIGHT, joins(state, level.getBlockState(pos.relative(right))))
                .setValue(LEFT, joins(state, level.getBlockState(pos.relative(right.getOpposite()))));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction looking : context.getNearestLookingDirections()) {
            if (!looking.getAxis().isHorizontal()) continue;
            BlockState state = defaultBlockState().setValue(FACING, looking.getOpposite());
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) return joined(state, context.getLevel(), context.getClickedPos());
        }
        return null;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        Direction right = right(state.getValue(FACING));
        if (direction == right) state = state.setValue(RIGHT, joins(state, neighbor));
        else if (direction == right.getOpposite()) state = state.setValue(LEFT, joins(state, neighbor));
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }
}
