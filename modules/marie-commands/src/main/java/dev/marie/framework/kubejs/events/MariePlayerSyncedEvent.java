package dev.marie.framework.kubejs.events;

// TODO(marie-core migration): depends on dev.marie.framework.{api} (not yet migrated to marie-core; module will not compile until that lands)

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.framework.api.ApiStatus;

@ApiStatus.Experimental
public class MariePlayerSyncedEvent implements KubeEvent {

    private String playerId;

    public MariePlayerSyncedEvent() {}

    public MariePlayerSyncedEvent(String playerId) {
        this.playerId = playerId;
    }

    public String getPlayerId() {
        return playerId;
    }
}
