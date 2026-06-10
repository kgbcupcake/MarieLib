package dev.marie.MariesLib.api.impl;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.api.MemoryView;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Null-object {@link MemoryView} returned when API queries receive a null player
 * or when the context provider is not registered.
 */
@ApiStatus.Internal
public final class EmptyMemoryView implements MemoryView {

    public static final EmptyMemoryView INSTANCE = new EmptyMemoryView();

    private EmptyMemoryView() {}

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
