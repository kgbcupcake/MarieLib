package dev.marie.MariesLib.api;

import dev.marie.MariesLib.api.ApiStatus;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Provides custom rendering logic for a value's HUD representation.
 *
 * <p>Implement this interface to replace the default value bar rendering
 * with a custom visual (icon, radial gauge, particle effect, etc.).</p>
 */
@ApiStatus.Experimental
public interface ValueRenderer {

    /**
     * Renders this value's visual representation at the given screen coordinates.
     *
     * @param graphics the current GUI graphics context for draw calls
     * @param x        the left x-coordinate in screen space where rendering should begin
     * @param y        the top y-coordinate in screen space where rendering should begin
     * @param level    the current value level, normalized between 0.0 (depleted) and 1.0 (full)
     */
    void render(GuiGraphics graphics, int x, int y, float level);
}
