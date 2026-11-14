package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * To a client: open the Mob Trophies Ledger, with the mobs they've collected a trophy of, and a gilded one of, and where
 * each mob they haven't collected spawns (its hint on hover).
 */
public record OpenLedgerPayload(List<ResourceLocation> collected, List<ResourceLocation> gilded, List<Habitat> habitats) implements CustomPacketPayload {
    public static final Type<OpenLedgerPayload> TYPE = new Type<>(TrophyPedestalsMod.id("open_ledger"));

    /** A mob, and the biomes it spawns in by itself. */
    public record Habitat(ResourceLocation mob, List<ResourceLocation> biomes) {}

    @Override
    public Type<OpenLedgerPayload> type() {
        return TYPE;
    }
}
