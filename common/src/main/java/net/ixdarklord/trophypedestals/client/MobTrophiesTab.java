package net.ixdarklord.trophypedestals.client;

import net.ixdarklord.coolcatcore.api.config.ConfigEvents;
import net.ixdarklord.coolcatcore.api.event.v2.client.ClientPlayerEvents;
import net.ixdarklord.trophypedestals.TrophyPedestalsConfig;
import net.ixdarklord.trophypedestals.pedestal.MobTrophies;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;

public final class MobTrophiesTab {
    private MobTrophiesTab() {}

    public static void init() {
        ConfigEvents.CHANGED.register((config, changed) -> {
            if (changed.contains(TrophyPedestalsConfig.MOB_TROPHY_CHANCE_OVERRIDES)) Minecraft.getInstance().execute(MobTrophiesTab::rebuild);
        });
        ClientPlayerEvents.LEAVE.register(player -> MobTrophies.forgetAllMobs());
    }

    private static void rebuild() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        Pedestals.MOB_TROPHIES_TAB.get().buildContents(new CreativeModeTab.ItemDisplayParameters(minecraft.level.enabledFeatures(), false, minecraft.level.registryAccess()));
    }
}
