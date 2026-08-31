package dev.marie.framework.tracking.tracker.network;

import dev.marie.framework.tracking.TrackingData;
import dev.marie.framework.tracking.tracker.definition.TrackerHistoryEntry;
import dev.marie.framework.tracking.tracker.definition.TrackingPeriodState;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for the on-login/reconnect snapshot data-loss bug where a tracker that had
 * accumulated a value but had no history and no opened period (the plausible case: a CUSTOM-period
 * tracker mid-accumulation) was dropped from the full snapshot entirely, so its current value never
 * reached the client.
 */
class TrackerNetworkingBuildEntryTest {

    private final ResourceLocation trackerId =
            ResourceLocation.fromNamespaceAndPath("marieslib_test", "custom_tracker");

    @Test
    void accumulatorOnlyTrackerStillProducesAnEntryWithItsCurrentValue() {
        TrackingData tracking = new TrackingData();
        tracking.trackingAccumulators.put(trackerId, 7.5f);

        CompoundTag entry = TrackerNetworking.buildTrackerEntry(trackerId, tracking);

        assertNotNull(entry, "a tracker with a nonzero accumulator must not be dropped from the snapshot");
        assertEquals(7.5f, entry.getFloat("current"),
                "the accumulated in-progress value must reach the client");
        assertTrue(entry.getList("history", net.minecraft.nbt.Tag.TAG_COMPOUND).isEmpty(),
                "no history was recorded");
        assertFalse(entry.contains("period"), "no period was opened");
    }

    @Test
    void emptyTrackerStillReturnsNull() {
        TrackingData tracking = new TrackingData();

        assertNull(TrackerNetworking.buildTrackerEntry(trackerId, tracking),
                "no history, no period, no accumulated value -> nothing to send");
    }

    @Test
    void zeroAccumulatorWithHistoryStillReturnsAnEntry() {
        TrackingData tracking = new TrackingData();
        tracking.trackingHistory.put(trackerId, List.of(
                new TrackerHistoryEntry(trackerId, "custom", 0L, 100L, 3f)));

        CompoundTag entry = TrackerNetworking.buildTrackerEntry(trackerId, tracking);

        assertNotNull(entry);
        assertEquals(0f, entry.getFloat("current"),
                "current still reflects the (empty) live accumulator alongside history");
        assertEquals(1, entry.getList("history", net.minecraft.nbt.Tag.TAG_COMPOUND).size());
    }

    @Test
    void periodOnlyTrackerIsUnaffected() {
        TrackingData tracking = new TrackingData();
        tracking.trackerPeriodStates.put(trackerId, new TrackingPeriodState(0L, 24000L));

        CompoundTag entry = TrackerNetworking.buildTrackerEntry(trackerId, tracking);

        assertNotNull(entry);
        assertTrue(entry.contains("period"));
        assertEquals(0f, entry.getFloat("current"));
    }
}
