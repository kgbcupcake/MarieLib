package dev.marie.framework.ui;

/** Persisted position/size/collapsed-state for one component, keyed externally by component id. */
public record ComponentState(int x, int y, int width, int height, boolean collapsed) {}
