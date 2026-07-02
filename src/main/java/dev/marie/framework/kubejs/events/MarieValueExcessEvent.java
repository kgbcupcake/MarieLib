package dev.marie.MariesLib.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieEvents;

@ApiStatus.Experimental
public class MarieValueExcessEvent implements KubeEvent {

    private String playerId;
    private String valueKey;

    public MarieValueExcessEvent() {}

    public MarieValueExcessEvent(MarieEvents.ValueExcessEvent event) {
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
