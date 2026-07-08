package dev.marie.framework.ui.edit;

import dev.marie.framework.ui.component.MarieComponent;
import net.minecraft.client.Minecraft;

/**
 * Per-target edit-mode state and screen lifecycle. Generalizes Nourished's HUDEditMode/
 * DietScreenEditMode, which each tracked a single static boolean and could therefore only ever
 * have one editable target active at a time. Here each editable target (a HUD panel, a Diet
 * Screen, a future crafting-station panel, etc.) owns its own controller instance, so any number
 * of targets can independently be in edit mode.
 */
public final class EditModeController {

    private final MarieComponent target;
    private final String hintText;
    private final int exitKeyCode;
    private final Runnable onExit;

    private EditOverlayScreen screen;

    public EditModeController(MarieComponent target, String hintText, int exitKeyCode, Runnable onExit) {
        this.target = target;
        this.hintText = hintText;
        this.exitKeyCode = exitKeyCode;
        this.onExit = onExit;
    }

    public boolean isActive() {
        return screen != null;
    }

    /** Opens an {@link EditOverlayScreen} wrapping the target. No-op if already active. */
    public void enter() {
        if (isActive()) {
            return;
        }
        screen = new EditOverlayScreen(target, hintText, exitKeyCode, this::handleExit);
        Minecraft.getInstance().setScreen(screen);
    }

    /**
     * Closes the screen if this controller opened it, and runs onExit unless it already ran via
     * the screen's own exit path (exit key/Escape, or {@link EditOverlayScreen#onClose()}).
     */
    public void exit() {
        if (!isActive()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == screen) {
            mc.setScreen(null);
        }
        handleExit();
    }

    /** Idempotent: the screen may already have invoked this via its own exit path. */
    private void handleExit() {
        if (screen == null) {
            return;
        }
        screen = null;
        onExit.run();
    }
}
