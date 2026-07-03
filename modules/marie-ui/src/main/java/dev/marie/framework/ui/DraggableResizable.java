package dev.marie.framework.ui;

import java.util.function.BiConsumer;

/**
 * Generic drag/resize gesture tracker for a single {@link MarieComponent} target. Extracted from
 * Nourished's DietScreenEditController, which hand-rolled this same grab-offset drag and
 * origin+diagonal-ratio resize math three times (main panel, recent-meals sub-box, eat-more
 * sub-box) as static fields on one class.
 *
 * <p>This is a composed helper rather than a mixin base class: {@link MarieComponent} is an
 * interface, so any component that wants gesture support holds a {@code DraggableResizable}
 * field (one per independently-draggable/resizable region, if it has more than one) instead of
 * being forced into an inheritance hierarchy just to get drag/resize behavior. Composition also
 * lets a single component own multiple gesture trackers, which a mixin-via-inheritance shape
 * could not express.
 *
 * <p>This class never persists anything itself. {@link #mouseReleased(int, int)} hands the final
 * committed {@link Bounds} to the {@code onCommit} callback supplied at construction — the caller
 * decides whether/how to run that through a {@link PersistenceProvider}.
 */
public final class DraggableResizable {

    public static final int RESIZE_HANDLE_SIZE = 8;

    private final MarieComponent target;
    private final Constraint constraint;
    private final BiConsumer<MarieComponent, Bounds> onCommit;

    private boolean dragging;
    private boolean resizing;

    private int grabOffsetX;
    private int grabOffsetY;

    private int resizeOriginX;
    private int resizeOriginY;
    private int resizeStartWidth;
    private int resizeStartHeight;
    private double resizeBaseDiagonal;

    private Bounds previewBounds;

    public DraggableResizable(MarieComponent target, Constraint constraint, BiConsumer<MarieComponent, Bounds> onCommit) {
        this.target = target;
        this.constraint = constraint;
        this.onCommit = onCommit;
    }

    public boolean isDragging() {
        return dragging;
    }

    public boolean isResizing() {
        return resizing;
    }

    /** Whether the resize handle should render in its "active" (currently being dragged) state. */
    public boolean isHandleActive() {
        return resizing;
    }

    /** Whether (mx, my) is within the resize-handle hit region of {@code bounds}, for hover rendering. */
    public boolean isHandleHovered(int mx, int my, Bounds bounds) {
        return !resizing && isOverResizeHandle(mx, my, bounds);
    }

    /** The 8x8 handle rectangle at the bottom-right corner of {@code bounds}, for render() to draw against. */
    public static Bounds handleBounds(Bounds bounds) {
        return new Bounds(
                bounds.x() + bounds.width() - RESIZE_HANDLE_SIZE,
                bounds.y() + bounds.height() - RESIZE_HANDLE_SIZE,
                RESIZE_HANDLE_SIZE,
                RESIZE_HANDLE_SIZE
        );
    }

    public static boolean isOverResizeHandle(int mx, int my, Bounds bounds) {
        Bounds handle = handleBounds(bounds);
        return mx >= handle.x() && my >= handle.y()
                && mx < handle.x() + handle.width() && my < handle.y() + handle.height();
    }

    /**
     * Call from the target's mouseClicked, passing its current committed {@code bounds}. Starts a
     * resize gesture if (mx, my) hits the handle, else a drag gesture if it's inside the bounds.
     * Returns true if a gesture started.
     */
    public boolean mouseClicked(int mx, int my, Bounds bounds) {
        if (isOverResizeHandle(mx, my, bounds)) {
            resizing = true;
            dragging = false;
            resizeOriginX = bounds.x();
            resizeOriginY = bounds.y();
            resizeStartWidth = bounds.width();
            resizeStartHeight = bounds.height();
            resizeBaseDiagonal = Math.max(1.0d, Math.hypot(bounds.width(), bounds.height()));
            previewBounds = bounds;
            return true;
        }
        if (bounds.contains(mx, my)) {
            dragging = true;
            resizing = false;
            grabOffsetX = mx - bounds.x();
            grabOffsetY = my - bounds.y();
            previewBounds = bounds;
            return true;
        }
        return false;
    }

    /**
     * Call from the target's mouseDragged with absolute mouse coordinates. Returns the live
     * preview {@link Bounds} for the active gesture, or {@code null} if no gesture is active.
     */
    public Bounds mouseDragged(int mx, int my) {
        if (dragging) {
            previewBounds = new Bounds(mx - grabOffsetX, my - grabOffsetY, previewBounds.width(), previewBounds.height());
            return previewBounds;
        }
        if (resizing) {
            double dist = Math.hypot(mx - resizeOriginX, my - resizeOriginY);
            double scale = dist / resizeBaseDiagonal;
            int newWidth = clampWidth((int) Math.round(resizeStartWidth * scale));
            int newHeight = clampHeight((int) Math.round(resizeStartHeight * scale));
            previewBounds = new Bounds(resizeOriginX, resizeOriginY, newWidth, newHeight);
            return previewBounds;
        }
        return null;
    }

    /**
     * Call from the target's mouseReleased. Ends any active gesture and hands the final committed
     * {@link Bounds} to the {@code onCommit} callback supplied at construction.
     */
    public void mouseReleased(int mx, int my) {
        if (!dragging && !resizing) {
            return;
        }
        dragging = false;
        resizing = false;
        if (previewBounds != null) {
            onCommit.accept(target, previewBounds);
        }
        previewBounds = null;
    }

    private int clampWidth(int width) {
        Size min = constraint.minSize();
        Size max = constraint.maxSize();
        return Math.max(min.width(), Math.min(max.width(), width));
    }

    private int clampHeight(int height) {
        Size min = constraint.minSize();
        Size max = constraint.maxSize();
        return Math.max(min.height(), Math.min(max.height(), height));
    }
}
