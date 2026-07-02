package dev.marie.framework.compat;

import com.google.gson.annotations.SerializedName;
import dev.marie.framework.api.ApiStatus;

@ApiStatus.Internal
public enum ConflictLevel {
    @SerializedName("none")
    NONE,

    @SerializedName("partial_conflict")
    PARTIAL_CONFLICT,

    @SerializedName("full_conflict")
    FULL_CONFLICT
}
