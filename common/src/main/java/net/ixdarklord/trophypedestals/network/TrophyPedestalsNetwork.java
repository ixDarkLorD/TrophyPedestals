package net.ixdarklord.trophypedestals.network;

import net.ixdarklord.coolcatcore.api.network.Network;
import net.ixdarklord.trophypedestals.client.PedestalScreen;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers the payloads, on both sides as CoolCatLib requires. The clientbound receivers run on the client only, so
 * the client classes they name are never loaded on a dedicated server.
 */
public final class TrophyPedestalsNetwork {
    private TrophyPedestalsNetwork() {}

    public static void register() {
        Network.registerClientbound(OpenPedestalPayload.class, (payload, context) -> PedestalScreen.open(payload.pos()));
        Network.registerServerbound(AdjustPedestalPayload.class, (payload, context) -> context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) payload.apply(player);
        }));
        Network.registerClientbound(OpenDisplayedTrophyPayload.class, (payload, context) -> PedestalScreen.openDisplayed(payload.pos(), payload.slot()));
        Network.registerServerbound(AdjustDisplayedTrophyPayload.class, (payload, context) -> context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) payload.apply(player);
        }));
    }
}
