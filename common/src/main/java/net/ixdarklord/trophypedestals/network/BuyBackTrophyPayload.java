package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.feat.TrophyLedger;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/** From a client's Mob Trophies Ledger: buy back a trophy of this mob (a gilded one, if {@code gilded}). See {@link TrophyLedger#buyBack}. */
public record BuyBackTrophyPayload(Identifier mob, boolean gilded) implements CustomPacketPayload {
    public static final Type<BuyBackTrophyPayload> TYPE = new Type<>(TrophyPedestalsMod.id("buy_back_trophy"));

    @Override
    public Type<BuyBackTrophyPayload> type() {
        return TYPE;
    }

    public void apply(ServerPlayer player) {
        TrophyLedger.buyBack(player, mob, gilded);
    }
}
