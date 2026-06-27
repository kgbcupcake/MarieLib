package dev.marie.MariesLib.api;

import java.util.Map;

/**
 * Resolves a single item of type {@code T} into a structured data map for export.
 * Register via {@link MarieAPI#registerExportResolver(ExportResolver)}.
 *
 * @param <T> the item type this resolver handles
 */
@ApiStatus.Stable
public interface ExportResolver<T> {

    /** Stable identifier for this resolver; used as the key in the dump command. */
    String resolverId();

    /**
     * Extracts export data from {@code item}.
     * Returns an empty map if the item is not applicable.
     */
    Map<String, Object> resolve(T item);
}
