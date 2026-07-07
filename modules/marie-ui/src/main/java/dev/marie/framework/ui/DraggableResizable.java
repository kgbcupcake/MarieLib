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
    public static final int EDGE_HANDLE_THICKNESS = 4;

    /**
     * Which handle (if any) started the active resize gesture. {@code CORNER} reproduces the
     * original locked-aspect-ratio diagonal-scale math; the four edge modes each move only their
     * own axis, anchored at the opposite fixed edge.
     */
    private enum ResizeMode {
        CORNER, LEFT, RIGHT, TOP, BOTTOM
    }

    private final MarieComponent target;
    private Constraint constraint;
    private final BiConsumer<MarieComponent, Bounds> onCommit;

    private boolean dragging;
    private boolean resizing;
    private ResizeMode resizeMode;

    private int grabOffsetX;
    private int grabOffsetY;

    private int resizeOriginX;
    private int resizeOriginY;
    private int resizeStartWidth;
    private int resizeStartHeight;
    private double resizeBaseDiagonal;
    private int resizeFixedRight;
    private int resizeFixedBottom;

    private Bounds previewBounds;

    public DraggableResizable(MarieComponent target, Constraint constraint, BiConsumer<MarieComponent, Bounds> onCommit) {
        this.target = target;
        this.constraint = constraint;
        this.onCommit = onCommit;
    }

    /**
     * Replaces the min/max/preferred bounds used by {@link #clampWidth}/{@link #clampHeight} for
     * every subsequent call, including mid-gesture. Callers whose reference size depends on a scale
     * that can change across a single edit-mode session (e.g. the diet panel's scale, if the panel
     * itself gets resized) should call this every frame with a freshly-computed {@link Constraint} —
     * otherwise the clamp stays frozen at whatever scale was active when this tracker was
     * constructed, and a resize committed at a very different live scale can convert to a wildly
     * wrong persisted size.
     */
    public void setConstraint(Constraint constraint) {
        this.constraint = constraint;
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

    /** Whether (mx, my) is within any edge-strip hit region of {@code bounds}, for hover rendering. */
    public boolean isEdgeHandleHovered(int mx, int my, Bounds bounds) {
        return !resizing && findEdgeMode(mx, my, bounds) != null;
    }

    /** Whether (mx, my) is within {@code edge}'s own hit region of {@code bounds}, for per-edge hover rendering. */
    public boolean isEdgeHovered(int mx, int my, Bounds bounds, Edge edge) {
        return !resizing && findEdgeHandle(mx, my, bounds) == edge;
    }

    /** Whether {@code edge} is the one currently being dragged, for per-edge active-state rendering. */
    public boolean isEdgeActive(Edge edge) {
        if (!resizing || resizeMode == null) {
            return false;
        }
        return switch (resizeMode) {
            case LEFT -> edge == Edge.LEFT;
            case RIGHT -> edge == Edge.RIGHT;
            case TOP -> edge == Edge.TOP;
            case BOTTOM -> edge == Edge.BOTTOM;
            case CORNER -> false;
        };
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
     * The thin hit-region strip along one edge of {@code bounds}, for render() to draw against.
     * Each strip stops short of the bottom-right corner so it never overlaps {@link #handleBounds}.
     */
    public static Bounds edgeHandleBounds(Bounds bounds, Edge edge) {
        return switch (edge) {
            case LEFT -> new Bounds(bounds.x(), bounds.y(), EDGE_HANDLE_THICKNESS, bounds.height());
            case TOP -> new Bounds(bounds.x(), bounds.y(), bounds.width(), EDGE_HANDLE_THICKNESS);
            case RIGHT -> new Bounds(
                    bounds.x() + bounds.width() - EDGE_HANDLE_THICKNESS, bounds.y(),
                    EDGE_HANDLE_THICKNESS, bounds.height() - RESIZE_HANDLE_SIZE
            );
            case BOTTOM -> new Bounds(
                    bounds.x(), bounds.y() + bounds.height() - EDGE_HANDLE_THICKNESS,
                    bounds.width() - RESIZE_HANDLE_SIZE, EDGE_HANDLE_THICKNESS
            );
        };
    }

    /** Which edge (if any) of {@code bounds} contains (mx, my). Corner handle takes priority; callers should check {@link #isOverResizeHandle} first. */
    public static Edge findEdgeHandle(int mx, int my, Bounds bounds) {
        for (Edge edge : Edge.values()) {
            Bounds strip = edgeHandleBounds(bounds, edge);
            if (mx >= strip.x() && my >= strip.y()
                    && mx < strip.x() + strip.width() && my < strip.y() + strip.height()) {
                return edge;
            }
        }
        return null;
    }

    /** One of the four independently-draggable edges of a resizable region's bounds. */
    public enum Edge {
        LEFT, RIGHT, TOP, BOTTOM
    }

    private ResizeMode findEdgeMode(int mx, int my, Bounds bounds) {
        Edge edge = findEdgeHandle(mx, my, bounds);
        if (edge == null) {
            return null;
        }
        return switch (edge) {
            case LEFT -> ResizeMode.LEFT;
            case RIGHT -> ResizeMode.RIGHT;
            case TOP -> ResizeMode.TOP;
            case BOTTOM -> ResizeMode.BOTTOM;
        };
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
            resizeMode = ResizeMode.CORNER;
            resizeOriginX = bounds.x();
            resizeOriginY = bounds.y();
            resizeStartWidth = bounds.width();
            resizeStartHeight = bounds.height();
            resizeBaseDiagonal = Math.max(1.0d, Math.hypot(bounds.width(), bounds.height()));
            previewBounds = bounds;
            return true;
        }
        ResizeMode edgeMode = findEdgeMode(mx, my, bounds);
        if (edgeMode != null) {
            resizing = true;
            dragging = false;
            resizeMode = edgeMode;
            resizeOriginX = bounds.x();
            resizeOriginY = bounds.y();
            resizeStartWidth = bounds.width();
            resizeStartHeight = bounds.height();
            resizeFixedRight = bounds.x() + bounds.width();
            resizeFixedBottom = bounds.y() + bounds.height();
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
            previewBounds = switch (resizeMode) {
                case CORNER -> {
                    double dist = Math.hypot(mx - resizeOriginX, my - resizeOriginY);
                    double scale = dist / resizeBaseDiagonal;
                    int newWidth = clampWidth((int) Math.round(resizeStartWidth * scale));
                    int newHeight = clampHeight((int) Math.round(resizeStartHeight * scale));
                    yield new Bounds(resizeOriginX, resizeOriginY, newWidth, newHeight);
                }
                case RIGHT -> {
                    int newWidth = clampWidth(mx - resizeOriginX);
                    yield new Bounds(resizeOriginX, resizeOriginY, newWidth, resizeStartHeight);
                }
                case LEFT -> {
                    int newWidth = clampWidth(resizeFixedRight - mx);
                    yield new Bounds(resizeFixedRight - newWidth, resizeOriginY, newWidth, resizeStartHeight);
                }
                case BOTTOM -> {
                    int newHeight = clampHeight(my - resizeOriginY);
                    yield new Bounds(resizeOriginX, resizeOriginY, resizeStartWidth, newHeight);
                }
                case TOP -> {
                    int newHeight = clampHeight(resizeFixedBottom - my);
                    yield new Bounds(resizeOriginX, resizeFixedBottom - newHeight, resizeStartWidth, newHeight);
                }
            };
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
