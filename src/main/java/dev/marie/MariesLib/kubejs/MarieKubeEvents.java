package dev.marie.MariesLib.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.kubejs.events.MarieDecayTickEvent;
import dev.marie.MariesLib.kubejs.events.MarieMilestoneTriggeredEvent;
import dev.marie.MariesLib.kubejs.events.MariePlayerSyncedEvent;
import dev.marie.MariesLib.kubejs.events.MarieSourceConsumedEvent;
import dev.marie.MariesLib.kubejs.events.MarieValueChangedEvent;
import dev.marie.MariesLib.kubejs.events.MarieValueCriticalEvent;
import dev.marie.MariesLib.kubejs.events.MarieValueDeltaModifierEvent;
import dev.marie.MariesLib.kubejs.events.MarieValueExcessEvent;

import javax.annotation.Nullable;

/**
 * KubeJS event group and handler definitions for MarieLib scripting.
 */
@ApiStatus.Experimental
public final class MarieKubeEvents {

    public static final String VALUE_CHANGED_ID = "MarieEvents.valueChanged";
    public static final String VALUE_CRITICAL_ID = "MarieEvents.valueCritical";
    public static final String VALUE_EXCESS_ID = "MarieEvents.valueExcess";
    public static final String SOURCE_CONSUMED_ID = "MarieEvents.sourceConsumed";
    public static final String MILESTONE_TRIGGERED_ID = "MarieEvents.milestoneTriggered";
    public static final String VALUE_DELTA_MODIFIER_ID = "MarieEvents.valueDeltaModifier";
    public static final String DECAY_TICK_ID = "MarieEvents.decayTick";
    public static final String PLAYER_SYNCED_ID = "MarieEvents.playerSynced";

    public static final EventGroup GROUP = EventGroup.of("MarieEvents");

    public static final EventHandler VALUE_CHANGED =
            GROUP.server("valueChanged", () -> MarieValueChangedEvent.class);
    public static final EventHandler VALUE_CRITICAL =
            GROUP.server("valueCritical", () -> MarieValueCriticalEvent.class);
    public static final EventHandler VALUE_EXCESS =
            GROUP.server("valueExcess", () -> MarieValueExcessEvent.class);
    public static final EventHandler SOURCE_CONSUMED =
            GROUP.server("sourceConsumed", () -> MarieSourceConsumedEvent.class);
    public static final EventHandler MILESTONE_TRIGGERED =
            GROUP.server("milestoneTriggered", () -> MarieMilestoneTriggeredEvent.class);
    public static final EventHandler VALUE_DELTA_MODIFIER =
            GROUP.server("valueDeltaModifier", () -> MarieValueDeltaModifierEvent.class);
    public static final EventHandler DECAY_TICK =
            GROUP.server("decayTick", () -> MarieDecayTickEvent.class);
    public static final EventHandler PLAYER_SYNCED =
            GROUP.server("playerSynced", () -> MariePlayerSyncedEvent.class);

    private MarieKubeEvents() {}

    @Nullable
    public static EventHandler handlerFor(String eventId) {
        return switch (eventId) {
            case VALUE_CHANGED_ID -> VALUE_CHANGED;
            case VALUE_CRITICAL_ID -> VALUE_CRITICAL;
            case VALUE_EXCESS_ID -> VALUE_EXCESS;
            case SOURCE_CONSUMED_ID -> SOURCE_CONSUMED;
            case MILESTONE_TRIGGERED_ID -> MILESTONE_TRIGGERED;
            case VALUE_DELTA_MODIFIER_ID -> VALUE_DELTA_MODIFIER;
            case DECAY_TICK_ID -> DECAY_TICK;
            case PLAYER_SYNCED_ID -> PLAYER_SYNCED;
            default -> null;
        };
    }
}
