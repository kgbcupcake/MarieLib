package dev.marie.framework.ui.component;

import dev.marie.framework.ui.edit.DraggableResizable;
import dev.marie.framework.ui.geometry.Bounds;

/**
 * Shared collapse-on-shrink contract for a {@link MarieComponent} whose content is a fixed header/
 * label followed by variable body content that may not fully fit once the component's live {@link
 * Bounds} has been independently resized (e.g. via {@link DraggableResizable} edge-resize) smaller
 * than its natural size. Extracted from Nourished's Diet Screen sub-boxes, which had each grown a
 * slightly different collapse behavior on their own (one hid its header along with its body, another
 * kept the header and only hid body rows) — this fixes the contract so every implementor behaves the
 * same way:
 *
 * <ul>
 *   <li>the header/label always renders, at {@link #headerLocalHeight()} — it never fully hides,
 *   even when the box has shrunk far below its natural size;</li>
 *   <li>body content collapses (stops rendering) below a size threshold, instead of overflowing
 *   past the box's edge or rendering truncated/cut-off content;</li>
 *   <li>nothing below the header renders once collapsed.</li>
 * </ul>
 *
 * <p>The two default fit-check methods both measure against {@code contentScale} — the same "screen
 * pixels per local unit" ratio the implementor's own {@code render()} already draws header/body at —
 * so the fit-check stays self-consistent with whatever axis (width or height) was independently
 * resized to produce the live {@code bounds}, rather than assuming a uniform/proportional resize.
 */
public interface HeaderCollapsibleComponent {

    /** The header/label block's fixed height, in local (pre-scale) units. Always rendered, never collapsed. */
    int headerLocalHeight();

    /**
     * How many repeating body units of {@code unitLocalHeight} each (e.g. list rows/lines) fit below
     * the header in {@code bounds} at {@code contentScale}, floor-clamped to {@code [0,
     * availableUnits]}. For a component whose body is a list of same-height repeating rows.
     */
    default int bodyUnitsFit(Bounds bounds, double contentScale, int availableUnits, int unitLocalHeight) {
        if (contentScale <= 0 || unitLocalHeight <= 0) {
            return 0;
        }
        double headerScreenH = headerLocalHeight() * contentScale;
        double unitScreenH = unitLocalHeight * contentScale;
        int fit = (int) Math.floor((bounds.height() - headerScreenH) / unitScreenH);
        return Math.max(0, Math.min(availableUnits, fit));
    }

    /**
     * Whether a single fixed-height body content block ({@code bodyLocalHeight}, local units) fits
     * below the header in {@code bounds} at {@code contentScale}. For a component whose body is one
     * all-or-nothing block rather than a repeating list (e.g. a fixed row of suggestion icons).
     */
    default boolean bodyBlockFits(Bounds bounds, double contentScale, int bodyLocalHeight) {
        if (contentScale <= 0) {
            return false;
        }
        double headerScreenH = headerLocalHeight() * contentScale;
        double bodyScreenH = bodyLocalHeight * contentScale;
        return bounds.height() >= headerScreenH + bodyScreenH;
    }
}
