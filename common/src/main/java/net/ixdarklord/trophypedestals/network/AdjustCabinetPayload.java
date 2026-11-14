package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.ixdarklord.trophypedestals.pedestal.TrophyCabinetBlockEntity;
import net.ixdarklord.trophypedestals.pedestal.TrophyDisplayBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * From a cabinet's settings screen: who else may use it, and when its set name shows, by their names (see
 * {@link OpenCabinetPayload}). Applied to both halves of a cabinet, for its owner and operators.
 */
public record AdjustCabinetPayload(BlockPos pos, String access, String setName) implements CustomPacketPayload {
    public static final Type<AdjustCabinetPayload> TYPE = new Type<>(TrophyPedestalsMod.id("adjust_cabinet"));
    // How near the cabinet the player must be, in blocks.
    private static final double REACH = 8.0;

    @Override
    public Type<AdjustCabinetPayload> type() {
        return TYPE;
    }

    public void apply(ServerPlayer player) {
        if (!player.level().isLoaded(pos) || player.distanceToSqr(pos.getCenter()) > REACH * REACH) return;
        if (!(player.level().getBlockEntity(pos) instanceof TrophyCabinetBlockEntity cabinet) || !cabinet.canChangeAccess(player)) return;
        PedestalBlockEntity.Access newAccess = PedestalBlockEntity.Access.byName(access);
        TrophyCabinetBlockEntity.SetName newSetName = TrophyCabinetBlockEntity.SetName.byName(setName);
        for (TrophyCabinetBlockEntity part : TrophyDisplayBlock.parts(player.level(), pos, player.level().getBlockState(pos))) {
            if (newAccess != null) part.setAccess(newAccess);
            if (newSetName != null) part.setSetName(newSetName);
        }
    }
}
