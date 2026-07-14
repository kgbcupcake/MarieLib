package dev.marie.framework.api.source;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.api.marieapi.MarieAPI;

import net.neoforged.bus.api.IEventBus;

/**
 * Consuming-mod-provided handler that fires the value pipeline when
 * a gameplay action occurs.
 *
 * <p>The consuming mod registers NeoForge event listeners itself and
 * calls {@link MarieAPI#fireSourceTrigger} when appropriate. This
 * interface is optional — it exists as a convenience contract for
 * mods that want MarieLib to manage handler registration lifecycle.</p>
 *
 * <p>Register via {@link MarieAPI#registerTriggerHandler}.</p>
 */
@ApiStatus.Stable
public interface SourceTriggerListener {

    /**
     * Called once during server-side event bus registration.
     * The implementation should subscribe its NeoForge event listeners here.
     *
     * @param eventBus the NeoForge game event bus
     */
    void register(IEventBus eventBus);
}
