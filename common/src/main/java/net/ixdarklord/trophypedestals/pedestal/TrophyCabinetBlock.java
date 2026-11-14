package net.ixdarklord.trophypedestals.pedestal;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * A trophy cabinet: a wooden shelf two blocks wide with a back, a top and a post at each end, framed in gold, holding up
 * to four trophies (two on each half). Placed, its left half (as seen from the front) is where it was put and its right
 * half beside it; breaking either breaks both, and the cabinet drops once (from its left half), with its trophies.
 */
public class TrophyCabinetBlock extends TrophyDisplayBlock {
    public static final MapCodec<TrophyCabinetBlock> CODEC = simpleCodec(TrophyCabinetBlock::new);
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    // Its two spots' windows on its front (pixels, facing north: the left spot is on the viewer's left, east), between
    // its base and its top rail, inside its outer post.
    private static final double[][] SPOTS = {{8.2, 2, 14, 14}, {2, 2, 7.8, 14}};
    /** Whether it's joined to a cabinet's like half above it, or below it (stacked: their boards meet as one divider). */
    public static final BooleanProperty UP = BlockStateProperties.UP, DOWN = BlockStateProperties.DOWN;

    /** Which half of the cabinet, as seen from its front. */
    public enum Part implements StringRepresentable {
        LEFT, RIGHT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public TrophyCabinetBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.LEFT)
                .setValue(UP, false).setValue(DOWN, false).setValue(GOLD_TRIM, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected double[][] spots() {
        return SPOTS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, UP, DOWN, GOLD_TRIM);
    }

    // Where the other half is.
    /** Where the other half of this cabinet is. */
    public static BlockPos partner(BlockPos pos, BlockState state) {
        Direction right = right(state.getValue(FACING));
        return state.getValue(PART) == Part.LEFT ? pos.relative(right) : pos.relative(right.getOpposite());
    }

    // Whether a block is a cabinet's half like this one (the same way round), to join to.
    private boolean joins(BlockState state, BlockState other) {
        return other.getBlock() instanceof TrophyCabinetBlock && other.getValue(FACING) == state.getValue(FACING) && other.getValue(PART) == state.getValue(PART);
    }

    // Joined to the like halves above and below it.
    private BlockState joined(BlockState state, LevelReader level, BlockPos pos) {
        return state.setValue(UP, joins(state, level.getBlockState(pos.above()))).setValue(DOWN, joins(state, level.getBlockState(pos.below())));
    }

    @Override
    protected List<BlockPos> trimmedTogether(BlockState state, BlockPos pos) {
        return List.of(pos, partner(pos, state));
    }

    // Facing the player, with room for its right half.
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos other = context.getClickedPos().relative(right(facing));
        Level level = context.getLevel();
        if (!level.getBlockState(other).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(other)) return null;
        return joined(defaultBlockState().setValue(FACING, facing).setValue(PART, Part.LEFT), level, context.getClickedPos());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        // Its right half, joined to what's above and below it, with the same trim.
        BlockState placed = level.getBlockState(pos);
        BlockPos other = partner(pos, placed);
        level.setBlock(other, joined(placed.setValue(PART, Part.RIGHT), level, other), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof TrophyCabinetBlockEntity left && level.getBlockEntity(other) instanceof TrophyCabinetBlockEntity right) {
            right.setTrim(left.trim());
        }
    }

    // A half whose other half is gone goes too.
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (neighborPos.equals(partner(pos, state))
                && !(neighbor.is(this) && neighbor.getValue(FACING) == state.getValue(FACING) && neighbor.getValue(PART) != state.getValue(PART))) {
            return Blocks.AIR.defaultBlockState();
        }
        if (direction.getAxis().isVertical()) state = state.setValue(direction == Direction.UP ? UP : DOWN, joins(state, neighbor));
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighbor, random);
    }

    // Broken at its right half, the left half (which drops the cabinet) breaks as if broken itself.
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && state.getValue(PART) == Part.RIGHT) {
            BlockPos other = partner(pos, state);
            if (level.getBlockState(other).is(this)) level.destroyBlock(other, !player.isCreative(), player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }
}
