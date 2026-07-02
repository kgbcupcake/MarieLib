package dev.marie.MariesLib.kubejs;

import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.latvian.mods.kubejs.script.ScriptManager;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.kubejs.bindings.MarieKubeBindings;
import dev.marie.MariesLib.kubejs.bindings.MarieKubeServerBindings;
import dev.marie.MariesLib.kubejs.internal.KubeGuard;

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
