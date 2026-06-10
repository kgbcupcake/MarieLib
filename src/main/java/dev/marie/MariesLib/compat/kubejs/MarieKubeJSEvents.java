package dev.marie.MariesLib.compat.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.marie.MariesLib.api.ApiStatus;

@ApiStatus.Internal
public final class MarieKubeJSEvents {

    public static final EventGroup GROUP = EventGroup.of("MarieEvents");

    static final EventHandler valueChanged = GROUP.server("valueChanged", () -> MarieKubeJSEventBridge.ValueChangedKubeEvent.class);
    static final EventHandler valueCritical = GROUP.server("valueCritical", () -> MarieKubeJSEventBridge.ValueCriticalKubeEvent.class);
    static final EventHandler sourceApplied = GROUP.server("sourceApplied", () -> MarieKubeJSEventBridge.SourceAppliedKubeEvent.class);
    static final EventHandler valueModifier = GROUP.server("valueModifier", () -> MarieKubeJSEventBridge.ValueModifierKubeEvent.class);

    private MarieKubeJSEvents() {}
}
