package net.ixdarklord.trophypedestals.client;

import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientTickEvents;

/**
 * Offers the pedestals' coloured lighting each time a player joins a world, while it's off and they haven't asked not to
 * be asked again ({@link LightingPromptScreen}). The popup waits for the world to finish loading, so it opens over the
 * world rather than the loading screen.
 */
public final class LightingPrompt {
    private static boolean pending;

    private LightingPrompt() {}

    public static void init() {
        ClientPlayerEvents.JOIN.register(player -> pending = true);
        ClientPlayerEvents.LEAVE.register(player -> pending = false);
        ClientTickEvents.END.register(minecraft -> {
            if (!pending || minecraft.player == null || minecraft.screen != null || minecraft.getOverlay() != null) return;
            pending = false;
            if (!TrophyPedestalsClientConfig.PEDESTAL_LIGHTING.get() && TrophyPedestalsClientConfig.LIGHTING_PROMPT.get()) {
                minecraft.setScreen(new LightingPromptScreen());
            }
        });
    }
}
