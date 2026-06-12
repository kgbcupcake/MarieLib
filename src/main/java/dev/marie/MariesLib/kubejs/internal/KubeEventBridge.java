package dev.marie.MariesLib.kubejs.internal;

import dev.latvian.mods.kubejs.script.ScriptsLoadedEvent;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieEvents;
import dev.marie.MariesLib.kubejs.MarieKubeEvents;
import dev.marie.MariesLib.kubejs.events.MarieSourceConsumedEvent;
import dev.marie.MariesLib.kubejs.events.MarieValueChangedEvent;
import dev.marie.MariesLib.kubejs.events.MarieValueCriticalEvent;
import dev.marie.MariesLib.kubejs.events.MarieValueExcessEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Bridges MarieLib NeoForge events into KubeJS script handlers.
 */
@ApiStatus.Internal
public final class KubeEventBridge {

    private static boolean registered;

    private KubeEventBridge() {}

    public static void register() {
        if (!KubeGuard.isPresent() || registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(KubeEventBridge::onValueChanged);
        NeoForge.EVENT_BUS.addListener(KubeEventBridge::onValueCritical);
        NeoForge.EVENT_BUS.addListener(KubeEventBridge::onValueExcess);
        NeoForge.EVENT_BUS.addListener(KubeEventBridge::onSourceConsumed);
        NeoForge.EVENT_BUS.addListener(KubeEventBridge::onScriptReload);
    }

    private static void onValueChanged(MarieEvents.ValueChangedEvent event) {
        if (!KubeGuard.hasListeners(MarieKubeEvents.VALUE_CHANGED_ID)) {
            return;
        }
        MarieKubeEvents.VALUE_CHANGED.post(new MarieValueChangedEvent(event));
    }

    private static void onValueCritical(MarieEvents.ValueCriticalEvent event) {
        if (!KubeGuard.hasListeners(MarieKubeEvents.VALUE_CRITICAL_ID)) {
            return;
        }
        MarieKubeEvents.VALUE_CRITICAL.post(new MarieValueCriticalEvent(event));
    }

    private static void onValueExcess(MarieEvents.ValueExcessEvent event) {
        if (!KubeGuard.hasListeners(MarieKubeEvents.VALUE_EXCESS_ID)) {
            return;
        }
        MarieKubeEvents.VALUE_EXCESS.post(new MarieValueExcessEvent(event));
    }

    private static void onSourceConsumed(MarieEvents.SourceAppliedEvent event) {
        if (!KubeGuard.hasListeners(MarieKubeEvents.SOURCE_CONSUMED_ID)) {
            return;
        }
        MarieKubeEvents.SOURCE_CONSUMED.post(new MarieSourceConsumedEvent(event));
    }

    private static void onScriptReload(ScriptsLoadedEvent event) {
        KubeGuard.invalidateCache();
    }
}
