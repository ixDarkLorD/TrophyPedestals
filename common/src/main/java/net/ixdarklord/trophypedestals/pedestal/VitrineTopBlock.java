package net.ixdarklord.trophypedestals.pedestal;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The upper part of a glass case: an invisible block in the space over a cased pedestal, its shape the part of the case
 * that reaches up into it (the case is drawn with the pedestal; its own model is clear, there only for the cracks
 * drawn over it as it's broken). It's there so that space is the case's, as a door's upper
 * half is the door's: looked at, clicked and picked as the pedestal below (a middle click gives the pedestal), and solid.
 * Breaking it lifts the glass off the pedestal (dropping it); it goes when the pedestal loses its case.
 */
public class VitrineTopBlock extends Block {
    public static final MapCodec<VitrineTopBlock> CODEC = simpleCodec(VitrineTopBlock::new);
    // The case over the cap, from the space's floor: as wide as the cap, up to the case's top.
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

    /** Whether the pedestal below has a glass case, which this is the top of. */
    public static boolean over(LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof PedestalBlock && below.getValue(PedestalBlock.CASED);
    }

    // Gone as soon as the pedestal below has no case (lifted off, or the pedestal broken).
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        if (direction == Direction.DOWN && !over(level, pos)) return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return over(level, pos);
    }

    // A middle click gives the pedestal (with its data, as the game copies it).
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof PedestalBlock ? below.getBlock().getCloneItemStack(level, pos.below(), below) : ItemStack.EMPTY;
    }

    // Used as the pedestal below is: sneaking with an empty hand lifts the glass (for those who may).
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos below = pos.below();
        BlockState pedestal = level.getBlockState(below);
        if (!(pedestal.getBlock() instanceof PedestalBlock)) return InteractionResult.PASS;
        return pedestal.useWithoutItem(level, player, new BlockHitResult(hit.getLocation(), hit.getDirection(), below, hit.isInside()));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos below = pos.below();
        BlockState pedestal = level.getBlockState(below);
        if (!(pedestal.getBlock() instanceof PedestalBlock)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return pedestal.useItemOn(stack, level, player, hand, new BlockHitResult(hit.getLocation(), hit.getDirection(), below, hit.isInside()));
    }

    // Broken by a player: the glass comes off the pedestal below, and drops (the pedestal stays).
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
