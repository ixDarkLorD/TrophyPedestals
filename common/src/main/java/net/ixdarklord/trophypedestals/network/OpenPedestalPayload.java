package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record OpenPedestalPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<OpenPedestalPayload> TYPE = new Type<>(TrophyPedestalsMod.id("open_pedestal"));

    @Override
    public Type<OpenPedestalPayload> type() {
        return TYPE;
    }
}
