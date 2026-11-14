package net.ixdarklord.trophypedestals;

import net.ixdarklord.coolcatcore.api.core.ClientModConstructor;
import net.ixdarklord.trophypedestals.client.CustomPedestalItemRenderer;
import net.ixdarklord.trophypedestals.client.DisplayTrimItemRenderer;
import net.ixdarklord.trophypedestals.client.PedestalRenderer;
import net.ixdarklord.trophypedestals.client.TrophyIconRenderer;
import net.ixdarklord.trophypedestals.client.TrophyPedestalsClientConfig;

/**
 * Client mod constructor (CoolCatLib). Fabric lists it under the {@code coolcatlib:client} entrypoint, and NeoForge's
 * {@code TrophyPedestalsNeoForge} passes it to {@code client(...)}.
 */
public final class TrophyPedestalsClient implements ClientModConstructor {
    @Override
    public void onConstructMod() {
        TrophyPedestalsClientConfig.init();
        PedestalRenderer.init();
        TrophyIconRenderer.init();
        CustomPedestalItemRenderer.init();
        DisplayTrimItemRenderer.init();
    }
}
