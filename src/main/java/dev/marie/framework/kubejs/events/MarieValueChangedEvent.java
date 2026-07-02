package dev.marie.framework.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.MarieEvents;

@ApiStatus.Experimental
public class MarieValueChangedEvent implements KubeEvent {

    private String playerId;
    private String valueKey;
    private float oldValue;
    private float newValue;

    public MarieValueChangedEvent() {}

    public MarieValueChangedEvent(MarieEvents.ValueChangedEvent event) {
        this.playerId = event.getPlayer().getUUID().toString();
        this.valueKey = event.getValueKey();
        this.oldValue = event.getOldValue();
        this.newValue = event.getNewValue();
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getValueKey() {
        return valueKey;
    }

    public float getOldValue() {
        return oldValue;
    }

    public float getNewValue() {
        return newValue;
    }
}
