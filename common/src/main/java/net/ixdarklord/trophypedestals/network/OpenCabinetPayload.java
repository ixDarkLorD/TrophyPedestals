package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.core.BlockPos;

public record OpenCabinetPayload(BlockPos pos, String access, String setName) implements CustomPacketPayload {
    public static final Type<OpenCabinetPayload> TYPE = new Type<>(TrophyPedestalsMod.id("open_cabinet"));

    @Override
    public Type<OpenCabinetPayload> type() {
        return TYPE;
    }
}
