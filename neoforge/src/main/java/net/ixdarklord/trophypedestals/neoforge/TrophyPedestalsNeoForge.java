package net.ixdarklord.trophypedestals.neoforge;

import net.ixdarklord.coolcatcore.api.core.neoforge.NeoForgeModEntrypoint;
import net.ixdarklord.trophypedestals.TrophyPedestalsClient;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.neoforge.client.MobTrophySearchNeoForge;
import net.ixdarklord.trophypedestals.neoforge.compat.top.TrophyProbeNeoForge;
import net.ixdarklord.trophypedestals.pedestal.MobTrophySearch;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(TrophyPedestalsMod.MOD_ID)
public final class TrophyPedestalsNeoForge extends NeoForgeModEntrypoint {
    public TrophyPedestalsNeoForge(ModContainer container) {
        super(container);
        MobTrophySearch.searchBar(builder -> builder.withSearchBar(MobTrophySearch.SEARCH_WIDTH));
        this.common(TrophyPedestalsMod::new);
        this.client(() -> TrophyPedestalsClient::new);
        if (FMLEnvironment.getDist().isClient()) MobTrophySearchNeoForge.init();
        if (ModList.get().isLoaded("theoneprobe")) TrophyProbeNeoForge.register(container.getEventBus());
    }
}
