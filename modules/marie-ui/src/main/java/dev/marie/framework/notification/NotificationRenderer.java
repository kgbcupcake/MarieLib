package dev.marie.framework.notification;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

import java.util.List;

/** Draws the active notification stack anchored above vanilla's XP bar. */
final class NotificationRenderer {

    /** Time constant (seconds) for the per-frame Y-position ease; frame-rate independent. */
    private static final double Y_EASE_TIME_CONSTANT = 0.12;

    private static long lastFrameNanos;

    private NotificationRenderer() {}

    static void onRenderGuiPost(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) {
            return;
        }

        long nowNanos = System.nanoTime();
        double dtSeconds = lastFrameNanos == 0L
                ? 0d
                : Mth.clamp((nowNanos - lastFrameNanos) / 1_000_000_000.0, 0.0, 0.25);
        lastFrameNanos = nowNanos;

        List<NotificationSlot> slots = NotificationManager.activeSlots();
        if (slots.isEmpty()) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        Font font = mc.font;
        NotificationConfig cfg = NotificationConfig.get();

        int centerX = graphics.guiWidth() / 2;
        int anchorY = xpBarTopY(mc, graphics) - cfg.verticalGap;
        long now = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int cumulativeHeight = 0;
        for (NotificationSlot slot : slots) {
            float scale = slot.emphasized ? cfg.emphasizedScale : cfg.baseScale;
            int lineHeight = Math.round((font.lineHeight + 1) * scale);
            int slotHeight = slot.content.size() * lineHeight;

            int targetY = anchorY - cumulativeHeight - slotHeight;
            cumulativeHeight += slotHeight + cfg.slotGap;

            if (!slot.yInitialized) {
                slot.currentY = targetY;
                slot.yInitialized = true;
            } else if (dtSeconds > 0) {
                double ease = 1 - Math.exp(-dtSeconds / Y_EASE_TIME_CONSTANT);
                slot.currentY += (targetY - slot.currentY) * ease;
            }

            float alpha = alphaFor(slot, now, cfg);
            if (alpha <= 0f) {
                continue;
            }

            float slideOffset = slideOffsetFor(slot, now, cfg);
            renderSlot(graphics, font, slot, centerX, Math.round((float) slot.currentY + slideOffset), scale, alpha);
        }

        RenderSystem.disableBlend();
    }

    /** Mirrors vanilla's own XP bar top-Y calculation (Gui#renderExperienceBar), which shifts by
     * whether the bar is currently visible (mounted on a jumpable vehicle hides it). */
    private static int xpBarTopY(Minecraft mc, GuiGraphics graphics) {
        boolean barVisible = mc.player.jumpableVehicle() == null && mc.gameMode != null && mc.gameMode.hasExperience();
        return barVisible ? graphics.guiHeight() - 32 + 3 : graphics.guiHeight() - 32;
    }

    private static float alphaFor(NotificationSlot slot, long now, NotificationConfig cfg) {
        long elapsed = now - slot.lastTriggeredMs;
        long fadeInMs = NotificationSlot.ticksToMs(cfg.fadeInTicks);
        long fadeOutMs = NotificationSlot.ticksToMs(cfg.fadeOutTicks);
        long duration = slot.durationMs();

        if (fadeInMs > 0 && elapsed < fadeInMs) {
            return Mth.clamp((float) elapsed / fadeInMs, 0f, 1f);
        }
        long remaining = duration - elapsed;
        if (fadeOutMs > 0 && remaining < fadeOutMs) {
            return Mth.clamp((float) remaining / fadeOutMs, 0f, 1f);
        }
        return 1f;
    }

    private static float slideOffsetFor(NotificationSlot slot, long now, NotificationConfig cfg) {
        long fadeInMs = NotificationSlot.ticksToMs(cfg.fadeInTicks);
        if (fadeInMs <= 0) {
            return 0f;
        }
        long elapsed = now - slot.lastTriggeredMs;
        if (elapsed >= fadeInMs) {
            return 0f;
        }
        float progress = Mth.clamp((float) elapsed / fadeInMs, 0f, 1f);
        return (1f - progress) * 4f;
    }

    private static void renderSlot(
            GuiGraphics graphics, Font font, NotificationSlot slot, int centerX, int y, float scale, float alpha) {
        int alphaByte = Mth.clamp(Mth.floor(alpha * 255f), 0, 255);
        if (alphaByte <= 0) {
            return;
        }

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, y, 0);
        graphics.pose().scale(scale, scale, 1f);

        int lineY = 0;
        for (List<TextSegment> line : slot.content) {
            int x = -lineWidth(font, line) / 2;
            for (TextSegment segment : line) {
                int color = applyAlpha(segment.argbColor(), alphaByte);
                graphics.drawString(font, segment.text(), x, lineY, color, false);
                x += font.width(segment.text());
            }
            lineY += font.lineHeight + 1;
        }

        graphics.pose().popPose();
    }

    private static int lineWidth(Font font, List<TextSegment> line) {
        int width = 0;
        for (TextSegment segment : line) {
            width += font.width(segment.text());
        }
        return width;
    }

    private static int applyAlpha(int argb, int alphaByte) {
        int baseAlpha = (argb >>> 24) & 0xFF;
        if (baseAlpha == 0) {
            baseAlpha = 0xFF;
        }
        int combined = Mth.clamp(Mth.floor(baseAlpha * (alphaByte / 255f)), 0, 255);
        return (combined << 24) | (argb & 0xFFFFFF);
    }
}
