package dev.marie.framework.client.config.render;

import dev.marie.framework.api.ValueRenderer;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Client-side {@link ValueRenderer} with a {@link GuiGraphics} draw contract.
 */
public interface GuiValueRenderer extends ValueRenderer {

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
