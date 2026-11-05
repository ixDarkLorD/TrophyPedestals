package net.ixdarklord.trophypedestals.api;

import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

import java.util.Optional;

/**
 * Pedestals in the world, for other mods: finding the one at a position, to read or change with
 * {@link PedestalSettings} or {@link TrophyPedestals}.
 * <pre>{@code
 * PedestalBlocks.pedestalAt(level, pos).ifPresent(pedestal -> PedestalSettings.lock(pedestal));
 * }</pre>
 */
public final class PedestalBlocks {
    private PedestalBlocks() {}

    /** The pedestal at this position (any kind: stone, wood or custom, a trophy or not), or empty if there's none. */
    public static Optional<PedestalBlockEntity> pedestalAt(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos, Pedestals.BLOCK_ENTITY.get());
    }
}
