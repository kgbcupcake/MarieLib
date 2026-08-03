package dev.marie.framework.color;

import dev.marie.framework.api.ApiStatus;

/**
 * Identity pair for a background/text color combo registered together via
 * {@link MarieColors#registerColorPair(String, String, int, int)}.
 */
@ApiStatus.Stable
public record ColorKeyPair(ColorKey background, ColorKey text) {
}
