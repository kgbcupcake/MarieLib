package dev.marie.framework.notification;

/** Mutable defaults for the notification stack's position, scale, and duration. */
public final class NotificationConfig {

    private static final NotificationConfig INSTANCE = new NotificationConfig();

    public int verticalGap = 4;
    public int slotGap = 2;
    public float baseScale = 1.0f;
    public float emphasizedScale = 1.3f;
    public int defaultDurationTicks = 60;
    public int fadeInTicks = 3;
    public int fadeOutTicks = 6;

    private NotificationConfig() {}

    public static NotificationConfig get() {
        return INSTANCE;
    }
}
