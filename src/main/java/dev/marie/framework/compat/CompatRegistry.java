package dev.marie.MariesLib.compat;

import com.google.gson.annotations.SerializedName;
import dev.marie.MariesLib.api.ApiStatus;

import java.util.List;

/**
 * Root JSON structure for compat_registry.json files.
 */
@ApiStatus.Internal
public record CompatRegistry(
        @SerializedName("entries")
        List<JsonCompatEntry> entries
) {}
