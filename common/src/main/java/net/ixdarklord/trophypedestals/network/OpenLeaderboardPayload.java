package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * To a client using a Leaderboard Crest on a pedestal: open the board picker for it, with every board, who leads each now
 * and with how much, and the board it shows now (empty for none).
 */
public record OpenLeaderboardPayload(BlockPos pos, String current, List<Status> boards) implements CustomPacketPayload {
    public static final Type<OpenLeaderboardPayload> TYPE = new Type<>(TrophyPedestalsMod.id("open_leaderboard"));

    /** A board, and who leads it (empty for no one) with how much. */
    public record Status(ResourceLocation board, String leader, int value) {}

    @Override
    public Type<OpenLeaderboardPayload> type() {
        return TYPE;
    }
}
