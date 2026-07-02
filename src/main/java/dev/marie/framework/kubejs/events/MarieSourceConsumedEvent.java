package dev.marie.framework.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.MarieEvents;

@ApiStatus.Experimental
public class MarieSourceConsumedEvent implements KubeEvent {

    private String playerId;
    private String itemId;
    private String valueKey;
    private float amount;

    public MarieSourceConsumedEvent() {}

    public MarieSourceConsumedEvent(MarieEvents.SourceAppliedEvent event) {
        this.playerId = event.getPlayer().getUUID().toString();
        this.itemId = event.getSourceId().toString();
        this.valueKey = event.getValueKey();
        this.amount = event.getAmount();
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getValueKey() {
        return valueKey;
    }

    public float getAmount() {
        return amount;
    }
}
