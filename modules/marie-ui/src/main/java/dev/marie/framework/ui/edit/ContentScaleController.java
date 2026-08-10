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
 * <p>This does not replace or decouple content size from box size. Every component already
 * computes its own proportional scale as {@code Math.min(widthScale, heightScale)} against its
 * resolved {@link Bounds} — that box-driven number is still the foundation. The value this
 * controller persists and lets the user scroll-adjust is a <em>multiplier</em> on top of that
 * scale, applied via {@link #resolveContentScale}:
 *
 * <pre>
 * box resize -&gt; existing proportional scale -&gt; user adjustment -&gt; final content scale
 * </pre>
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

    /** Storage range for the persisted multipliers — wider than the effective on-screen range enforced by {@link #resolveContentScale}/{@link #resolvePadding}. */
    private static final double SCALE_STORAGE_MIN = 0.1d;
    private static final double SCALE_STORAGE_MAX = 5.0d;

    /**
     * Live per-render clamp on {@link #resolveContentScale}'s result, as a multiplier of that
     * call's own {@code proportionalScale} — not derived from width/height scale at all, so every
     * component gets a real, resize-independent adjustment range regardless of its aspect ratio.
     * Shrinking below the proportional scale is always safe (never clips content); growing above it
     * can, so both bounds exist to keep the box-driven scale as the dominant factor.
     */
    private static final double TEXT_SCALE_FLOOR_MULTIPLIER = 0.5d;
    private static final double TEXT_SCALE_CEILING_MULTIPLIER = 3.0d;

    /** Analogous live clamp for {@link #resolvePadding}, as a multiplier of that call's own {@code basePadding}. */
    private static final double PADDING_FLOOR_MULTIPLIER = 0.25d;
    private static final double PADDING_CEILING_MULTIPLIER = 3.0d;

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

    /**
     * Combines a component's existing box-driven {@code proportionalScale} (its own {@code
     * Math.min(widthScale, heightScale)}) with its persisted user adjustment for text/content draw
     * calls only — never for the component's own box sizing, which must stay driven by {@code
     * proportionalScale} alone so this adjustment can't grow the box itself. Clamped every call to
     * {@code [proportionalScale * TEXT_SCALE_FLOOR_MULTIPLIER, proportionalScale *
     * TEXT_SCALE_CEILING_MULTIPLIER]} — a fixed range around the component's own current
     * proportional scale, not an independent absolute scale.
     */
    public static float resolveContentScale(double proportionalScale, double userScaleAdjustment) {
        double min = proportionalScale * TEXT_SCALE_FLOOR_MULTIPLIER;
        double max = proportionalScale * TEXT_SCALE_CEILING_MULTIPLIER;
        double adjusted = proportionalScale * userScaleAdjustment;
        return (float) Math.min(max, Math.max(min, adjusted));
    }

    /**
     * Analogous to {@link #resolveContentScale}, for a component's padding: combines a base padding
     * value (already scaled to the component's own box, in whatever unit the caller renders with)
     * with the persisted paddingScale multiplier, clamped to {@code [basePadding *
     * PADDING_FLOOR_MULTIPLIER, basePadding * PADDING_CEILING_MULTIPLIER]}.
     */
    public static float resolvePadding(double basePadding, double paddingScaleAdjustment) {
        double min = basePadding * PADDING_FLOOR_MULTIPLIER;
        double max = basePadding * PADDING_CEILING_MULTIPLIER;
        double adjusted = basePadding * paddingScaleAdjustment;
        return (float) Math.min(max, Math.max(min, adjusted));
    }
}
