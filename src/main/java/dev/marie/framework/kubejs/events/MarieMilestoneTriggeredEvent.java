package dev.marie.MariesLib.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MarieEvents;

@ApiStatus.Experimental
public class MarieMilestoneTriggeredEvent implements KubeEvent {

    private String playerId;
    private String milestoneId;
    private String valueKey;
    private float cumulativeIntake;
    private float cumulativeGoal;

    public MarieMilestoneTriggeredEvent() {}

    public MarieMilestoneTriggeredEvent(MarieEvents.MilestoneTriggeredEvent event) {
        this.playerId = event.getPlayer().getUUID().toString();
        this.milestoneId = event.getMilestone().getId();
        this.valueKey = event.getValueKey();
        this.cumulativeIntake = event.getCumulativeIntake();
        this.cumulativeGoal = event.getMilestone().getCumulativeGoal();
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getMilestoneId() {
        return milestoneId;
    }

    public String getValueKey() {
        return valueKey;
    }

    public float getCumulativeIntake() {
        return cumulativeIntake;
    }

    public float getCumulativeGoal() {
        return cumulativeGoal;
    }
}
