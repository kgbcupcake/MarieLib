package dev.marie.MariesLib.api;

import dev.marie.MariesLib.api.ApiStatus;

/**
 * Marker for custom HUD rendering implementations.
 *
 * <p>Client-side implementations should extend {@code dev.marie.MariesLib.client.GuiValueRenderer}
 * which adds the {@code GuiGraphics}-based draw contract.</p>
 */
@ApiStatus.Experimental
public interface ValueRenderer {
}
