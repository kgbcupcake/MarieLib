package dev.marie.framework.notification;

import net.neoforged.neoforge.common.NeoForge;

/** Public facade for the generic client-side notification stack anchored above the XP bar. */
public final class MarieNotifications {

    private static volatile boolean listenerRegistered;

    private MarieNotifications() {}

    /** Triggers a notification: merges into an in-window matching slot, or pushes a new one. */
    public static void show(NotificationRequest request) {
        NotificationManager.show(request);
    }

    /** Clears the active notification stack, e.g. on disconnect. */
    public static void clear() {
        NotificationManager.clear();
    }

    /** Wires the render listener into the client event bus; safe to call more than once. */
    public static void registerClientListeners() {
        if (listenerRegistered) {
            return;
        }
        synchronized (MarieNotifications.class) {
            if (listenerRegistered) {
                return;
            }
            NeoForge.EVENT_BUS.addListener(NotificationRenderer::onRenderGuiPost);
            listenerRegistered = true;
        }
    }
}
