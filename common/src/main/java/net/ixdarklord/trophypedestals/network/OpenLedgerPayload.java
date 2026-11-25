package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

public record OpenLedgerPayload(List<Identifier> collected, List<Identifier> gilded, List<Habitat> habitats) implements CustomPacketPayload {
    public static final Type<OpenLedgerPayload> TYPE = new Type<>(TrophyPedestalsMod.id("open_ledger"));

    public record Habitat(Identifier mob, List<Identifier> biomes) {}

    @Override
    public Type<OpenLedgerPayload> type() {
        return TYPE;
    }
}
