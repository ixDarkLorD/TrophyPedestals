package net.ixdarklord.trophypedestals.neoforge.compat.kubegroovy;

import dev.uncandango.kubegroovy.event.EventGroupRegistry;
import dev.uncandango.kubegroovy.plugin.KubeGroovyPlugin;
import dev.uncandango.kubegroovy.script.BindingRegistry;
import dev.uncandango.kubegroovy.script.ScriptType;
import net.ixdarklord.trophypedestals.neoforge.compat.kubejs.TrophyPedestalsBinding;

public final class TrophyPedestalsKubeGroovyPlugin implements KubeGroovyPlugin {
    @Override
    public void init() {
        TrophyPedestalsKubeGroovyEvents.init();
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(TrophyPedestalsKubeGroovyEvents.GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        if (bindings.type() == ScriptType.SERVER) bindings.add("TrophyPedestals", TrophyPedestalsBinding.INSTANCE);
    }
}
