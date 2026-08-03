package dev.marie.framework.resources.api;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.network.SyncState;
import dev.marie.framework.resources.network.MarieResourcesNetworking;
import dev.marie.framework.resources.registry.ClientConfigSyncStateRegistry;
import dev.marie.framework.resources.registry.ConfigSyncClientHandlerRegistry;
import dev.marie.framework.resources.registry.ConfigSyncSupplierRegistry;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Public facade for marie-resources's generic server→client registry sync mechanism — the single
 * entry point downstream mods use instead of hand-rolling their own sync payload, channel, and
 * login handler for every server-loaded JSON registry.
 *
 * <p>This mirrors {@code dev.marie.framework.api.marieapi.MarieAPI}'s role for marie-core, but
 * lives in this module since the mechanism it fronts (config/registry snapshot sync) is
 * marie-resources-specific.</p>
 */
public final class MarieResourcesAPI {

    private MarieResourcesAPI() {}

    /**
     * Registers the server-side snapshot builder for {@code registryId}. Called on player login
     * and on {@link #broadcastConfigSyncReload} to produce the {@link CompoundTag} sent to
     * clients — MarieLib does not interpret its contents.
     *
     * <p>Must be called during mod initialization.</p>
     *
     * @param registryId unique id identifying this registry's sync channel
     * @param supplier   builds a fresh snapshot of the registry's current state
     * @throws IllegalArgumentException if {@code registryId} or {@code supplier} is null
     * @throws IllegalStateException    if {@code registryId} is already registered
     */
    @ApiStatus.Stable
    public static void registerConfigSyncSupplier(String registryId, Supplier<CompoundTag> supplier) {
        ConfigSyncSupplierRegistry.instance().register(registryId, supplier);
    }

    /**
     * Registers the client-side apply function for {@code registryId}. Invoked whenever a
     * {@link dev.marie.framework.resources.network.GenericConfigSyncPayload} for this registry is
     * received. MarieLib does not interpret the tag's contents — the handler defines what the
     * synced state means and how to apply it.
     *
     * <p>Must be called during mod initialization.</p>
     *
     * @param registryId unique id identifying this registry's sync channel
     * @param handler    applies the received snapshot on the client
     * @throws IllegalArgumentException if {@code registryId} or {@code handler} is null
     * @throws IllegalStateException    if {@code registryId} is already registered
     */
    @ApiStatus.Stable
    public static void registerConfigSyncClientHandler(String registryId, Consumer<CompoundTag> handler) {
        ConfigSyncClientHandlerRegistry.instance().register(registryId, handler);
    }

    /**
     * Rebuilds {@code registryId}'s snapshot via its registered supplier and resends it to every
     * connected player. Call this after a server-side reload of the underlying registry.
     *
     * @param server     the running server whose connected players receive the resent snapshot
     * @param registryId the registry to rebuild and resend
     */
    @ApiStatus.Stable
    public static void broadcastConfigSyncReload(MinecraftServer server, String registryId) {
        MarieResourcesNetworking.broadcastReload(server, registryId);
    }

    /**
     * Returns the client-side sync state for {@code registryId}: {@code UNINITIALIZED} until the
     * first snapshot arrives, {@code PENDING} while one is being applied, {@code ACTIVE} once
     * applied. Client-side only.
     *
     * @param registryId the registry to query
     */
    @ApiStatus.Stable
    public static SyncState getConfigSyncState(String registryId) {
        return ClientConfigSyncStateRegistry.get(registryId);
    }
}
