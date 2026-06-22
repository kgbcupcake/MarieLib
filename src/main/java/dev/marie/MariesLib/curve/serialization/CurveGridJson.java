package dev.marie.MariesLib.curve.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.curve.math.CurveGrid;

/**
 * JSON (de)serialization for {@link CurveGrid}. Reusable infrastructure — the
 * schema is {"xCells": int, "yCells": int, "multipliers": [float...]}.
 */
@ApiStatus.Internal
public final class CurveGridJson {

    private CurveGridJson() {}

    public static JsonObject toJson(CurveGrid grid) {
        JsonObject obj = new JsonObject();
        obj.addProperty("xCells", grid.xCells());
        obj.addProperty("yCells", grid.yCells());
        JsonArray arr = new JsonArray();
        for (float v : grid.multipliers()) {
            arr.add(v);
        }
        obj.add("multipliers", arr);
        return obj;
    }

    /**
     * Parses a CurveGrid from JSON. Returns {@code null} (does not throw) if the
     * object is malformed, so callers can fall back to a default grid — mirrors
     * the fallback-on-malformed-input pattern used elsewhere in MarieLib's
     * config loaders.
     */
    public static CurveGrid fromJson(JsonObject obj) {
        try {
            int xCells = obj.get("xCells").getAsInt();
            int yCells = obj.get("yCells").getAsInt();
            JsonArray arr = obj.getAsJsonArray("multipliers");
            float[] multipliers = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                multipliers[i] = arr.get(i).getAsFloat();
            }
            return new CurveGrid(xCells, yCells, multipliers);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
