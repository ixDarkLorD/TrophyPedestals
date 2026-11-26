package net.ixdarklord.trophypedestals.neoforge.compat.kubegroovy;

import dev.uncandango.kubegroovy.event.EventGroupRegistry;
import dev.uncandango.kubegroovy.plugin.KubeGroovyPlugin;
import dev.uncandango.kubegroovy.script.BindingRegistry;
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
        // every script type: startup scripts add trophies to the creative tab, server scripts change pedestals
        bindings.add("TrophyPedestals", TrophyPedestalsBinding.INSTANCE);
    }
}
