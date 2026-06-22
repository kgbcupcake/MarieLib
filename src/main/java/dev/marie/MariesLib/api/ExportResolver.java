package dev.marie.MariesLib.api;

import java.util.Map;

/**
 * A consuming-mod-provided resolver that exports per-entry data for an entire registry
 * to an editable config file. The consuming mod decides what the exported data means —
 * nutrient values, EMC value, block hardness, anything keyed by registry entry.
 *
 * <p>Register via {@link MarieAPI#registerExportResolver}.</p>
 *
 * @param <T> the registry entry type (e.g. {@link net.minecraft.world.item.Item})
 */
@ApiStatus.Stable
public interface ExportResolver<T> {

    /**
     * A unique identifier for this resolver, used as the output filename prefix
     * (e.g. {@code "nourished_nutrients"} writes to {@code nourished_nutrients_export.json}).
     */
    String resolverId();

    /**
     * Resolves exportable data for a single registry entry.
     *
     * @param entry the registry entry being exported
     * @return a map of field name to value (String, Number, or Boolean values only —
     *         nested maps are not supported in this version), or an empty map to skip
     *         this entry in the export
     */
    Map<String, Object> resolve(T entry);
}
