package dev.marie.framework.client;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.core.MarieCore;
import dev.marie.framework.ui.Theme;
import dev.marie.framework.ui.ThemeKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;

/**
 * Custom fallback renderer for {@link BlockHoverClientCache}. Not a MarieComponent — fixed
 * position, no drag/resize/persistence, just a simple always-on-top box near the crosshair.
 */
@ApiStatus.Internal
@EventBusSubscriber(modid = MarieCore.MOD_ID, value = Dist.CLIENT)
public final class BlockHoverOverlay {

    private static final int PADDING = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int HOTBAR_GAP = 30;
    private static final int BORDER_THICKNESS = 1;

    // Checked once: once a Jade adapter exists it renders this data instead, so this overlay
    // must stay silent whenever Jade is present to avoid drawing the same info twice.
    private static final boolean JADE_LOADED = ModList.get().isLoaded("jade");

    private BlockHoverOverlay() {}

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (JADE_LOADED) {
            return;
        }
        if (BlockHoverClientCache.getTrackedPos() == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            return;
        }

        List<Component> lines = BlockHoverClientCache.getRenderLines(level);
        if (lines.isEmpty()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        Theme theme = Theme.DARK;

        int contentWidth = 0;
        for (Component line : lines) {
            contentWidth = Math.max(contentWidth, font.width(line));
        }
        int boxWidth = contentWidth + PADDING * 2;
        int boxHeight = lines.size() * LINE_HEIGHT + PADDING * 2;

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();
        int x = (screenWidth - boxWidth) / 2;
        int y = screenHeight - HOTBAR_GAP - boxHeight;

        graphics.fill(x, y, x + boxWidth, y + boxHeight, theme.color(ThemeKey.PANEL_BACKGROUND));
        drawBorder(graphics, x, y, boxWidth, boxHeight, theme.color(ThemeKey.BORDER));

        int textColor = theme.color(ThemeKey.TEXT_PRIMARY);
        int textY = y + PADDING;
        for (Component line : lines) {
            int textX = x + (boxWidth - font.width(line)) / 2;
            graphics.drawString(font, line, textX, textY, textColor, false);
            textY += LINE_HEIGHT;
        }
    }

    private static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + BORDER_THICKNESS, color);
        graphics.fill(x, y + height - BORDER_THICKNESS, x + width, y + height, color);
        graphics.fill(x, y, x + BORDER_THICKNESS, y + height, color);
        graphics.fill(x + width - BORDER_THICKNESS, y, x + width, y + height, color);
    }
}
