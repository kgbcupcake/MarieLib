package dev.marie.framework.ui.edit;

/**
 * Tracks time and screen-distance between successive clicks of the same mouse button to recognize
 * double-clicks, independently per button. A component wires {@link #onClick} into its own {@code
 * mouseClicked} override, before any existing forward to {@link DraggableResizable#mouseClicked};
 * a click that doesn't complete into a double-click within the window falls through unconsumed so
 * ordinary single-click drag-start behavior is unaffected.
 *
 * <p>Left- and right-button double-clicks are exposed as distinct signals via separate callbacks
 * rather than a single "double-click" event carrying a button field, since callers generally want
 * to react to each button differently rather than branch on it themselves.
 */
public final class DoubleClickRecognizer {

    /** Max delay between two clicks of the same button to count as a double-click. Not yet tuned against real play. */
    public static final long DEFAULT_WINDOW_MS = 300L;

    /** Max screen-pixel distance between two clicks of the same button to count as a double-click. Not yet tuned against real play. */
    public static final double DEFAULT_DISTANCE_TOLERANCE_PX = 4.0;

    private final long windowMs;
    private final double distanceTolerancePx;
    private Runnable onLeftDoubleClick = () -> { };
    private Runnable onRightDoubleClick = () -> { };

    private int lastButton = -1;
    private long lastClickTimeMs = Long.MIN_VALUE;
    private double lastClickX;
    private double lastClickY;

    public DoubleClickRecognizer() {
        this(DEFAULT_WINDOW_MS, DEFAULT_DISTANCE_TOLERANCE_PX);
    }

    public DoubleClickRecognizer(long windowMs, double distanceTolerancePx) {
        this.windowMs = windowMs;
        this.distanceTolerancePx = distanceTolerancePx;
    }

    /** Callback invoked when two left clicks (button 0) land within the time/distance window. */
    public void onLeftDoubleClick(Runnable callback) {
        this.onLeftDoubleClick = callback;
    }

    /** Callback invoked when two right clicks (button 1) land within the time/distance window. */
    public void onRightDoubleClick(Runnable callback) {
        this.onRightDoubleClick = callback;
    }

    /**
     * Call from the target's {@code mouseClicked}, before forwarding to {@link
     * DraggableResizable#mouseClicked}. Returns {@code true} if this click completed a
     * double-click (and fired the matching callback) — callers should treat that as consumed and
     * skip the normal forward for this event. Returns {@code false} for the first click of a pair,
     * a stale click outside the window, or a button with no registered callback; in every {@code
     * false} case the caller should proceed with its existing forward exactly as before.
     */
    public boolean onClick(int button, double mouseX, double mouseY) {
        long now = System.currentTimeMillis();
        boolean isRepeat = button == lastButton
                && now - lastClickTimeMs <= windowMs
                && distance(mouseX, mouseY, lastClickX, lastClickY) <= distanceTolerancePx;

        lastButton = button;
        lastClickTimeMs = now;
        lastClickX = mouseX;
        lastClickY = mouseY;

        if (!isRepeat) {
            return false;
        }
        // Consumed: reset so a third rapid click starts a fresh pair rather than chaining.
        lastClickTimeMs = Long.MIN_VALUE;
        if (button == 0) {
            onLeftDoubleClick.run();
            return true;
        }
        if (button == 1) {
            onRightDoubleClick.run();
            return true;
        }
        return false;
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }
}
