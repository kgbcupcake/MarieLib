package dev.marie.framework.ui.visibility;

import dev.marie.framework.ui.VisibilityRule;

import java.util.function.BooleanSupplier;

/** Visible iff a wrapped config/feature-flag supplier currently returns true. */
public final class ConfigToggleVisibility implements VisibilityRule {

    private final BooleanSupplier toggle;

    public ConfigToggleVisibility(BooleanSupplier toggle) {
        this.toggle = toggle;
    }

    @Override
    public boolean isVisible() {
        return toggle.getAsBoolean();
    }
}
