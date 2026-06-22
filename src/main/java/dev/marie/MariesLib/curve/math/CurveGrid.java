package dev.marie.MariesLib.curve.math;

import dev.marie.MariesLib.api.ApiStatus;

/**
 * Immutable 2D bilinear response grid. Maps two normalized inputs (x, y), each
 * expected in [0, 1], to a single output multiplier via bilinear interpolation
 * between the four nearest grid cells.
 *
 * <p>Reusable infrastructure — has no knowledge of what x, y, or the output
 * represent. The consuming mod defines that meaning.</p>
 *
 * @param xCells       number of grid cells along the x axis (>= 1)
 * @param yCells       number of grid cells along the y axis (>= 1)
 * @param multipliers  flattened [xCells+1][yCells+1] grid of corner values,
 *                     row-major (index = row * (yCells + 1) + col), where
 *                     row corresponds to x and col corresponds to y
 */
@ApiStatus.Stable
public record CurveGrid(int xCells, int yCells, float[] multipliers) {

    public CurveGrid {
        if (xCells < 1 || yCells < 1) {
            throw new IllegalArgumentException("CurveGrid: xCells and yCells must be >= 1");
        }
        int expected = (xCells + 1) * (yCells + 1);
        if (multipliers == null || multipliers.length != expected) {
            throw new IllegalArgumentException(
                    "CurveGrid: multipliers length must be " + expected + " for a " + xCells + "x" + yCells + " grid");
        }
    }

    /**
     * Returns a flat grid of uniform {@code value} at every corner — the identity/no-op curve.
     */
    public static CurveGrid flat(int xCells, int yCells, float value) {
        int size = (xCells + 1) * (yCells + 1);
        float[] grid = new float[size];
        java.util.Arrays.fill(grid, value);
        return new CurveGrid(xCells, yCells, grid);
    }

    /**
     * Evaluates the curve at (x, y), clamping inputs to [0, 1] and bilinearly
     * interpolating between the four surrounding grid corners.
     */
    public float evaluate(float x, float y) {
        float cx = clamp01(x);
        float cy = clamp01(y);

        float gx = cx * xCells;
        float gy = cy * yCells;

        int x0 = (int) Math.floor(gx);
        int y0 = (int) Math.floor(gy);
        x0 = Math.min(x0, xCells - 1);
        y0 = Math.min(y0, yCells - 1);
        int x1 = x0 + 1;
        int y1 = y0 + 1;

        float tx = gx - x0;
        float ty = gy - y0;

        float c00 = cornerAt(x0, y0);
        float c10 = cornerAt(x1, y0);
        float c01 = cornerAt(x0, y1);
        float c11 = cornerAt(x1, y1);

        float top = lerp(c00, c10, tx);
        float bottom = lerp(c01, c11, tx);
        return lerp(top, bottom, ty);
    }

    private float cornerAt(int xi, int yi) {
        int cols = yCells + 1;
        return multipliers[xi * cols + yi];
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp01(float v) {
        if (Float.isNaN(v)) return 0f;
        return Math.max(0f, Math.min(1f, v));
    }
}
