package net.ixdarklord.trophypedestals.forge.compat.rei;

import me.shedaniel.rei.forge.REIPluginClient;
import net.ixdarklord.trophypedestals.compat.rei.TrophyPedestalsReiPlugin;

/** How REI finds Trophy Pedestals' plugin on Forge: its annotation is only in REI's Forge API. */
@REIPluginClient
public final class TrophyPedestalsReiPluginForge extends TrophyPedestalsReiPlugin {}
