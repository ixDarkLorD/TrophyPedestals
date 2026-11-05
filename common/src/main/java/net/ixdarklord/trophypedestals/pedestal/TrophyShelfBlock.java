package net.ixdarklord.trophypedestals.pedestal;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * A trophy shelf: a wooden board half a block deep hung on a wall on two brackets, with a short rim along its back,
 * trimmed in gold, holding two trophies side by side against the rim. It goes only on the side of a solid block, facing
 * out from it, and falls when that block goes. Shelves side by side facing the same way (of any wood) join into one
 * long shelf, with corner caps only at its ends.
 */
public class TrophyShelfBlock extends TrophyDisplayBlock {
    public static final MapCodec<TrophyShelfBlock> CODEC = simpleCodec(TrophyShelfBlock::new);
    /** Whether it's joined to a shelf on its left, or on its right (as seen from its front). */
    public static final BooleanProperty LEFT = BooleanProperty.create("left"), RIGHT = BooleanProperty.create("right");
    // Its board (with its lip), its rim and its brackets, facing north (its back to the wall on its south), in pixels.
    private static final Map<Direction, VoxelShape> SHAPES = shapes(new double[][] {{0, 6, 6, 16, 10, 16}, {0, 9, 14, 16, 12, 16}, {1, 1, 9, 3, 7, 16}, {13, 1, 9, 15, 7, 16}});

    public TrophyShelfBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GOLD_TRIM, false).setValue(LEFT, false).setValue(RIGHT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, GOLD_TRIM, LEFT, RIGHT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    // Hung on the solid side of the block behind it.
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos wall = pos.relative(facing.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
    }

    // Whether a block is a shelf facing the same way, to join to.
    private static boolean joins(BlockState state, BlockState other) {
        return other.getBlock() instanceof TrophyShelfBlock && other.getValue(FACING) == state.getValue(FACING);
    }

    // Joined to the shelves beside it.
    private static BlockState joined(BlockState state, LevelReader level, BlockPos pos) {
        Direction right = right(state.getValue(FACING));
        return state.setValue(RIGHT, joins(state, level.getBlockState(pos.relative(right))))
                .setValue(LEFT, joins(state, level.getBlockState(pos.relative(right.getOpposite()))));
    }

    // On the wall clicked, or failing that the nearest wall the player faces that it can hang on; never on a floor.
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        for (Direction looking : context.getNearestLookingDirections()) {
            if (!looking.getAxis().isHorizontal()) continue;
            BlockState state = defaultBlockState().setValue(FACING, looking.getOpposite());
            if (state.canSurvive(context.getLevel(), context.getClickedPos())) return joined(state, context.getLevel(), context.getClickedPos());
        }
        return null;
    }

    // Its wall gone, it falls (dropping itself and its trophies).
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
