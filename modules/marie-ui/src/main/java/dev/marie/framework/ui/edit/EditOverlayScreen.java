package dev.marie.framework.ui;

import dev.marie.framework.ui.render.GuiGraphicsRenderContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Transparent full-screen overlay that captures all input while a {@link MarieComponent} is being
 * edited (dragged/resized). Generalizes Nourished's HUDEditScreen/DietScreenEditScreen, which
 * were structurally identical aside from their hint text, exit key, and delegate target.
 * isPauseScreen()=false keeps the world ticking; input is forwarded to {@code target} rather
 * than handled here.
 */
public final class EditOverlayScreen extends Screen {

    private final MarieComponent target;
    private final String hintText;
    private final int exitKeyCode;
    private final Runnable onExit;
    private final Theme theme;

    public EditOverlayScreen(MarieComponent target, String hintText, int exitKeyCode, Runnable onExit) {
        this(target, hintText, exitKeyCode, onExit, Theme.DARK);
    }

    public EditOverlayScreen(MarieComponent target, String hintText, int exitKeyCode, Runnable onExit, Theme theme) {
        super(Component.empty());
        this.target = target;
        this.hintText = hintText;
        this.exitKeyCode = exitKeyCode;
        this.onExit = onExit;
        this.theme = theme;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Suppress the default dirt/blur background — the overlay must stay fully transparent. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // intentionally empty
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (minecraft == null) {
            return;
        }
        RenderContext context = new GuiGraphicsRenderContext(graphics, minecraft, theme, partialTick);
        target.render(context, new Bounds(0, 0, context.screenWidth(), context.screenHeight()));
        drawHintBanner(context);
    }

    /** Ported from HudDrawHelpers#drawEditBanner: centered text over a fitted background fill. */
    private void drawHintBanner(RenderContext context) {
        if (minecraft == null || minecraft.font == null) {
            return;
        }
        int textW = minecraft.font.width(hintText);
        int bannerX = (context.screenWidth() - textW) / 2 - 4;
        context.fillRect(bannerX, 4, textW + 8, 13, context.theme().color(ThemeKey.EDIT_BANNER_BACKGROUND));
        context.drawText(hintText, bannerX + 4, 8, context.theme().color(ThemeKey.EDIT_BANNER_TEXT), 1f);
    }

    /** exitKeyCode (or Escape) exits edit mode; all other keys are consumed to freeze player movement. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == exitKeyCode || keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onExit.run();
            if (minecraft != null) {
                minecraft.setScreen(null);
            }
            return true;
        }
        target.keyPressed(keyCode, scanCode, modifiers);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        target.mouseClicked(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        target.mouseReleased(mouseX, mouseY, button);
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        target.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return true;
    }

    /** Called when Minecraft closes the screen externally (e.g. player death). */
    @Override
    public void onClose() {
        onExit.run();
    }
}
