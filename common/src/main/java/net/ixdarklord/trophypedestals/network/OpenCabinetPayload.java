package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.core.BlockPos;

/**
 * To a cabinet's or shelf's owner: open its settings screen, with who else may use it and when its set name shows (by
 * their names: {@code private}, {@code team}, {@code everyone}; {@code shown}, {@code on_look}, {@code hidden}).
 */
public record OpenCabinetPayload(BlockPos pos, String access, String setName) implements CustomPacketPayload {
    public static final Type<OpenCabinetPayload> TYPE = new Type<>(TrophyPedestalsMod.id("open_cabinet"));

    @Override
    public Type<OpenCabinetPayload> type() {
        return TYPE;
    }
}
