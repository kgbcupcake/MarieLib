package dev.marie.MariesLib.compat;

import com.google.gson.annotations.SerializedName;
import dev.marie.MariesLib.api.ApiStatus;

@ApiStatus.Internal
public enum CompatCategory {
    @SerializedName("survival_overhaul")
    SURVIVAL_OVERHAUL,

    @SerializedName("source_mod")
    SOURCE_MOD,

    @SerializedName("farming_mod")
    FARMING_MOD,

    @SerializedName("magic_mod")
    MAGIC_MOD,

    @SerializedName("unknown")
    UNKNOWN
}
