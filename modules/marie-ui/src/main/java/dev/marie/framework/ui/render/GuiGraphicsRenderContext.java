package dev.marie.framework.ui.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.marie.framework.ui.RenderContext;
import dev.marie.framework.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * The one concrete {@link RenderContext} MarieUI ships: draws through NeoForge's
 * {@link GuiGraphics}. Callers construct a fresh instance per frame.
 */
public final class GuiGraphicsRenderContext implements RenderContext {

    private final GuiGraphics graphics;
    private final Minecraft minecraft;
    private final Theme theme;
    private final float partialTick;

    public GuiGraphicsRenderContext(GuiGraphics graphics, Minecraft minecraft, Theme theme, float partialTick) {
        this.graphics = graphics;
        this.minecraft = minecraft;
        this.theme = theme;
        this.partialTick = partialTick;
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
