package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.core.BlockPos;

public record OpenDisplayedTrophyPayload(BlockPos pos, int slot) implements CustomPacketPayload {
    public static final Type<OpenDisplayedTrophyPayload> TYPE = new Type<>(TrophyPedestalsMod.id("open_displayed_trophy"));

    @Override
    public Type<OpenDisplayedTrophyPayload> type() {
        return TYPE;
    }
}
