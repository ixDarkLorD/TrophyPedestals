package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.pedestal.PedestalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import net.minecraft.world.phys.Vec3;

/**
 * From a client's adjustment screen, for a mannequin pedestal (one with an armor stand installed): whether it shows as a
 * player (in its owner's skin) or the armor stand, and the armor stand's pose (each part's x, y and z, in degrees, in
 * {@link PedestalBlockEntity#STAND_PARTS}' order).
 */
public record AdjustMannequinPayload(BlockPos pos, boolean playerModel, List<Float> pose) implements CustomPacketPayload {
    public static final Type<AdjustMannequinPayload> TYPE = new Type<>(TrophyPedestalsMod.id("adjust_mannequin"));
    // How near the pedestal the player must be, in blocks.
    private static final double REACH = 8.0;

    @Override
    public Type<AdjustMannequinPayload> type() {
        return TYPE;
    }

    /** Applies it for this player, if they're near the mannequin pedestal and allowed to adjust it. */
    public void apply(ServerPlayer player) {
        if (!player.level().isLoaded(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > REACH * REACH) return;
        if (!(player.level().getBlockEntity(pos) instanceof PedestalBlockEntity pedestal) || !pedestal.mannequin() || !pedestal.canAdjust(player)) return;
        if (pose.size() != PedestalBlockEntity.DEFAULT_STAND_POSE.size()) return;
        pedestal.setMannequinLook(playerModel, pose);
    }
}
