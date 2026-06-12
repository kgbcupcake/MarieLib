package dev.marie.MariesLib.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.MariesLib.api.ApiStatus;

@ApiStatus.Experimental
public class MarieDecayTickEvent implements KubeEvent {

    private String playerId;
    private String valueKey;
    private float amount;
    private boolean cancelled;

    public MarieDecayTickEvent() {}

    public MarieDecayTickEvent(String playerId, String valueKey, float amount) {
        this.playerId = playerId;
        this.valueKey = valueKey;
        this.amount = amount;
    }

    public String getPlayerId() {
        return playerId;
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

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
        this.amount = 0f;
    }
}
