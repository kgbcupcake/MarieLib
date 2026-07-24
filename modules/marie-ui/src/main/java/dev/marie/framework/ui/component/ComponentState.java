package dev.marie.framework.ui.component;

/**
 * Persisted position, size, and state for a component, stored externally by component id.
 *
 * <p>Manual size flags indicate whether the user has explicitly overridden an auto-computed axis.
 * {@code leftMargin} is an accumulated screen-pixel offset for components that reserve dead space
 * before their content on one side, grown only by dragging that side's edge — never touched by
 * resizing the opposite edge, so it can't jump when a different edge starts a gesture.
 *
 * <p>{@code contentScale} lets a component's content be sized independently of the component's own
 * box bounds (above) — e.g. a zoomable inner element. Defaults to {@code 1.0} (no scaling) for any
 * component that doesn't use this.
 */
public record ComponentState(
        int x,
        int y,
        int width,
        int height,
        boolean collapsed,
        boolean widthManual,
        boolean heightManual,
        int leftMargin,
        double contentScale
) {

    /** Default content transform for components that don't use it: scale 1.0. */
    public static final double DEFAULT_CONTENT_SCALE = 1.0;

    public ComponentState(int x, int y, int width, int height, boolean collapsed,
                           boolean widthManual, boolean heightManual, int leftMargin) {
        this(x, y, width, height, collapsed, widthManual, heightManual, leftMargin,
                DEFAULT_CONTENT_SCALE);
    }
}
