package dev.marie.MariesLib.compat.kubejs;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieEvents;
import dev.marie.MariesLib.api.ValueModifierEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Bridges MarieLib NeoForge events into KubeJS handlers declared by {@link MarieKubeJSEvents}.
 */
@ApiStatus.Internal
public final class MarieKubeJSEventBridge {

    private static boolean registered;

    private MarieKubeJSEventBridge() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.register(new MarieKubeJSEventBridge());
    }

    @SubscribeEvent
    public void onValueChanged(MarieEvents.ValueChangedEvent event) {
        MarieKubeJSEvents.valueChanged.post(new ValueChangedKubeEvent(event));
    }

    @SubscribeEvent
    public void onValueCritical(MarieEvents.ValueCriticalEvent event) {
        MarieKubeJSEvents.valueCritical.post(new ValueCriticalKubeEvent(event));
    }

    @SubscribeEvent
    public void onSourceApplied(MarieEvents.SourceAppliedEvent event) {
        MarieKubeJSEvents.sourceApplied.post(new SourceAppliedKubeEvent(event));
    }

    @SubscribeEvent
    public void onValueModifier(ValueModifierEvent event) {
        ValueModifierKubeEvent wrapped = new ValueModifierKubeEvent(event);
        MarieKubeJSEvents.valueModifier.post(wrapped);
        event.setCanceled(wrapped.cancelled);
        event.setAmount(wrapped.amount);
    }

    public static final class ValueChangedKubeEvent implements KubeEvent {
        public ServerPlayer player;
        public String valueKey;
        public float oldValue;
        public float newValue;

        public ValueChangedKubeEvent() {}

        ValueChangedKubeEvent(MarieEvents.ValueChangedEvent event) {
            this.player = (ServerPlayer) event.getPlayer();
            this.valueKey = event.getValueKey();
            this.oldValue = event.getOldValue();
            this.newValue = event.getNewValue();
        }
    }

    public static final class ValueCriticalKubeEvent implements KubeEvent {
        public ServerPlayer player;
        public String valueKey;

        public ValueCriticalKubeEvent() {}

        ValueCriticalKubeEvent(MarieEvents.ValueCriticalEvent event) {
            this.player = (ServerPlayer) event.getPlayer();
            this.valueKey = event.getValueKey();
        }
    }

    public static final class SourceAppliedKubeEvent implements KubeEvent {
        public ServerPlayer player;
        public String sourceId;
        public String valueKey;
        public float amount;

        public SourceAppliedKubeEvent() {}

        SourceAppliedKubeEvent(MarieEvents.SourceAppliedEvent event) {
            this.player = (ServerPlayer) event.getPlayer();
            this.sourceId = event.getSourceId().toString();
            this.valueKey = event.getValueKey();
            this.amount = event.getAmount();
        }
    }

    public static final class ValueModifierKubeEvent implements KubeEvent {
        public ServerPlayer player;
        public String sourceId;
        public String valueKey;
        public float amount;
        public boolean cancelled;

        public ValueModifierKubeEvent() {}

        ValueModifierKubeEvent(ValueModifierEvent event) {
            this.player = (ServerPlayer) event.getPlayer();
            this.sourceId = event.getSourceId().toString();
            this.valueKey = event.getValueKey();
            this.amount = event.getAmount();
            this.cancelled = event.isCanceled();
        }
    }
}
