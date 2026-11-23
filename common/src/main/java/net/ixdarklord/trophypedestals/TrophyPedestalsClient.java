package net.ixdarklord.trophypedestals;

import net.ixdarklord.coolcatcore.api.client.registry.ItemColorRegistry;
import net.ixdarklord.coolcatcore.api.core.ClientModConstructor;
import net.ixdarklord.trophypedestals.client.CustomPedestalItemRenderer;
import net.ixdarklord.trophypedestals.client.DisplayTrimItemRenderer;
import net.ixdarklord.trophypedestals.client.MobTrophiesTab;
import net.ixdarklord.trophypedestals.client.PedestalRenderer;
import net.ixdarklord.trophypedestals.client.TrophyIconRenderer;
import net.ixdarklord.trophypedestals.client.TrophyPedestalsClientConfig;
import net.ixdarklord.trophypedestals.compat.render.ItemDefinitions;
import net.ixdarklord.trophypedestals.pedestal.Lens;
import net.ixdarklord.trophypedestals.pedestal.Pedestals;
import net.minecraft.world.item.component.DyedItemColor;

/** Client mod constructor (CoolCatLib). */
public final class TrophyPedestalsClient implements ClientModConstructor {
    @Override
    public void onConstructMod() {
        TrophyPedestalsClientConfig.init();
        PedestalRenderer.init();
        TrophyIconRenderer.init();
        MobTrophiesTab.init();
        CustomPedestalItemRenderer.init();
        ItemDefinitions.init();
        DisplayTrimItemRenderer.init();
        // A dyed Clear Lens's glass (26.1's item definition says this with its dye tint).
        ItemColorRegistry.register((stack, layer) -> layer == 1 ? DyedItemColor.getOrDefault(stack, -1) : -1, Pedestals.LENSES.get(Lens.CLEAR));
    }
}
