package net.ixdarklord.trophypedestals.pedestal;

import net.ixdarklord.coolcatcore.api.event.v2.common.BlockEvents;
import net.ixdarklord.coolcatcore.api.event.v2.core.EventResult;
import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.client.PedestalParticles;
import net.ixdarklord.trophypedestals.compat.ItemInteractionResult;
import net.ixdarklord.trophypedestals.network.OpenPedestalPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** A pedestal: a fluted column between gold bands, on a stepped plinth under a cap with a raised plate. */
public class PedestalBlock extends BaseEntityBlock {
    public static final int MAX_RANK = 5;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final IntegerProperty RANK = IntegerProperty.create("rank", 0, MAX_RANK);
    public static final BooleanProperty CASED = BooleanProperty.create("cased");
    /** Whether its When Powered setting has it on: it gives a low light then, even with nothing on it. */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final int POWERED_LIGHT = 3;
    /** Whether it's a display pedestal with no lens fitted: with power, its open socket gives a little more light. */
    public static final BooleanProperty SOCKET = BooleanProperty.create("socket");
    public static final int SOCKET_LIGHT = 6;
    public static final float CASE_HEIGHT = 1.1F;
    private static final int[] LIGHT_BY_RANK = {10, 11, 12, 13, 14, 15};
    private static final VoxelShape SHAPE = Shapes.or(
            box(0.5, 0.0, 0.5, 15.5, 1.5, 15.5),
            box(1.5, 1.5, 1.5, 14.5, 3.0, 14.5),
            box(4.0, 3.0, 4.0, 12.0, 12.0, 12.0),
            box(2.0, 12.0, 2.0, 14.0, 14.0, 14.0),
            box(3.0, 14.0, 3.0, 13.0, 15.0, 4.0),
            box(3.0, 14.0, 12.0, 13.0, 15.0, 13.0),
            box(3.0, 14.0, 4.0, 4.0, 15.0, 12.0),
            box(12.0, 14.0, 4.0, 13.0, 15.0, 12.0));
    private static final VoxelShape CASED_SHAPE = Shapes.or(SHAPE, box(2.0, 14.0, 2.0, 14.0, 16.0, 14.0));
    private static final VoxelShape OUTLINE = Shapes.or(
            box(0.5, 0.0, 0.5, 15.5, 1.5, 15.5),
            box(1.5, 1.5, 1.5, 14.5, 3.0, 14.5),
            box(4.0, 3.0, 4.0, 12.0, 12.0, 12.0),
            box(2.0, 12.0, 2.0, 14.0, 15.0, 14.0));
    private static final VoxelShape CASED_OUTLINE = Shapes.or(OUTLINE, box(2.0, 14.0, 2.0, 14.0, 16.0, 14.0));

    public PedestalBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false).setValue(RANK, 0).setValue(CASED, false).setValue(POWERED, false).setValue(SOCKET, false));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, RANK, CASED, POWERED, SOCKET);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(LIT, Pedestals.isTrophy(context.getItemInHand())).setValue(RANK, rank(Pedestals.rarity(context.getItemInHand())));
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return state.getValue(LIT) && state.getValue(RANK) == 0 || !state.getValue(POWERED);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) pedestal.syncLight();
    }

    public static int light(BlockState state) {
        return Math.max(state.getValue(LIT) ? LIGHT_BY_RANK[state.getValue(RANK)] : 0, !state.getValue(POWERED) ? 0 : state.getValue(SOCKET) ? SOCKET_LIGHT : POWERED_LIGHT);
    }

    public static int rank(@Nullable ResourceLocation rarity) {
        if (rarity == null || PedestalRarities.isNone(rarity)) return 0;
        return Mth.clamp(PedestalRarities.get(rarity).rank(), 1, MAX_RANK);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) {
            pedestal.applyItemLens(stack);
            pedestal.markPlaced();
            if (placer instanceof Player player) pedestal.setOwner(player);
            if (stack.hasCustomHoverName()) pedestal.setCustomName(stack.getHoverName());
        }
        ResourceLocation rarity = Pedestals.rarity(stack);
        if (level.isClientSide() || rarity == null || PedestalRarities.isNone(rarity)) return;
        PedestalRarity.Sound sound = PedestalRarities.get(rarity).placeSound();
        if (sound != null) level.playSound(null, pos, SoundEvent.createVariableRangeEvent(sound.id()), SoundSource.BLOCKS, sound.volume(), sound.pitch());
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moved);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) pedestal.setPowered(level.hasNeighborSignal(pos));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) pedestal.setPowered(level.hasNeighborSignal(pos));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) pedestal.preRemoveSideEffects(pos, state);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(CASED) ? CASED_OUTLINE : OUTLINE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(CASED) ? CASED_SHAPE : SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        ItemInteractionResult item = stack.isEmpty() ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                : useItemOn(stack, state, level, pos, player, hand, hit);
        if (item != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) return item.result();
        return hand == InteractionHand.MAIN_HAND ? useWithoutItem(state, level, pos, player, hit) : InteractionResult.PASS;
    }

    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof CuratorsStampItem stamp && level.getBlockEntity(pos) instanceof PedestalBlockEntity target) {
            return stamp.paste(stack, level, player, target);
        }
        if (isCaseGlass(stack) && level.getBlockEntity(pos) instanceof PedestalBlockEntity cased && caseable(cased)) {
            if (!level.getBlockState(pos.above()).canBeReplaced()) {
                if (player instanceof ServerPlayer server) server.displayClientMessage(Component.translatable("trophypedestals.pedestal.no_room_for_case"), true);
                return ItemInteractionResult.FAIL;
            }
            if (!level.isClientSide()) closeCase(level, pos, cased, stack, player);
            return ItemInteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof PedestalBlockEntity closed && closed.glass()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.is(Items.ARMOR_STAND) && level.getBlockEntity(pos) instanceof PedestalBlockEntity stand && !stand.isTrophy()
                && stand.display().isEmpty() && stand.leaderboard() == null && !stand.mannequin()) {
            if (!level.isClientSide()) {
                stand.setArmorStand(true);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level.playSound(null, pos, SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof PedestalBlockEntity dressed && !dressed.isTrophy() && dressed.display().isEmpty()
                && dressed.leaderboard() == null && !stack.isEmpty()) {
            EquipmentSlot slot = mannequinSlot(stack, dressed);
            if (slot != null) {
                if (!level.isClientSide()) {
                    ItemStack old = dressed.setOutfit(slot, stack.copyWithCount(1));
                    if (!player.getAbilities().instabuild) stack.shrink(1);
                    if (!old.isEmpty() && !player.getInventory().add(old)) player.drop(old, false);
                    if (dressed.sounds()) level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return ItemInteractionResult.SUCCESS;
            }
        }
        if (stack.getItem() instanceof LensItem && level.getBlockEntity(pos) instanceof PedestalBlockEntity lensed && lensable(lensed)) {
            if (hit.getDirection() != Direction.UP) {
                if (player instanceof ServerPlayer server) server.displayClientMessage(Component.translatable("trophypedestals.pedestal.lens_from_top"), true);
                return ItemInteractionResult.FAIL;
            }
            if (!level.isClientSide()) {
                ItemStack old = lensed.setLens(stack.copyWithCount(1));
                if (!player.getAbilities().instabuild) stack.shrink(1);
                if (!old.isEmpty() && !player.getInventory().add(old)) player.drop(old, false);
                if (lensed.sounds()) level.playSound(null, pos, SoundEvents.SPYGLASS_USE, SoundSource.BLOCKS, 1.0F, 1.3F);
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (stack.is(Pedestals.LEADERBOARD_CREST.get()) && level.getBlockEntity(pos) instanceof PedestalBlockEntity board
                && !board.isTrophy() && board.display().isEmpty() && !board.mannequin() && !player.isSecondaryUseActive()) {
            if (player instanceof ServerPlayer server) Leaderboards.openPicker(server, pos, board);
            return ItemInteractionResult.SUCCESS;
        }
        if (stack.isEmpty() || level.getBlockEntity(pos) instanceof PedestalBlockEntity shown && shown.leaderboard() != null || !(level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) || pedestal.isTrophy() || !pedestal.display().isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (pedestal.lens().isEmpty()) {
            if (player instanceof ServerPlayer server) server.displayClientMessage(Component.translatable("trophypedestals.pedestal.needs_lens"), true);
            return ItemInteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            pedestal.setDisplay(stack.copyWithCount(1));
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (pedestal.sounds()) level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return ItemInteractionResult.SUCCESS;
    }

    private static void pop(Level level, BlockPos pos) {
        RandomSource random = level.getRandom();
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F, ((random.nextFloat() - random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
    }

    private static boolean onGlass(BlockPos pos, BlockHitResult hit) {
        return hit.getLocation().y - pos.getY() > 14.0 / 16.0 - 1.0E-4;
    }

    public static void init() {
        BlockEvents.PLACE.register((level, pos, state, placer, stack) -> {
            BlockPos below = pos.below();
            if (!(level.getBlockState(below).getBlock() instanceof PedestalBlock) || !(level.getBlockEntity(below) instanceof PedestalBlockEntity pedestal)) {
                return EventResult.pass();
            }
            if (isCaseGlass(stack) && caseable(pedestal)) {
                closeCase(level, below, pedestal, stack, placer instanceof LivingEntity living ? living : null);
            } else if (placer instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("trophypedestals.pedestal.nothing_on_top"), true);
            }
            return EventResult.interrupt();
        });
    }

    /** Lenses fit display pedestals: not trophies, leaderboards or mannequins. */
    private static boolean lensable(PedestalBlockEntity pedestal) {
        return !pedestal.isTrophy() && pedestal.leaderboard() == null && !pedestal.mannequin();
    }

    private static boolean caseable(PedestalBlockEntity pedestal) {
        return !pedestal.glass() && (pedestal.isTrophy() || !pedestal.display().isEmpty() || pedestal.leaderboard() != null || pedestal.mannequin());
    }

    private static void closeCase(Level level, BlockPos pos, PedestalBlockEntity pedestal, ItemStack stack, @Nullable LivingEntity user) {
        pedestal.setGlass(true, stack.getItem() instanceof BlockItem item && item.getBlock() instanceof StainedGlassBlock stained ? stained.getColor() : null);
        if (!(user instanceof Player player && player.getAbilities().instabuild)) stack.shrink(1);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private static boolean isCaseGlass(ItemStack stack) {
        return stack.is(Items.GLASS) || stack.getItem() instanceof BlockItem item && item.getBlock() instanceof StainedGlassBlock;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return comparatorSignal(state);
    }

    public static int comparatorSignal(BlockState state) {
        return state.getValue(LIT) ? Math.max(1, state.getValue(RANK)) : 0;
    }

    private static @Nullable EquipmentSlot mannequinSlot(ItemStack stack, PedestalBlockEntity pedestal) {
        if (!pedestal.mannequin()) return null;
        Equipable equippable = Equipable.get(stack);
        EquipmentSlot slot = equippable == null ? null : equippable.getEquipmentSlot();
        if (slot != null && slot.getType() == EquipmentSlot.Type.ARMOR) return slot;
        return slot == EquipmentSlot.OFFHAND || stack.is(Items.SHIELD) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
    }

    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal)) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) {
            if (pedestal.glass() && onGlass(pos, hit)) {
                if (player instanceof ServerPlayer server) {
                    if (pedestal.canAdjust(server)) {
                        ItemStack glass = pedestal.glassItem();
                        pedestal.setGlass(false, null);
                        if (!player.getInventory().add(glass)) player.drop(glass, false);
                        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
                    } else {
                        server.displayClientMessage(Component.translatable("trophypedestals.pedestal.not_yours", pedestal.ownerName()), true);
                    }
                }
                return InteractionResult.SUCCESS;
            }
            if (player instanceof ServerPlayer server) {
                if (pedestal.locked()) {
                    if (pedestal.refusal() != null) {
                        server.displayClientMessage(pedestal.refusal(), true);
                        level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.5F, 1.0F);
                    }
                } else if (pedestal.canAdjust(server)) {
                    Network.sendToPlayer(server, new OpenPedestalPayload(pos));
                } else {
                    server.displayClientMessage(Component.translatable("trophypedestals.pedestal.not_yours", pedestal.ownerName()), true);
                    level.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.5F, 1.0F);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (pedestal.glass()) {
            if (pedestal.mobTrophy() != null && !level.isClientSide() && pedestal.sounds()) MobTrophies.playSound(level, pos, pedestal);
            return InteractionResult.SUCCESS;
        }
        if (pedestal.mannequin() && TrophyPedestalsConfig.PEDESTAL_TAKE_ITEMS.get()) {
            if (!level.isClientSide()) {
                boolean dressed = false;
                for (EquipmentSlot slot : PedestalBlockEntity.OUTFIT_SLOTS) {
                    ItemStack worn = pedestal.setOutfit(slot, ItemStack.EMPTY);
                    if (worn.isEmpty()) continue;
                    dressed = true;
                    if (!player.getInventory().add(worn)) player.drop(worn, false);
                }
                if (dressed) {
                    if (pedestal.sounds()) level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.BLOCKS, 1.0F, 0.8F);
                } else {
                    pedestal.setArmorStand(false);
                    ItemStack stand = new ItemStack(Items.ARMOR_STAND);
                    if (!player.getInventory().add(stand)) player.drop(stand, false);
                    if (pedestal.sounds()) pop(level, pos);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (pedestal.leaderboard() != null && TrophyPedestalsConfig.PEDESTAL_TAKE_ITEMS.get()) {
            if (!level.isClientSide()) {
                pedestal.setLeaderboard(null);
                ItemStack crest = new ItemStack(Pedestals.LEADERBOARD_CREST.get());
                if (!player.getInventory().add(crest)) player.drop(crest, false);
                if (pedestal.sounds()) pop(level, pos);
            }
            return InteractionResult.SUCCESS;
        }
        if (pedestal.mobTrophy() != null) {
            if (!level.isClientSide() && pedestal.sounds()) MobTrophies.playSound(level, pos, pedestal);
            return InteractionResult.SUCCESS;
        }
        if (pedestal.display().isEmpty() && !pedestal.lens().isEmpty() && hit.getDirection() == Direction.UP && TrophyPedestalsConfig.PEDESTAL_TAKE_ITEMS.get()) {
            if (!level.isClientSide()) {
                ItemStack lens = pedestal.setLens(ItemStack.EMPTY);
                if (!player.getInventory().add(lens)) player.drop(lens, false);
                if (pedestal.sounds()) pop(level, pos);
            }
            return InteractionResult.SUCCESS;
        }
        if (pedestal.display().isEmpty() || !TrophyPedestalsConfig.PEDESTAL_TAKE_ITEMS.get()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            ItemStack shown = pedestal.display();
            pedestal.setDisplay(ItemStack.EMPTY);
            if (!player.getInventory().add(shown)) player.drop(shown, false);
            if (pedestal.sounds()) pop(level, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PedestalBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, Pedestals.BLOCK_ENTITY.get(), PedestalBlock::clientTick)
                : createTickerHelper(type, Pedestals.BLOCK_ENTITY.get(), (tickLevel, pos, tickState, pedestal) -> {
                    MobTrophies.grow(tickLevel, pos, pedestal);
                    Leaderboards.tick(tickLevel, pos, pedestal);
                });
    }

    private static void clientTick(Level level, BlockPos pos, BlockState state, PedestalBlockEntity pedestal) {
        PedestalParticles.tick(level, pos, pedestal);
    }
}
