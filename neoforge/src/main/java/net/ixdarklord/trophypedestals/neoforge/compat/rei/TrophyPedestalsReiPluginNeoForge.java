package net.ixdarklord.trophypedestals.neoforge.compat.rei;

import me.shedaniel.rei.forge.REIPluginClient;
import net.ixdarklord.trophypedestals.compat.rei.TrophyPedestalsReiPlugin;

/** How REI finds Trophy Pedestals' plugin on NeoForge: its annotation is only in REI's NeoForge API. */
@REIPluginClient
public final class TrophyPedestalsReiPluginNeoForge extends TrophyPedestalsReiPlugin {}
