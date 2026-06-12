package dev.marie.MariesLib.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieEvents;

@ApiStatus.Experimental
public class MarieValueCriticalEvent implements KubeEvent {

    private String playerId;
    private String valueKey;

    public MarieValueCriticalEvent() {}

    public MarieValueCriticalEvent(MarieEvents.ValueCriticalEvent event) {
        this.playerId = event.getPlayer().getUUID().toString();
        this.valueKey = event.getValueKey();
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getValueKey() {
        return valueKey;
    }
}
