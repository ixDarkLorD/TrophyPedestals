package net.ixdarklord.trophypedestals.forge;

import net.ixdarklord.coolcatcore.api.core.forge.ForgeModEntrypoint;
import net.ixdarklord.trophypedestals.TrophyPedestalsClient;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.forge.compat.top.TrophyProbeForge;
import net.ixdarklord.trophypedestals.pedestal.MobTrophySearch;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(TrophyPedestalsMod.MOD_ID)
public final class TrophyPedestalsForge extends ForgeModEntrypoint {
    public TrophyPedestalsForge(FMLJavaModLoadingContext context) {
        super(context);
        MobTrophySearch.searchBar(builder -> builder.withSearchBar(MobTrophySearch.SEARCH_WIDTH));
        this.common(TrophyPedestalsMod::new);
        this.client(() -> TrophyPedestalsClient::new);
        if (ModList.get().isLoaded("theoneprobe")) TrophyProbeForge.register(context.getModEventBus());
    }
}
