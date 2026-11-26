package net.ixdarklord.trophypedestals.forge.compat.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;

public final class TrophyPedestalsKubeJSPlugin extends KubeJSPlugin {
    @Override
    public void init() {
        TrophyPedestalsKubeEvents.init();
    }

    @Override
    public void registerEvents() {
        TrophyPedestalsKubeEvents.GROUP.register();
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        // every script type: startup scripts add trophies to the creative tab, server scripts change pedestals
        event.add("TrophyPedestals", TrophyPedestalsBinding.INSTANCE);
    }
}
