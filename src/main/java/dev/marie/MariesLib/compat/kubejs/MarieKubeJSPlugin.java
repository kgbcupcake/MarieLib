package dev.marie.MariesLib.compat.kubejs;

import dev.latvian.mods.kubejs.plugin.KubeJSPlugin;
import dev.latvian.mods.kubejs.plugin.KubeJSPlugins;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.script.BindingRegistry;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.core.MarieLibContext;
import dev.marie.MariesLib.core.MariesLib;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.stream.Stream;

/**
 * MarieLib KubeJS plugin entrypoint.
 *
 * <p>KubeJS 2101 discovers plugins from each mod jar's {@code kubejs.plugins.txt} (not
 * {@code META-INF/services}). When that file or service-loader discovery fails (e.g. under
 * Architectury), {@link #bootstrap()} registers this plugin via {@link KubeJSPlugins} internal
 * {@code loadFromFile}.</p>
 */
@ApiStatus.Internal
public final class MarieKubeJSPlugin implements KubeJSPlugin {

    private static final String PLUGIN_LINE = MarieKubeJSPlugin.class.getName() + " " + MarieLibContext.get().modId();

    private static boolean manualRegistrationAttempted;

    /**
     * Ensures this plugin is present in {@link KubeJSPlugins} when automatic discovery failed.
     * KubeJS 2101 has no public {@code register()} API; the supported manual path is
     * {@code kubejs.plugins.txt} plus this {@code loadFromFile} fallback.
     */
    public static void bootstrap() {
        if (!ModList.get().isLoaded("kubejs")) {
            return;
        }
        if (isRegistered()) {
            return;
        }
        if (manualRegistrationAttempted) {
            return;
        }
        manualRegistrationAttempted = true;

        try {
            registerViaKubeJSPluginsLoadFromFile();
            if (isRegistered()) {
                MariesLib.LOGGER.info(
                        "[MarieLib] Manually registered KubeJS plugin (kubejs.plugins.txt / service-loader discovery missed).");
                initializePluginLifecycle();
            } else {
                MariesLib.LOGGER.warn("[MarieLib] Manual KubeJS plugin registration did not add MarieKubeJSPlugin.");
            }
        } catch (Throwable t) {
            MariesLib.LOGGER.warn("[MarieLib] Failed to manually register KubeJS plugin.", t);
        }
    }

    public static boolean isRegistered() {
        for (KubeJSPlugin plugin : KubeJSPlugins.getAll()) {
            if (plugin.getClass() == MarieKubeJSPlugin.class) {
                return true;
            }
        }
        return false;
    }

    /**
     * Uses the same code path as {@code kubejs.plugins.txt} parsing inside KubeJS 2101.
     */
    private static void registerViaKubeJSPluginsLoadFromFile() throws ReflectiveOperationException {
        Method loadFromFile = KubeJSPlugins.class.getDeclaredMethod(
                "loadFromFile", Stream.class, String.class, boolean.class);
        loadFromFile.setAccessible(true);
        loadFromFile.invoke(null, Stream.of(PLUGIN_LINE), MarieLibContext.get().modId(), false);
    }

    /**
     * If registration happened after KubeJS already ran plugin init, run init hooks on this plugin.
     */
    private static void initializePluginLifecycle() {
        for (KubeJSPlugin plugin : KubeJSPlugins.getAll()) {
            if (plugin.getClass() == MarieKubeJSPlugin.class) {
                plugin.init();
                plugin.initStartup();
                return;
            }
        }
    }

    @Override
    public void registerBindings(BindingRegistry bindings) {
        if (!ModList.get().isLoaded("kubejs")) {
            return;
        }
        bindings.add(MarieKubeJSBindings.API_BINDING, MarieKubeJSBindings.createBindingObject());
        bindings.add(MarieKubeJSBindings.EVENTS_BINDING, MarieKubeJSBindings.createEventsBindingObject(bindings.type()));
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        if (!ModList.get().isLoaded("kubejs")) {
            return;
        }
        registry.register(MarieKubeJSEvents.GROUP);
        MarieKubeJSEventBridge.register();
        MarieKubeJSStartupEvents.register(registry);
    }
}
