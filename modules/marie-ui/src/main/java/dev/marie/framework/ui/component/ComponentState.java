package dev.marie.framework.ui.component;

/**
 * Persisted position, size, and state for a component, stored externally by component id.
 *
 * <p>Manual size flags indicate whether the user has explicitly overridden an auto-computed axis.
 * {@code leftMargin} is an accumulated screen-pixel offset for components that reserve dead space
 * before their content on one side, grown only by dragging that side's edge — never touched by
 * resizing the opposite edge, so it can't jump when a different edge starts a gesture.
 */
public record ComponentState(
        int x,
        int y,
        int width,
        int height,
        boolean collapsed,
        boolean widthManual,
        boolean heightManual,
        int leftMargin
) {}
