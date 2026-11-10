package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** To a client: open the Mob Trophies Ledger, with the mobs they've collected a trophy of, and a gilded one of. */
public record OpenLedgerPayload(List<ResourceLocation> collected, List<ResourceLocation> gilded) implements CustomPacketPayload {
    public static final Type<OpenLedgerPayload> TYPE = new Type<>(TrophyPedestalsMod.id("open_ledger"));

    @Override
    public Type<OpenLedgerPayload> type() {
        return TYPE;
    }
}
