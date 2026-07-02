package dev.marie.framework.curve.math;

import dev.marie.framework.api.ApiStatus;

/**
 * A 2D grid of float multipliers, evaluated via bilinear interpolation.
 * Axes are (intensity, confidence), each normalised to [0, 1].
 * The backing array has {@code (xCells+1) * (yCells+1)} elements stored as
 * {@code grid[xi * (yCells+1) + yi]}.
 */
@ApiStatus.Stable
public final class CurveGrid {

    private final int xCells;
    private final int yCells;
    private final float[] grid;

    public CurveGrid(int xCells, int yCells, float[] grid) {
        this.xCells = xCells;
        this.yCells = yCells;
        this.grid = grid;
    }

    /** Creates a grid filled uniformly with {@code value}. */
    public static CurveGrid flat(int xCells, int yCells, float value) {
        float[] grid = new float[(xCells + 1) * (yCells + 1)];
        java.util.Arrays.fill(grid, value);
        return new CurveGrid(xCells, yCells, grid);
    }

    /**
     * Bilinear interpolation of the grid at normalised coordinates (x, y).
     * Both x and y are clamped to [0, 1].
     */
    public float evaluate(float x, float y) {
        x = Math.max(0f, Math.min(1f, x));
        y = Math.max(0f, Math.min(1f, y));

        float fx = x * xCells;
        float fy = y * yCells;
        int x0 = Math.min((int) fx, xCells - 1);
        int y0 = Math.min((int) fy, yCells - 1);
        int x1 = x0 + 1;
        int y1 = y0 + 1;

        float tx = fx - x0;
        float ty = fy - y0;

        float v00 = grid[x0 * (yCells + 1) + y0];
        float v10 = grid[x1 * (yCells + 1) + y0];
        float v01 = grid[x0 * (yCells + 1) + y1];
        float v11 = grid[x1 * (yCells + 1) + y1];

        return (v00 * (1 - tx) + v10 * tx) * (1 - ty)
             + (v01 * (1 - tx) + v11 * tx) * ty;
    }

    public int xCells() { return xCells; }
    public int yCells() { return yCells; }
    public float[] gridData() { return grid; }
}
