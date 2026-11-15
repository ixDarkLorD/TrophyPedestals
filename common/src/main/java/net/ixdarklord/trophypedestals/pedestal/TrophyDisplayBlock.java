package net.ixdarklord.trophypedestals.pedestal;
import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.network.OpenCabinetPayload;
import net.ixdarklord.trophypedestals.network.OpenDisplayedTrophyPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
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
import net.minecraft.world.level.block.EntityBlock;
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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A block that holds two trophies side by side, each shown as a small pedestal with its rank's light (the trophy
 * cabinet's halves and the trophy shelf): a trophy goes onto the spot it's used on and comes back off it into an empty
 * hand, and sneaking with an empty hand on one opens its adjust screen (the settings of its spot that don't change its
 * size or place).
 */
public abstract class TrophyDisplayBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty GOLD_TRIM = BooleanProperty.create("gold_trim");
    public static final Identifier GOLD = BuiltInRegistries.BLOCK.getKey(Blocks.GOLD_BLOCK);
    private static final Map<Direction, VoxelShape> HALF_DEEP = shapes(new double[][] {{0, 0, 7, 16, 16, 16}});

    protected TrophyDisplayBlock(Properties properties) {
        super(properties);
    }

    protected abstract double[][] spots();

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return HALF_DEEP.get(state.getValue(FACING));
    }

    public static int spotAt(BlockState state, BlockPos pos, BlockHitResult hit) {
        if (!(state.getBlock() instanceof TrophyDisplayBlock display) || hit.getDirection() != state.getValue(FACING)) return -1;
        Vec3 at = hit.getLocation();
        double wx = (at.x - pos.getX()) * 16.0, wz = (at.z - pos.getZ()) * 16.0, y = (at.y - pos.getY()) * 16.0;
        double x = switch (state.getValue(FACING)) {
            case SOUTH -> 16.0 - wx;
            case EAST -> wz;
            case WEST -> 16.0 - wz;
            default -> wx;
        };
        double[][] spots = display.spots();
        for (int i = 0; i < spots.length; i++) {
            double[] window = spots[i];
            if (x >= window[0] && x <= window[2] && y >= window[1] && y <= window[3]) return i;
        }
        return -1;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    protected static Map<Direction, VoxelShape> shapes(double[][] boxes) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape shape = Shapes.empty();
            for (double[] box : boxes) shape = Shapes.or(shape, turned(box, facing));
            shapes.put(facing, shape);
        }
        return shapes;
    }

    private static VoxelShape turned(double[] box, Direction facing) {
        double x0 = box[0], z0 = box[2], x1 = box[3], z1 = box[5];
        return switch (facing) {
            case SOUTH -> Block.box(16 - x1, box[1], 16 - z1, 16 - x0, box[4], 16 - z0);
            case EAST -> Block.box(16 - z1, box[1], x0, 16 - z0, box[4], x1);
            case WEST -> Block.box(z0, box[1], 16 - x1, z1, box[4], 16 - x0);
            default -> Block.box(x0, box[1], z0, x1, box[4], z1);
        };
    }

    protected List<BlockPos> trimmedTogether(BlockState state, BlockPos pos) {
        return List.of(pos);
    }

    private void setTrim(Level level, BlockState state, BlockPos pos, @Nullable Identifier material) {
        for (BlockPos each : trimmedTogether(state, pos)) {
            BlockState there = level.getBlockState(each);
            if (!(there.getBlock() instanceof TrophyDisplayBlock) || !(level.getBlockEntity(each) instanceof TrophyCabinetBlockEntity display)) continue;
            display.setTrim(material);
            level.setBlock(each, there.setValue(GOLD_TRIM, GOLD.equals(material)), Block.UPDATE_ALL);
        }
    }

    public static final TagKey<Block> NO_TRIM = TagKey.create(Registries.BLOCK, TrophyPedestalsMod.id("no_trim"));

    private static @Nullable Identifier trimMaterial(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item) || !canTrim(item.getBlock())) return null;
        return BuiltInRegistries.BLOCK.getKey(item.getBlock());
    }

    public static boolean canTrim(Block block) {
        return !(block instanceof EntityBlock) && !block.builtInRegistryHolder().is(NO_TRIM);
    }

    public static boolean canTrim(Identifier material) {
        if (GOLD.equals(material)) return true;
        Block block = BuiltInRegistries.BLOCK.getOptional(material).orElse(null);
        return block != null && canTrim(block);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof TrophyCabinetBlockEntity display && display.trim() != null) {
            level.setBlock(pos, state.setValue(GOLD_TRIM, GOLD.equals(display.trim())), Block.UPDATE_ALL);
        }
        if (!level.isClientSide() && placer instanceof Player player) {
            for (TrophyCabinetBlockEntity part : parts(level, pos, level.getBlockState(pos))) part.setOwner(player.getUUID());
        }
    }

    public static List<TrophyCabinetBlockEntity> parts(Level level, BlockPos pos, BlockState state) {
        List<TrophyCabinetBlockEntity> parts = new ArrayList<>();
        if (level.getBlockEntity(pos) instanceof TrophyCabinetBlockEntity own) parts.add(own);
        if (state.getBlock() instanceof TrophyCabinetBlock && level.getBlockEntity(TrophyCabinetBlock.partner(pos, state)) instanceof TrophyCabinetBlockEntity other) {
            parts.add(other);
        }
        return parts;
    }

    private static boolean openSettings(TrophyCabinetBlockEntity display, Player player) {
        if (!display.canChangeAccess(player)) {
            if (player instanceof ServerPlayer server) server.sendOverlayMessage(Component.translatable("trophypedestals.cabinet.not_yours"));
            return false;
        }
        if (player instanceof ServerPlayer server) {
            Network.sendToPlayer(server, new OpenCabinetPayload(display.getBlockPos(), display.access().getSerializedName(), display.setName().getSerializedName()));
        }
        return true;
    }

    private static boolean mayUse(TrophyCabinetBlockEntity display, Player player) {
        if (display.canUse(player)) return true;
        if (player instanceof ServerPlayer server) server.sendOverlayMessage(Component.translatable("trophypedestals.cabinet.not_yours"));
        return false;
    }

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

    public static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    public static int slot(BlockState state, BlockPos pos, Vec3 at) {
        Direction right = right(state.getValue(FACING));
        double across = (at.x - pos.getX() - 0.5) * right.getStepX() + (at.z - pos.getZ() - 0.5) * right.getStepZ();
        return across < 0.0 ? 0 : 1;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TrophyCabinetBlockEntity display)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!mayUse(display, player)) return InteractionResult.FAIL;
        if (stack.is(ItemTags.AXES)) {
            if (display.trim() == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!level.isClientSide()) {
                setTrim(level, state, pos, null);
                level.playSound(null, pos, SoundEvents.AXE_STRIP.value(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        Identifier material = stack.is(Items.GOLD_INGOT) ? GOLD : Pedestals.isTrophy(stack) ? null : trimMaterial(stack);
        if (material != null) {
            if (material.equals(display.trim())) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (!level.isClientSide()) {
                setTrim(level, state, pos, material);
                level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
            }
            return InteractionResult.SUCCESS;
        }
        int slot = spotAt(state, pos, hit);
        if (!holds(stack) || slot < 0) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!display.item(slot).isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide()) {
            display.setItem(slot, stack.copyWithCount(1));
            stack.consume(1, player);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof TrophyCabinetBlockEntity display)) return InteractionResult.PASS;
        int slot = spotAt(state, pos, hit);
        if (player.isSecondaryUseActive()) {
            if (slot < 0 || !Pedestals.isTrophy(display.item(slot))) return openSettings(display, player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
            if (!mayUse(display, player)) return InteractionResult.FAIL;
            if (player instanceof ServerPlayer server) Network.sendToPlayer(server, new OpenDisplayedTrophyPayload(pos, slot));
            return InteractionResult.SUCCESS;
        }
        if (slot < 0 || display.item(slot).isEmpty()) return InteractionResult.PASS;
        if (!mayUse(display, player)) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            ItemStack taken = display.item(slot);
            display.setItem(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(taken)) player.drop(taken, false, Prediction.SERVER_ONLY);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    public static boolean holds(ItemStack stack) {
        return Pedestals.isTrophy(stack) || !stack.isEmpty() && !(stack.getItem() instanceof BlockItem);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TrophyCabinetBlockEntity(pos, state);
    }
}
