package net.ixdarklord.trophypedestals.neoforge.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;

public final class TrophyPedestalsKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void init() {
        TrophyPedestalsKubeEvents.init();
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(TrophyPedestalsKubeEvents.GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        // every script type: startup scripts add trophies to the creative tab, server scripts change pedestals
        bindings.add("TrophyPedestals", TrophyPedestalsBinding.INSTANCE);
    }
}
