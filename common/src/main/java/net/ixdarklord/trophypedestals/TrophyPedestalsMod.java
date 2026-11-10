package net.ixdarklord.trophypedestals;

import net.ixdarklord.coolcatcore.api.core.ModConstructor;
import net.ixdarklord.coolcatcore.api.gametest.GameTests;
import net.ixdarklord.coolcatcore.api.platform.Platform;
import net.ixdarklord.trophypedestals.command.TrophyPedestalsCommands;
import net.ixdarklord.trophypedestals.compat.ftb.TrophyReward;
import net.ixdarklord.trophypedestals.feat.Feats;
import net.ixdarklord.trophypedestals.gametest.TrophyPedestalsGameTests;
import net.ixdarklord.trophypedestals.network.TrophyPedestalsNetwork;
import net.ixdarklord.trophypedestals.pedestal.Leaderboards;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * Common mod constructor (CoolCatLib). Fabric lists it under the {@code coolcatlib:common} entrypoint, and
 * NeoForge's {@code TrophyPedestalsNeoForge} passes it to {@code common(...)}.
 */
public final class TrophyPedestalsMod implements ModConstructor {
    public static final String MOD_ID = "trophypedestals";
    public static final String MOD_NAME = "Trophy Pedestals";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    @Override
    public void onConstructMod() {
        LOGGER.info("Loading {} on {}", MOD_NAME, Platform.isFabric() ? "Fabric" : "NeoForge");
        TrophyPedestalsConfig.init();
        TrophyPedestalsNetwork.register();
        Pedestals.init();
        Feats.init();
        Leaderboards.init();
        TrophyPedestalsCommands.init();
        // Optional FTB Quests: a reward that gives a trophy (compat/ftb).
        if (Platform.isModLoaded("ftbquests")) TrophyReward.register();

        if (GameTests.isEnabled()) {
            LOGGER.info("Registered game tests {}", GameTests.register(MOD_ID, TrophyPedestalsGameTests.class));
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path.toLowerCase(Locale.ROOT));
    }
}
