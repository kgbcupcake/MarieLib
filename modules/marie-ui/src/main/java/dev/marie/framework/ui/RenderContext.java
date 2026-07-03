package dev.marie.framework.ui;

import net.minecraft.world.item.ItemStack;

/**
 * Everything a {@link MarieComponent} needs to draw itself: frame/screen metadata, the active
 * {@link Theme}, and generic drawing primitives.
 *
 * <p>No Minecraft <em>rendering-pipeline</em> types (e.g. {@code GuiGraphics}, {@code PoseStack})
 * leak into this contract — concrete implementations (e.g. a GuiGraphics-backed one) live outside
 * dev.marie.framework.ui. Domain types intrinsic to what's being drawn, such as {@link ItemStack},
 * are unavoidable for a Minecraft-native UI framework and are accepted as parameters where a
 * primitive genuinely requires them.
 */
public interface RenderContext {

    int screenWidth();

    int screenHeight();

    float partialTick();

    Theme theme();

    void fillRect(int x, int y, int width, int height, int argbColor);

    void drawBorder(int x, int y, int width, int height, int thickness, int argbColor);

    void drawDashedBorder(int x, int y, int width, int height, int argbColor);

    void drawText(String text, int x, int y, int argbColor, float scale);

    void drawBar(int x, int y, int width, int height, float fillPct, int backgroundColor, int fillColor);

    void drawVerticalBar(int x, int y, int width, int height, float fillPct, int backgroundColor, int fillColor);

    /**
     * Draws an already-resolved {@link ItemStack} as a scaled icon at (x, y). Callers are
     * responsible for resolving whatever domain key (e.g. a Nourished nutrient key) to an
     * {@code ItemStack} themselves — this primitive only knows how to draw one.
     */
    void drawItem(ItemStack stack, int x, int y, float scale);

    /**
     * Draws a resize-handle square at (x, y) — the handle's own top-left corner, e.g. from
     * {@link DraggableResizable#handleBounds(Bounds)} — with a corner glyph, colored via
     * {@link ThemeKey#HANDLE_ACTIVE}/{@link ThemeKey#HANDLE_HOVER}/{@link ThemeKey#HANDLE_BACKGROUND}.
     * Ported from Nourished's {@code HudDrawHelpers#drawResizeHandle} geometry, minus the "Drag to
     * resize" tooltip — that's presentation text a caller can add from its own render() if wanted.
     */
    default void drawResizeHandle(int x, int y, boolean hovered, boolean active) {
        int size = DraggableResizable.RESIZE_HANDLE_SIZE;
        int handleColor = active
                ? theme().color(ThemeKey.HANDLE_ACTIVE)
                : hovered ? theme().color(ThemeKey.HANDLE_HOVER) : theme().color(ThemeKey.HANDLE_BACKGROUND);
        fillRect(x, y, size, size, handleColor);
        drawText("◢", x + 1, y, 0xFF101010, 1f);
    }
}
