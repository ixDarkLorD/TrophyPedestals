package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * To a client: open the Mob Trophies Ledger, with the mobs they've collected a trophy of, and a gilded one of, and where
 * each mob they haven't collected spawns (its hint on hover).
 */
public record OpenLedgerPayload(List<Identifier> collected, List<Identifier> gilded, List<Habitat> habitats) implements CustomPacketPayload {
    public static final Type<OpenLedgerPayload> TYPE = new Type<>(TrophyPedestalsMod.id("open_ledger"));

    /** A mob, and the biomes it spawns in by itself. */
    public record Habitat(Identifier mob, List<Identifier> biomes) {}

    @Override
    public Type<OpenLedgerPayload> type() {
        return TYPE;
    }
}
