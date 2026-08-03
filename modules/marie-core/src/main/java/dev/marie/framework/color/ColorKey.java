package dev.marie.framework.color;

import dev.marie.framework.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Identity for a registered color definition. Wraps a {@link ResourceLocation} so color
 * definitions can be registered and resolved consistently across the color system.
 */
@ApiStatus.Stable
public record ColorKey(ResourceLocation id) {

    public ColorKey {
        Objects.requireNonNull(id, "id");
    }

    /**
     * Creates a {@link ColorKey} wrapping the given id.
     *
     * @param id the resource location identifying the color
     * @return a new {@link ColorKey}
     */
    @ApiStatus.Stable
    public static ColorKey of(ResourceLocation id) {
        return new ColorKey(id);
    }
}
