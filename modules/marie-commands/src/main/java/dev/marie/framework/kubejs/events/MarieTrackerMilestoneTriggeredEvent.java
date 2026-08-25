package dev.marie.framework.kubejs.events;


import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.marie.MarieEvents;

/**
 * KubeJS-facing wrapper around {@link MarieEvents.TrackerMilestoneTriggeredEvent}, exposing the
 * triggering player, milestone, tracker, and value as plain scriptable fields. Sibling to
 * {@link MarieMilestoneTriggeredEvent} for the generic MarieLib tracker milestone system.
 */
@ApiStatus.Experimental
public class MarieTrackerMilestoneTriggeredEvent implements KubeEvent {

    private String playerId;
    private String milestoneId;
    private String trackerId;
    private float currentValue;
    private float goal;

    public MarieTrackerMilestoneTriggeredEvent() {}

    public MarieTrackerMilestoneTriggeredEvent(MarieEvents.TrackerMilestoneTriggeredEvent event) {
        this.playerId = event.getPlayer().getUUID().toString();
        this.milestoneId = event.getMilestone().getId();
        this.trackerId = event.getTrackerId().toString();
        this.currentValue = event.getCurrentValue();
        this.goal = event.getMilestone().getGoal();
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getMilestoneId() {
        return milestoneId;
    }

    public String getTrackerId() {
        return trackerId;
    }

    public float getCurrentValue() {
        return currentValue;
    }

    public float getGoal() {
        return goal;
    }
}
