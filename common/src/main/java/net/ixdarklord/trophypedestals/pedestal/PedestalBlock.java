package net.ixdarklord.trophypedestals.pedestal;

import com.mojang.serialization.MapCodec;
import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.api.PedestalRarities;
import net.ixdarklord.trophypedestals.api.PedestalRarity;
import net.ixdarklord.trophypedestals.client.PedestalParticles;
import net.ixdarklord.trophypedestals.network.OpenPedestalPayload;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
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

/**
 * A pedestal: a fluted column between gold bands, on a stepped plinth under a cap with a raised plate. One given for
 * recording an achievement (a tier unlocked, say) holds its icon, turning and glowing above it, with who unlocked it and when floating over it
 * ({@link PedestalBlockEntity}), and on clients its light rises in a spiral ({@code client/PedestalParticles}); a crafted
 * one takes an item to display, put on with it in hand and taken back with an empty one. Sneaking and using it with an
 * empty hand switches its text between always shown and shown while looked at. One block for each stone
 * ({@link Pedestals.Material}).
 */
public class PedestalBlock extends BaseEntityBlock {
    public static final MapCodec<PedestalBlock> CODEC = simpleCodec(PedestalBlock::new);
    public static final int MAX_RANK = 5;
    /** Whether it holds something (a trophy's icon or a displayed item), whose hologram lights the area round it. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /**
     * Its trophy's rank, 0 for none (a display, or a trophy with no rank) up to 5 (legendary, and any grander), which
     * sets how strongly it lights the area round it while lit.
     */
    public static final IntegerProperty RANK = IntegerProperty.create("rank", 0, MAX_RANK);
    /** How strong its light is while lit, at each rank: a display's 10, up to a legendary trophy's full 15. */
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

    public PedestalBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false).setValue(RANK, 0));
    }

    // Drawn by its model (a block with a block entity draws nothing unless it says so).
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, RANK);
    }

    // A trophy lights up as it's placed (its record is on the item), as strongly as its rank says.
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(LIT, Pedestals.isTrophy(context.getItemInHand())).setValue(RANK, rank(Pedestals.rarity(context.getItemInHand())));
    }

    // A lit pedestal with no rank in its state may be a trophy placed before pedestals' light followed their rank: now and
    // then it checks, and takes its rank's light.
    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(LIT) && state.getValue(RANK) == 0;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) pedestal.syncLight();
    }

    /** How strongly a pedestal in this state lights the area round it. */
    public static int light(BlockState state) {
        return state.getValue(LIT) ? LIGHT_BY_RANK[state.getValue(RANK)] : 0;
    }

    /** The {@link #RANK} a trophy of this rarity has: 0 for none, its rank up to {@link #MAX_RANK}. */
    public static int rank(@Nullable ResourceLocation rarity) {
        if (rarity == null || PedestalRarities.isNone(rarity)) return 0;
        return Mth.clamp(PedestalRarities.get(rarity).rank(), 1, MAX_RANK);
    }

    // A trophy announces its rank as it's placed, with its rank's sound for everyone near.
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) {
            pedestal.markPlaced();
            if (placer instanceof Player player) pedestal.setOwner(player);
        }
        ResourceLocation rarity = Pedestals.rarity(stack);
        if (level.isClientSide() || rarity == null || PedestalRarities.isNone(rarity)) return;
        PedestalRarity.Sound sound = PedestalRarities.get(rarity).placeSound();
        if (sound != null) level.playSound(null, pos, SoundEvent.createVariableRangeEvent(sound.id()), SoundSource.BLOCKS, sound.volume(), sound.pitch());
    }

    // Its redstone signal, kept on its block entity for its redstone setting (and sent to clients, which draw by it).
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos, boolean moved) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moved);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) pedestal.setPowered(level.hasNeighborSignal(pos));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) pedestal.setPowered(level.hasNeighborSignal(pos));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) pedestal.preRemoveSideEffects(pos, state);
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // An item in hand goes on display on a crafted pedestal with nothing on it.
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        // (An empty hand goes on to useWithoutItem: the adjustment screen, or taking an item back.)
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) || pedestal.isTrophy() || !pedestal.display().isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide()) {
            pedestal.setDisplay(stack.copyWithCount(1));
            stack.consume(1, player);
            if (pedestal.sounds()) level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return ItemInteractionResult.SUCCESS;
    }

    // Sneaking with an empty hand opens its adjustment screen (its text and display size), for those allowed to; otherwise
    // a displayed item comes back off it into the player's hands, unless the server turned that off (pedestal_take_items:
    // a server setting, sent to the clients, so both sides agree).
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof PedestalBlockEntity pedestal)) return InteractionResult.PASS;
        if (player.isSecondaryUseActive()) {
            if (player instanceof ServerPlayer server) {
                if (pedestal.locked()) {
                    // Locked by a mod: refused with its message, or silently.
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
        if (pedestal.display().isEmpty() || !TrophyPedestalsConfig.PEDESTAL_TAKE_ITEMS.get()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            ItemStack shown = pedestal.display();
            pedestal.setDisplay(ItemStack.EMPTY);
            if (!player.getInventory().add(shown)) player.drop(shown, false);
            if (pedestal.sounds()) level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PedestalBlockEntity(pos, state);
    }

    // Ticks only on clients, for the particles.
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, Pedestals.BLOCK_ENTITY.get(), PedestalBlock::clientTick) : null;
    }

    private static void clientTick(Level level, BlockPos pos, BlockState state, PedestalBlockEntity pedestal) {
        PedestalParticles.tick(level, pos, pedestal);
    }
}
