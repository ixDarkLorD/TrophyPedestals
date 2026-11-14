package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.coolcatcore.api.network.CustomPacketPayload;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.feat.TrophyLedger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** From a client's Mob Trophies Ledger: buy back a trophy of this mob (a gilded one, if {@code gilded}). See {@link TrophyLedger#buyBack}. */
public record BuyBackTrophyPayload(ResourceLocation mob, boolean gilded) implements CustomPacketPayload {
    public static final Type<BuyBackTrophyPayload> TYPE = new Type<>(TrophyPedestalsMod.id("buy_back_trophy"));

    @Override
    public Type<BuyBackTrophyPayload> type() {
        return TYPE;
    }

    public void apply(ServerPlayer player) {
        TrophyLedger.buyBack(player, mob, gilded);
    }
}
