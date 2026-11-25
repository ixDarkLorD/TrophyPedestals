package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record OpenLedgerPayload(List<ResourceLocation> collected, List<ResourceLocation> gilded, List<Habitat> habitats) implements CustomPacketPayload {
    public static final Type<OpenLedgerPayload> TYPE = new Type<>(TrophyPedestalsMod.id("open_ledger"));

    public record Habitat(ResourceLocation mob, List<ResourceLocation> biomes) {}

    @Override
    public Type<OpenLedgerPayload> type() {
        return TYPE;
    }
}
