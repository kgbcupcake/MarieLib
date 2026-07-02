package dev.marie.framework.api;

import dev.marie.framework.api.ApiStatus;

/**
 * Marker for custom HUD rendering implementations.
 *
 * <p>Client-side implementations should extend {@code dev.marie.framework.client.GuiValueRenderer}
 * which adds the {@code GuiGraphics}-based draw contract.</p>
 */
@ApiStatus.Experimental
public interface ValueRenderer {
}
