package net.ixdarklord.trophypedestals.neoforge.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptType;

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
        if (bindings.type() == ScriptType.SERVER) bindings.add("TrophyPedestals", TrophyPedestalsBinding.INSTANCE);
    }
}
