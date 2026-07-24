package dev.marie.framework.ui.component;

import java.util.List;

import dev.marie.framework.ui.edit.DraggableResizable;
import dev.marie.framework.ui.geometry.Bounds;


public final class AutoGrowPanelContainer {

    private AutoGrowPanelContainer() {}


    public static int naturalContentHeight(List<? extends SelfPositioningModule> modules, int startLocalY, double scale) {
        int cursorY = startLocalY;
        for (SelfPositioningModule module : modules) {
            cursorY = nextSiblingStartLocalY(cursorY, module.localHeight(), module.resolvedBounds(), scale);
        }
        return cursorY;
    }


    public static int nextSiblingStartLocalY(int currentLocalY, int localHeight, Bounds resolvedBounds, double scale) {
        if (localHeight <= 0) {
            return currentLocalY;
        }
        // resolvedBounds is already the sibling's true committed footprint — its manual override
        // if one exists, else exactly `localHeight`'s own natural size (see
        // DietScreenPersistence#resolveRelativeToPanel). Maxing it against `localHeight` used to
        // mean a sibling manually shrunk *below* its own natural size (e.g. a per-box scale config
        // bigger than what the user actually dragged it to) still reserved the bigger natural
        // footprint for whatever comes next — silently eating into later siblings' room with dead
        // space nothing was actually occupying.
        return currentLocalY + (int) Math.round(resolvedBounds.height() / scale);
    }

    public record ManualOverride(boolean widthManual, boolean heightManual) {
        public static final ManualOverride NONE = new ManualOverride(false, false);
    }


    public static ManualOverride withCommit(ManualOverride existing, DraggableResizable drag) {
        boolean width = existing.widthManual() || drag.lastCommitAffectedWidth();
        boolean height = existing.heightManual() || drag.lastCommitAffectedHeight();
        if (width == existing.widthManual() && height == existing.heightManual()) {
            return existing;
        }
        return new ManualOverride(width, height);
    }

    public static int resolveWidth(ManualOverride override, int persistedWidth, int naturalWidth) {
        return override.widthManual() ? persistedWidth : naturalWidth;
    }

    public static int resolveHeight(ManualOverride override, int persistedHeight, int naturalHeight) {
        return override.heightManual() ? persistedHeight : naturalHeight;
    }
}
