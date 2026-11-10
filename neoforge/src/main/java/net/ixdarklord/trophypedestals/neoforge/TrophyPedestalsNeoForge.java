package net.ixdarklord.trophypedestals.neoforge;

import net.ixdarklord.coolcatcore.api.core.neoforge.NeoForgeModEntrypoint;
import net.ixdarklord.trophypedestals.TrophyPedestalsClient;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.ixdarklord.trophypedestals.neoforge.compat.top.TrophyProbeNeoForge;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;

/** NeoForge entrypoint: hands the common and client mod constructors to CoolCatLib. */
@Mod(TrophyPedestalsMod.MOD_ID)
public final class TrophyPedestalsNeoForge extends NeoForgeModEntrypoint {
    public TrophyPedestalsNeoForge(ModContainer container) {
        super(container);
        this.common(TrophyPedestalsMod::new);
        this.client(() -> TrophyPedestalsClient::new);
        // Optional The One Probe: registered through NeoForge's inter-mod messages (compat/top).
        if (ModList.get().isLoaded("theoneprobe")) TrophyProbeNeoForge.register(container.getEventBus());
    }
}
