package dev.marie.framework.kubejs.internal;


import dev.latvian.mods.kubejs.script.ScriptsLoadedEvent;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.marie.MarieEvents;
import dev.marie.framework.kubejs.MarieKubeEvents;
import dev.marie.framework.kubejs.events.MarieMilestoneTriggeredEvent;
import dev.marie.framework.kubejs.events.MarieSourceConsumedEvent;
import dev.marie.framework.kubejs.events.MarieValueChangedEvent;
import dev.marie.framework.kubejs.events.MarieValueCriticalEvent;
import dev.marie.framework.kubejs.events.MarieValueExcessEvent;
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
        NeoForge.EVENT_BUS.addListener(KubeEventBridge::onMilestoneTriggered);
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

    private static void onMilestoneTriggered(MarieEvents.MilestoneTriggeredEvent event) {
        if (!KubeGuard.hasListeners(MarieKubeEvents.MILESTONE_TRIGGERED_ID)) {
            return;
        }
        MarieKubeEvents.MILESTONE_TRIGGERED.post(new MarieMilestoneTriggeredEvent(event));
    }

    private static void onScriptReload(ScriptsLoadedEvent event) {
        KubeGuard.invalidateCache();
    }
}
