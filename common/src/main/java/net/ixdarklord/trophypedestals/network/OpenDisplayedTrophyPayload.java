package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenDisplayedTrophyPayload(BlockPos pos, int slot) implements CustomPacketPayload {
    public static final Type<OpenDisplayedTrophyPayload> TYPE = new Type<>(TrophyPedestalsMod.id("open_displayed_trophy"));

    @Override
    public Type<OpenDisplayedTrophyPayload> type() {
        return TYPE;
    }
}
