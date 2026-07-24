package dev.marie.framework.ui.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayDeque;

/**
 * The one concrete {@link RenderContext} MarieUI ships: draws through NeoForge's
 * {@link GuiGraphics}. Callers construct a fresh instance per frame.
 */
public final class GuiGraphicsRenderContext implements RenderContext {

    private final GuiGraphics graphics;
    private final Minecraft minecraft;
    private final Theme theme;
    private final float partialTick;

    /**
     * {@code [x1, y1, x2, y2]} per active {@link #pushClip}, innermost on top — a fresh instance is
     * constructed per frame (see class javadoc), so this never leaks state across frames.
     */
    private final ArrayDeque<int[]> clipStack = new ArrayDeque<>();

    public GuiGraphicsRenderContext(GuiGraphics graphics, Minecraft minecraft, Theme theme, float partialTick) {
        this.graphics = graphics;
        this.minecraft = minecraft;
        this.theme = theme;
        this.partialTick = partialTick;
    }

    /**
     * Escape hatch for consumers that must issue raw {@link GuiGraphics} draw calls of their own
     * (e.g. a legacy non-MarieUI renderer invoked mid-frame during a MarieUI-managed drag/resize
     * interaction) rather than going through this class's {@link RenderContext} primitives.
     * Deliberately narrow — not a general invitation to bypass {@link RenderContext}.
     */
    public GuiGraphics graphics() {
        return graphics;
    }

    @Override
    public int screenWidth() {
        return minecraft.getWindow().getGuiScaledWidth();
    }

    @Override
    public int screenHeight() {
        return minecraft.getWindow().getGuiScaledHeight();
    }

    @Override
    public float partialTick() {
        return partialTick;
    }

    @Override
    public Theme theme() {
        return theme;
    }

    @Override
    public void fillRect(int x, int y, int width, int height, int argbColor) {
        graphics.fill(x, y, x + width, y + height, argbColor);
    }

    @Override
    public void drawBorder(int x, int y, int width, int height, int thickness, int argbColor) {
        graphics.fill(x, y, x + width, y + thickness, argbColor);
        graphics.fill(x, y + height - thickness, x + width, y + height, argbColor);
        graphics.fill(x, y + thickness, x + thickness, y + height - thickness, argbColor);
        graphics.fill(x + width - thickness, y + thickness, x + width, y + height - thickness, argbColor);
    }

    @Override
    public void drawDashedBorder(int x, int y, int width, int height, int argbColor) {
        int step = 4;
        int seg = 2;
        for (int i = 0; i < width; i += step) {
            graphics.fill(x + i, y, x + Math.min(i + seg, width), y + 1, argbColor);
            graphics.fill(x + i, y + height - 1, x + Math.min(i + seg, width), y + height, argbColor);
        }
        for (int i = 0; i < height; i += step) {
            graphics.fill(x, y + i, x + 1, y + Math.min(i + seg, height), argbColor);
            graphics.fill(x + width - 1, y + i, x + width, y + Math.min(i + seg, height), argbColor);
        }
    }

    @Override
    public void drawText(String text, int x, int y, int argbColor, float scale) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1f);
        graphics.drawString(minecraft.font, text, 0, 0, argbColor, false);
        pose.popPose();
    }

    @Override
    public void drawItem(ItemStack stack, int x, int y, float scale) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1f);
        graphics.renderItem(stack, 0, 0);
        pose.popPose();
    }

    @Override
    public void drawBar(int x, int y, int width, int height, float fillPct, int backgroundColor, int fillColor) {
        graphics.fill(x, y + 1, x + width, y + height - 1, backgroundColor);
        graphics.fill(x + 1, y, x + width - 1, y + 1, backgroundColor);
        graphics.fill(x + 1, y + height - 1, x + width - 1, y + height, backgroundColor);

        int filled = Mth.clamp((int) (width * fillPct), 0, width);
        if (filled <= 0) {
            return;
        }
        graphics.fill(x, y + 1, Math.min(x + filled, x + width), y + height - 1, fillColor);
        graphics.fill(x + 1, y, Math.min(x + filled, x + width - 1), y + 1, fillColor);
        graphics.fill(x + 1, y + height - 1, Math.min(x + filled, x + width - 1), y + height, fillColor);
        if (filled >= width) {
            graphics.fill(x + width - 1, y, x + width, y + 1, fillColor);
            graphics.fill(x + width - 1, y + height - 1, x + width, y + height, fillColor);
        }
    }

    @Override
    public void pushClip(int x, int y, int width, int height) {
        int x1 = x;
        int y1 = y;
        int x2 = x + width;
        int y2 = y + height;
        if (!clipStack.isEmpty()) {
            int[] parent = clipStack.peek();
            x1 = Math.max(x1, parent[0]);
            y1 = Math.max(y1, parent[1]);
            x2 = Math.min(x2, parent[2]);
            y2 = Math.min(y2, parent[3]);
        }
        x2 = Math.max(x1, x2);
        y2 = Math.max(y1, y2);
        clipStack.push(new int[]{x1, y1, x2, y2});
        graphics.enableScissor(x1, y1, x2, y2);
    }

    @Override
    public void popClip() {
        if (clipStack.isEmpty()) {
            return;
        }
        clipStack.pop();
        if (clipStack.isEmpty()) {
            graphics.disableScissor();
        } else {
            int[] parent = clipStack.peek();
            graphics.enableScissor(parent[0], parent[1], parent[2], parent[3]);
        }
    }

    @Override
    public void drawVerticalBar(int x, int y, int width, int height, float fillPct, int backgroundColor, int fillColor) {
        graphics.fill(x + 1, y, x + width - 1, y + height, backgroundColor);
        graphics.fill(x, y + 1, x + 1, y + height - 1, backgroundColor);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, backgroundColor);

        int filled = Mth.clamp((int) (height * fillPct), 0, height);
        if (filled <= 0) {
            return;
        }
        int fillTop = y + height - filled;
        graphics.fill(x + 1, fillTop, x + width - 1, y + height, fillColor);
        graphics.fill(x, fillTop + 1, x + 1, y + height - 1, fillColor);
        graphics.fill(x + width - 1, fillTop + 1, x + width, y + height - 1, fillColor);
        if (filled >= height) {
            graphics.fill(x + 1, y, x + width - 1, y + 1, fillColor);
        }
    }
}
