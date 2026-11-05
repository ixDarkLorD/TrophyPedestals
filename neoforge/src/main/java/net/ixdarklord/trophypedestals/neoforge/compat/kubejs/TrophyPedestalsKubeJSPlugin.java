package net.ixdarklord.trophypedestals.neoforge.compat.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptType;

/**
 * KubeJS support (NeoForge only: KubeJS has no Fabric build for 26.x): server scripts get a {@code TrophyPedestals}
 * binding to read, set and lock pedestals. KubeJS finds this plugin through {@code kubejs.plugins.txt}; nothing else
 * loads it.
 */
public final class TrophyPedestalsKubeJSPlugin implements KubeJSPlugin {
    @Override
    public void registerBindings(BindingRegistry bindings) {
        if (bindings.type() == ScriptType.SERVER) bindings.add("TrophyPedestals", TrophyPedestalsBinding.INSTANCE);
    }
}
