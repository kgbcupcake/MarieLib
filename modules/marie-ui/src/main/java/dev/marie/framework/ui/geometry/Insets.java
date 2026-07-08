package dev.marie.framework.ui;

/** Directional spacing shared by {@link Constraint#padding()} and {@link Constraint#margin()}. */
public record Insets(int top, int right, int bottom, int left) {

    public static final Insets NONE = new Insets(0, 0, 0, 0);

    public static Insets of(int all) {
        return new Insets(all, all, all, all);
    }

    public int horizontal() {
        return left + right;
    }

    public int vertical() {
        return top + bottom;
    }
}
