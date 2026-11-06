package net.ixdarklord.trophypedestals.forge;

import net.ixdarklord.coolcatcore.api.core.forge.ForgeModEntrypoint;
import net.ixdarklord.trophypedestals.TrophyPedestalsClient;
import net.ixdarklord.trophypedestals.TrophyPedestalsMod;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Forge entrypoint: hands the common and client mod constructors to CoolCatLib. */
@Mod(TrophyPedestalsMod.MOD_ID)
public final class TrophyPedestalsForge extends ForgeModEntrypoint {
    public TrophyPedestalsForge(FMLJavaModLoadingContext context) {
        super(context);
        this.common(TrophyPedestalsMod::new);
        this.client(() -> TrophyPedestalsClient::new);
    }
}
