package dev.marie.framework.ui.edit;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.ui.PersistenceProvider;
import dev.marie.framework.ui.component.ComponentState;
import dev.marie.framework.ui.geometry.Bounds;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic double-click-to-adjust controller for a component's existing box-driven proportional
 * content scale and padding, keyed by component id. Generalizes the per-box zoom pattern Nourished
 * hand-rolled for its Diet Screen sub-boxes (double-click to enter/exit a scroll-adjustable mode,
 * one box at a time) into a reusable marie-ui primitive that also covers padding.
 *
 * <p>Content size is driven by the user's persisted adjustment alone, full stop — never by box
 * size. Every component still computes its own proportional scale as {@code Math.min(widthScale,
 * heightScale)} against its resolved {@link Bounds}, and that box-driven number is still the
 * foundation for the component's own box sizing and coordinate mapping, but it plays no part in
 * content/text scale: {@link #resolveContentScale} just passes the user's chosen scale through
 * (sanity-clamped). If the box is too small to fit that scale, the content overflows the box's
 * natural extent and gets cut off by the component's own clip region instead of shrinking.
 *
 * <p>Double-left-clicking inside a component toggles {@link Mode#TEXT_SCALE} for it; while active,
 * scrolling adjusts that component's persisted {@link ComponentState#contentScale}. Double-right-
 * clicking toggles {@link Mode#PADDING}; while active, scrolling adjusts {@link
 * ComponentState#paddingScale}. Only one component/mode pair is active at a time — entering one
 * implicitly exits whichever other was active, same contract as Nourished's DietZoomController.
 *
 * <p>Like {@link DraggableResizable}, this class never persists gesture-tracking state itself
 * beyond what's needed to recognize the click/scroll pairs — the actual adjustment is read-modify-
 * written straight to the {@link PersistenceProvider} supplied at construction, since a scroll
 * gesture is many small increments rather than a single end-of-gesture commit.
 */
@ApiStatus.Experimental
public final class ContentScaleController {

    /** Which aspect of a component's content the active scroll gesture is adjusting. */
    public enum Mode {
        NONE, TEXT_SCALE, PADDING
    }

    /** contentScale/paddingScale change applied per scroll notch. */
    private static final double SCROLL_STEP = 0.05d;

    /** Valid range for the persisted contentScale/paddingScale multipliers themselves — the correct bound for anything editing those fields directly (e.g. a slider), as opposed to {@link #resolvePadding}'s sanity clamp, which is in different (already-resolved, caller-specific pixel) units. */
    public static final double SCALE_STORAGE_MIN = 0.1d;
    public static final double SCALE_STORAGE_MAX = 5.0d;

    private final PersistenceProvider persistence;
    private final Map<String, DoubleClickRecognizer> recognizers = new HashMap<>();

    /**
     * Per-component hit-test bounds/time snapshot from that component's own most recent click,
     * reused for the *next* click's containment check — same reasoning as DietZoomController's own
     * {@code pendingClickBounds}: a component whose bounds shift between the two clicks of a
     * double-click (e.g. chained after a sibling whose height depends on live content) would
     * otherwise silently fail to complete the pair against already-stale live bounds.
     */
    private final Map<String, Bounds> pendingClickBounds = new HashMap<>();
    private final Map<String, Long> pendingClickTimeMs = new HashMap<>();

    private String activeComponentId;
    private Mode activeMode = Mode.NONE;

    public ContentScaleController(PersistenceProvider persistence) {
        this.persistence = persistence;
    }

    /**
     * Feeds a click at ({@code mouseX}, {@code mouseY}) to {@code componentId}'s recognizer,
     * hit-tested against {@code liveBounds} for the first click of a potential pair or that same
     * click's snapshotted bounds for a second click following within the window — see {@link
     * #pendingClickBounds}. Returns true if this click completed a double-click (toggling text-scale
     * or padding mode) — callers should treat that as consumed and skip the component's normal
     * click handling for this event, same contract as {@link DoubleClickRecognizer#onClick}.
     */
    public boolean onClick(String componentId, int button, double mouseX, double mouseY, Bounds liveBounds) {
        long now = System.currentTimeMillis();
        Long pendingTime = pendingClickTimeMs.get(componentId);
        boolean pendingFresh = pendingTime != null && now - pendingTime <= DoubleClickRecognizer.DEFAULT_WINDOW_MS;
        Bounds testBounds = pendingFresh ? pendingClickBounds.get(componentId) : liveBounds;
        if (!testBounds.contains((int) mouseX, (int) mouseY)) {
            return false;
        }
        boolean completed = recognizer(componentId).onClick(button, mouseX, mouseY);
        if (completed) {
            pendingClickBounds.remove(componentId);
            pendingClickTimeMs.remove(componentId);
        } else {
            pendingClickBounds.put(componentId, liveBounds);
            pendingClickTimeMs.put(componentId, now);
        }
        return completed;
    }

    private DoubleClickRecognizer recognizer(String componentId) {
        return recognizers.computeIfAbsent(componentId, id -> {
            DoubleClickRecognizer recognizer = new DoubleClickRecognizer();
            recognizer.onLeftDoubleClick(() -> toggle(id, Mode.TEXT_SCALE));
            recognizer.onRightDoubleClick(() -> toggle(id, Mode.PADDING));
            return recognizer;
        });
    }

    /** Toggles {@code mode} off if it's already active for {@code componentId}, else activates it — implicitly exiting whatever other component/mode was active. */
    private void toggle(String componentId, Mode mode) {
        if (componentId.equals(activeComponentId) && activeMode == mode) {
            activeComponentId = null;
            activeMode = Mode.NONE;
        } else {
            activeComponentId = componentId;
            activeMode = mode;
        }
    }

    /** {@code componentId}'s current adjustment mode — {@link Mode#NONE} unless it's the active component. */
    public Mode activeMode(String componentId) {
        return componentId.equals(activeComponentId) ? activeMode : Mode.NONE;
    }

    /**
     * Adjusts {@code componentId}'s persisted contentScale (in {@link Mode#TEXT_SCALE}) or
     * paddingScale (in {@link Mode#PADDING}) by one scroll notch, clamped to {@code
     * [SCALE_STORAGE_MIN, SCALE_STORAGE_MAX]}. Returns true if {@code componentId} is the active
     * component and consumed the scroll, false if it isn't active, no mode is active, or {@code
     * scrollY} is zero.
     */
    public boolean handleScroll(String componentId, double scrollY) {
        if (!componentId.equals(activeComponentId) || activeMode == Mode.NONE || scrollY == 0) {
            return false;
        }
        double delta = scrollY > 0 ? SCROLL_STEP : -SCROLL_STEP;
        ComponentState base = persistence.load(componentId).orElse(
                new ComponentState(0, 0, 0, 0, false, false, false, 0));
        ComponentState adjusted = switch (activeMode) {
            case TEXT_SCALE -> withContentScale(base, clampStorage(base.contentScale() + delta));
            case PADDING -> withPaddingScale(base, clampStorage(base.paddingScale() + delta));
            case NONE -> base;
        };
        persistence.save(componentId, adjusted);
        return true;
    }

    private static double clampStorage(double value) {
        return Math.min(SCALE_STORAGE_MAX, Math.max(SCALE_STORAGE_MIN, value));
    }

    private static ComponentState withContentScale(ComponentState base, double contentScale) {
        return new ComponentState(base.x(), base.y(), base.width(), base.height(), base.collapsed(),
                base.widthManual(), base.heightManual(), base.leftMargin(), contentScale, base.paddingScale());
    }

    private static ComponentState withPaddingScale(ComponentState base, double paddingScale) {
        return new ComponentState(base.x(), base.y(), base.width(), base.height(), base.collapsed(),
                base.widthManual(), base.heightManual(), base.leftMargin(), base.contentScale(), paddingScale);
    }

    /** {@code componentId}'s persisted contentScale multiplier, or {@link ComponentState#DEFAULT_CONTENT_SCALE} if never set. */
    public double contentScaleAdjustment(String componentId) {
        return persistence.load(componentId).map(ComponentState::contentScale).orElse(ComponentState.DEFAULT_CONTENT_SCALE);
    }

    /** {@code componentId}'s persisted paddingScale multiplier, or {@link ComponentState#DEFAULT_PADDING_SCALE} if never set. */
    public double paddingScaleAdjustment(String componentId) {
        return persistence.load(componentId).map(ComponentState::paddingScale).orElse(ComponentState.DEFAULT_PADDING_SCALE);
    }

    /** Sanity clamp for {@link #resolveContentScale} — a multiplier, so degenerate values are caught close to zero/five. */
    private static final double CONTENT_SCALE_SANITY_MIN = 0.1d;
    private static final double CONTENT_SCALE_SANITY_MAX = 5.0d;

    /** Sanity clamp for {@link #resolvePadding} — already-resolved local-pixel units (caller's reference padding times its multiplier), so the range is wide rather than a multiplier-shaped [0.1, 5.0]. */
    private static final double PADDING_SANITY_MIN = 0.0d;
    private static final double PADDING_SANITY_MAX = 1000.0d;

    /**
     * Pass-through for the user's persisted content-scale adjustment: box size plays no part in this
     * — {@code userScaleAdjustment} is returned as-is, only sanity-clamped to {@code
     * [CONTENT_SCALE_SANITY_MIN, CONTENT_SCALE_SANITY_MAX]} to stop degenerate (e.g. corrupted-save)
     * values, not to constrain it to whatever the box can currently fit. If the box is too small for
     * the resulting content, the caller's own clip region is what cuts it off, not this method.
     */
    public static float resolveContentScale(double userScaleAdjustment) {
        return (float) clamp(userScaleAdjustment, CONTENT_SCALE_SANITY_MIN, CONTENT_SCALE_SANITY_MAX);
    }

    /**
     * Analogous to {@link #resolveContentScale}, for a component's padding: {@code userPaddingAmount}
     * (the persisted paddingScale multiplier already applied to the component's reference padding) is
     * returned as-is, only sanity-clamped to {@code [PADDING_SANITY_MIN, PADDING_SANITY_MAX]}.
     */
    public static float resolvePadding(double userPaddingAmount) {
        return (float) clamp(userPaddingAmount, PADDING_SANITY_MIN, PADDING_SANITY_MAX);
    }

    private static double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }
}
