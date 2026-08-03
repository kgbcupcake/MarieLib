package dev.marie.framework.color;

import dev.marie.framework.api.ApiStatus;

import java.util.Objects;

/**
 * Defines a registered color: its {@link ColorKey} identity and default ARGB value.
 *
 * <p>Register via {@link MarieColors#registerColor(ColorDefinition)}.
 * The default ARGB here is the fallback used by
 * {@link MarieColors#resolveColor(ColorKey)} when no user/datapack
 * override exists in {@link ColorRegistry}.</p>
 */
@ApiStatus.Stable
public final class ColorDefinition {

    private final ColorKey key;
    private final int defaultArgb;

    private ColorDefinition(ColorKey key, int defaultArgb) {
        this.key = key;
        this.defaultArgb = defaultArgb;
    }

    /**
     * Creates a color definition.
     *
     * @param key         the unique identity for this color
     * @param defaultArgb the default packed ARGB value used when no override exists
     * @return the constructed definition
     */
    @ApiStatus.Stable
    public static ColorDefinition of(ColorKey key, int defaultArgb) {
        Objects.requireNonNull(key, "key");
        return new ColorDefinition(key, defaultArgb);
    }

    @ApiStatus.Stable
    public ColorKey getKey() {
        return key;
    }

    @ApiStatus.Stable
    public int getDefaultArgb() {
        return defaultArgb;
    }
}
