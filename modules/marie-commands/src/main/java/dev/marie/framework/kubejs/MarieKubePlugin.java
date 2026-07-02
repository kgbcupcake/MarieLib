package dev.marie.framework.kubejs;

// TODO(marie-core migration): depends on dev.marie.framework.{api} (not yet migrated to marie-core; module will not compile until that lands)

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.kubejs.bindings.MarieKubeBindings;
import dev.marie.framework.kubejs.bindings.MarieKubeServerBindings;
import dev.marie.framework.kubejs.internal.KubeGuard;

/**
 * KubeJS plugin entry point for MarieLib scripting integration.
 */
@ApiStatus.Experimental
public class MarieKubePlugin implements KubeJSPlugin {

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(MarieKubeEvents.GROUP);
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        bindings.add("MarieAPI", MarieKubeBindings.class);
        if (bindings.type() == ScriptType.SERVER) {
            bindings.add("MarieServer", MarieKubeServerBindings.class);
        }
    }

    @Override
    public void beforeScriptsLoaded(ScriptManager manager) {
        KubeGuard.invalidateCache();
    }

    @Override
    public void afterScriptsLoaded(ScriptManager manager) {
        KubeGuard.invalidateCache();
    }
}
