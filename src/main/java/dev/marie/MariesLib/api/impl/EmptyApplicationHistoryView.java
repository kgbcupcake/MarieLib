package dev.marie.MariesLib.api.impl;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.ApplicationHistoryView;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Null-object {@link ApplicationHistoryView} returned when API queries receive a null player
 * or when the context provider is not registered.
 */
@ApiStatus.Internal
public final class EmptyApplicationHistoryView implements ApplicationHistoryView {

    public static final EmptyApplicationHistoryView INSTANCE = new EmptyApplicationHistoryView();

    private EmptyApplicationHistoryView() {}

    @Override
    public List<ResourceLocation> getRecentSources() {
        return List.of();
    }

    @Override
    public boolean hasSourceRecently(ResourceLocation sourceId) {
        return false;
    }

    @Override
    public long getTimeSinceSource(ResourceLocation sourceId) {
        return -1L;
    }
}
