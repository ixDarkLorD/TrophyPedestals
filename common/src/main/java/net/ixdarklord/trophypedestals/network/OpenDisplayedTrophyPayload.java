package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.core.BlockPos;
import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;

/** Tells a client to open the adjust screen for the trophy on this spot (0 or 1) of the trophy display here. */
public record OpenDisplayedTrophyPayload(BlockPos pos, int slot) implements CustomPacketPayload {
    public static final Type<OpenDisplayedTrophyPayload> TYPE = new Type<>(TrophyPedestalsMod.id("open_displayed_trophy"));

    @Override
    public Type<OpenDisplayedTrophyPayload> type() {
        return TYPE;
    }
}
