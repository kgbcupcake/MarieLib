package dev.marie.MariesLib.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.MariesLib.api.ApiStatus;

@ApiStatus.Experimental
public class MarieValueDeltaModifierEvent implements KubeEvent {

    private final String playerId;
    private final String itemId;
    private final String valueKey;
    private float amount;

    public MarieValueDeltaModifierEvent() {
        this.playerId = "";
        this.itemId = "";
        this.valueKey = "";
    }

    public MarieValueDeltaModifierEvent(String playerId, String itemId, String valueKey, float amount) {
        this.playerId = playerId;
        this.itemId = itemId;
        this.valueKey = valueKey;
        this.amount = amount;
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

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public void cancel() {
        this.amount = 0f;
    }
}
