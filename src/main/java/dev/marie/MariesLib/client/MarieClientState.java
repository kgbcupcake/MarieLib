package dev.marie.MariesLib.client;

import dev.marie.MariesLib.network.SyncState;

/**
 * Client-only runtime mirror of server-authoritative config.
 * Never access on a dedicated server — guarded by isReady() at all call sites.
 */
public final class MarieClientState {

    private static volatile Object snapshot = null;
    private static volatile SyncState state = SyncState.UNINITIALIZED;

    private MarieClientState() {}

    public static boolean isReady() {
        if (isRunningOnDedicatedServer()) {
            return false;
        }
        return state == SyncState.ACTIVE;
    }

    /** Returns the current snapshot, or null if not yet received. Callers must guard with isReady(). */
    public static Object getConfig() {
        if (isRunningOnDedicatedServer()) {
            return null;
        }
        return snapshot;
    }

    /** Called when a config snapshot packet arrives. Transitions state to PENDING (awaiting full tracking sync). */
    public static void setConfig(Object received) {
        snapshot = received;
        state = SyncState.PENDING;
    }

    /** Called when a full tracking sync packet arrives. Transitions state to ACTIVE. */
    public static void onFullTrackingSynced() {
        state = SyncState.ACTIVE;
    }

    /** Resets to uninitialized, e.g. on disconnect. */
    public static void reset() {
        snapshot = null;
        state = SyncState.UNINITIALIZED;
    }

    private static boolean isRunningOnDedicatedServer() {
        return net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.DEDICATED_SERVER;
    }
}
