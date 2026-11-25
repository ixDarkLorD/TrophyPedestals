package net.ixdarklord.trophypedestals.forge.compat.kubejs;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;

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
        if (event.getType() == ScriptType.SERVER) event.add("TrophyPedestals", TrophyPedestalsBinding.INSTANCE);
    }
}
