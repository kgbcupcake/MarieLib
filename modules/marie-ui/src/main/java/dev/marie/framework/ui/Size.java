package dev.marie.framework.ui;

/** A width/height pair used by {@link Constraint} to describe sizing intent, not a resolved layout result. */
public record Size(int width, int height) {

    public static final Size ZERO = new Size(0, 0);
    public static final Size UNBOUNDED = new Size(Integer.MAX_VALUE, Integer.MAX_VALUE);
}
