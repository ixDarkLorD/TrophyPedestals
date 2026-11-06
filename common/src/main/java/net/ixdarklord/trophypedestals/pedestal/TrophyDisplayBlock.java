package net.ixdarklord.trophypedestals.pedestal;
import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.trophypedestals.network.OpenDisplayedTrophyPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A block that holds two trophies side by side, each shown as a small pedestal with its rank's light (the trophy
 * cabinet's halves and the trophy shelf): a trophy goes onto the spot it's used on and comes back off it into an empty
 * hand, and sneaking with an empty hand on one opens its adjust screen (the settings of its spot that don't change its
 * size or place). It faces whoever placed it. It has no trim until one is put on it: any block used on it makes its
 * trim of that block, drawn in the block's textures over the faces its gold would take; a gold ingot or block gives it
 * the gold trim of its own model ({@link #GOLD_TRIM}); an axe takes the trim off again. The trim is its
 * {@code trophypedestals:trim_material} ({@link #GOLD} for gold), kept on its item when it's broken.
 */
public abstract class TrophyDisplayBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    /** Whether its trim is gold (in its model); without it, it has none, or one of another block (drawn by its renderer). */
    public static final BooleanProperty GOLD_TRIM = BooleanProperty.create("gold_trim");
    /** The trim material that is the model's own gold: a gold block's. */
    public static final Identifier GOLD = BuiltInRegistries.BLOCK.getKey(Blocks.GOLD_BLOCK);
    // Half a block deep, its back against the wall (and its gold a pixel proud): a cabinet's shape, facing each way.
    private static final Map<Direction, VoxelShape> HALF_DEEP = shapes(new double[][] {{0, 0, 7, 16, 16, 16}});

    protected TrophyDisplayBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return HALF_DEEP.get(state.getValue(FACING));
    }

    // Lit inside as the open air around it is: smooth lighting doesn't darken the faces within it (the underside of a
    // cabinet's top, say) as it would inside a solid block, and daylight comes down through it.
    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    /** A shape made of these boxes (in pixels, as the north-facing model has them), turned to face each way. */
    protected static Map<Direction, VoxelShape> shapes(double[][] boxes) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (double[] box : boxes) shape = Shapes.or(shape, turned(box, facing));
            shapes.put(facing, shape);
        }
        return shapes;
    }

    // A box of the north-facing model turned to face this way (as its blockstate turns the model).
    private static VoxelShape turned(double[] box, Direction facing) {
        double x0 = box[0], z0 = box[2], x1 = box[3], z1 = box[5];
        return switch (facing) {
            case SOUTH -> Block.box(16 - x1, box[1], 16 - z1, 16 - x0, box[4], 16 - z0);
            case EAST -> Block.box(16 - z1, box[1], x0, 16 - z0, box[4], x1);
            case WEST -> Block.box(z0, box[1], 16 - x1, z1, box[4], 16 - x0);
            default -> Block.box(x0, box[1], z0, x1, box[4], z1);
        };
    }

    /** The blocks that share this one's trim: itself, and a cabinet's other half. */
    protected List<BlockPos> trimmedTogether(BlockState state, BlockPos pos) {
        return List.of(pos);
    }

    // Makes the trim of it (and what shares its trim) of this block (GOLD for gold), or takes it off (null).
    private void setTrim(Level level, BlockState state, BlockPos pos, @Nullable Identifier material) {
        for (BlockPos each : trimmedTogether(state, pos)) {
            BlockState there = level.getBlockState(each);
            if (!(there.getBlock() instanceof TrophyDisplayBlock) || !(level.getBlockEntity(each) instanceof TrophyCabinetBlockEntity display)) continue;
            display.setTrim(material);
            level.setBlock(each, there.setValue(GOLD_TRIM, GOLD.equals(material)), Block.UPDATE_ALL);
        }
    }

    // A block it can be trimmed with: any block's item but another display's or a pedestal's.
    private static @Nullable Identifier trimMaterial(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item) || item.getBlock() instanceof TrophyDisplayBlock || item.getBlock() instanceof PedestalBlock) return null;
        return BuiltInRegistries.BLOCK.getKey(item.getBlock());
    }

    // Placed from an item with a trim, it has that trim.
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TrophyCabinetBlockEntity display && display.trim() != null) {
            level.setBlock(pos, state.setValue(GOLD_TRIM, GOLD.equals(display.trim())), Block.UPDATE_ALL);
        }
    }

    // Its trophies' settings that answer redstone know whether a signal reaches it.
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, @Nullable Orientation orientation, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, orientation, moved);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TrophyCabinetBlockEntity display) display.setPowered(level.hasNeighborSignal(pos));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TrophyCabinetBlockEntity display) display.setPowered(level.hasNeighborSignal(pos));
    }

    /** Toward the viewer's right, looking at one facing this way. */
    public static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    /** The spot a point is over: 0 on the viewer's left, 1 on their right. */
    public static int slot(BlockState state, BlockPos pos, Vec3 at) {
        Direction right = right(state.getValue(FACING));
        double across = (at.x - pos.getX() - 0.5) * right.getStepX() + (at.z - pos.getZ() - 0.5) * right.getStepZ();
        return across < 0.0 ? 0 : 1;
    }

    // A trophy in hand goes onto the spot used, if it's free; a block in hand makes its trim of that block.
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TrophyCabinetBlockEntity display)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        // An axe takes its trim off.
        if (stack.is(ItemTags.AXES)) {
            if (display.trim() == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!level.isClientSide()) {
                setTrim(level, state, pos, null);
                level.playSound(null, pos, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        // A gold ingot (or a gold block) gives it the gold trim; any other block a trim of that block.
        Identifier material = stack.is(Items.GOLD_INGOT) ? GOLD : Pedestals.isTrophy(stack) ? null : trimMaterial(stack);
        if (material != null) {
            if (material.equals(display.trim())) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!level.isClientSide()) {
                setTrim(level, state, pos, material);
                level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }
        if (!Pedestals.isTrophy(stack)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        int slot = slot(state, pos, hit.getLocation());
        if (!display.item(slot).isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            display.setItem(slot, stack.copyWithCount(1));
            stack.consume(1, player);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    // An empty hand takes the trophy on the spot used; sneaking, it opens that trophy's adjust screen.
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TrophyCabinetBlockEntity display)) return InteractionResult.PASS;
        int slot = slot(state, pos, hit.getLocation());
        if (player.isSecondaryUseActive()) {
            if (display.item(slot).isEmpty()) return InteractionResult.PASS;
            if (player instanceof ServerPlayer server) Network.sendToPlayer(server, new OpenDisplayedTrophyPayload(pos, slot));
            return InteractionResult.SUCCESS;
        }
        if (display.item(slot).isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            ItemStack taken = display.item(slot);
            display.setItem(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(taken)) player.drop(taken, false, Prediction.SERVER_ONLY);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrophyCabinetBlockEntity(pos, state);
    }
}
