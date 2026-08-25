package dev.marie.framework.notification;

import dev.marie.framework.api.ApiStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Owns the active notification stack: merge-or-push on trigger, eviction, and expiry.
 *
 * <p>{@code show}/{@code clear} are {@code public} only so {@code MarieNotifications} (in {@code
 * dev.marie.framework.ui.api}) can delegate to them across packages — not a general-purpose API;
 * consumer mods should go through the facade.
 */
@ApiStatus.Internal
public final class NotificationManager {

    private static final int MAX_VISIBLE = 4;

    private static final List<NotificationSlot> slots = new ArrayList<>();

    private NotificationManager() {}

    public static synchronized void show(NotificationRequest request) {
        long now = System.currentTimeMillis();
        if (request.mergeKey() != null) {
            for (NotificationSlot slot : slots) {
                if (slot.withinMergeWindow(request.mergeKey(), now)) {
                    slot.mergeIn(request.content(), now);
                    return;
                }
            }
        }
        // Newest slot goes to index 0 (closest to the XP bar); index grows upward/older.
        slots.add(0, new NotificationSlot(request, now));
        while (slots.size() > MAX_VISIBLE) {
            slots.remove(slots.size() - 1);
        }
    }

    /** Snapshot of live slots, having first dropped any slot past its full duration. */
    static synchronized List<NotificationSlot> activeSlots() {
        long now = System.currentTimeMillis();
        slots.removeIf(slot -> now - slot.lastTriggeredMs >= slot.durationMs());
        return List.copyOf(slots);
    }

    public static synchronized void clear() {
        slots.clear();
    }
}
