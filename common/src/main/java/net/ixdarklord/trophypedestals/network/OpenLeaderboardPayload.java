package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record OpenLeaderboardPayload(BlockPos pos, String current, List<Status> boards) implements CustomPacketPayload {
    public static final Type<OpenLeaderboardPayload> TYPE = new Type<>(TrophyPedestalsMod.id("open_leaderboard"));

    public record Status(ResourceLocation board, String leader, int value) {}

    @Override
    public Type<OpenLeaderboardPayload> type() {
        return TYPE;
    }
}
