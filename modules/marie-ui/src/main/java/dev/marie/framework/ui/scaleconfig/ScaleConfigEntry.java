package dev.marie.framework.ui.scaleconfig;

import dev.marie.framework.api.ApiStatus;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

/** One card of {@link ScaleConfigPanel}: a component id, its display label, and an optional accent color — null falls back to the panel's cycling palette. */
@ApiStatus.Experimental
public record ScaleConfigEntry(String componentId, Component label, @Nullable Integer accentColor) {

    public ScaleConfigEntry(String componentId, Component label) {
        this(componentId, label, null);
    }
}
