package dev.marie.MariesLib.api;

import dev.marie.MariesLib.core.MariesLib;

import java.util.concurrent.atomic.AtomicReference;

@ApiStatus.Internal
public final class MarieAPIState {

    public enum Phase {
        MOD_INIT,
        DATAPACK_RELOAD,
        CLOSED
    }

    private static final AtomicReference<Phase> currentPhase =
            new AtomicReference<>(Phase.MOD_INIT);

    private MarieAPIState() {}

    public static boolean isRegistrationAllowed() {
        Phase phase = currentPhase.get();
        return phase == Phase.MOD_INIT || phase == Phase.DATAPACK_RELOAD;
    }

    public static Phase getPhase() {
        return currentPhase.get();
    }

    /**
     * Asserts registration is currently allowed.
     * Thread-safe: reads the atomic reference once.
     */
    public static void assertRegistrationAllowed(String context) {
        Phase p = currentPhase.get();
        if (p != Phase.MOD_INIT && p != Phase.DATAPACK_RELOAD) {
            throw new IllegalStateException(
                    "[MarieAPI] Registration closed — " + context +
                    " must be called during mod initialization or datapack reload.");
        }
    }

    /**
     * Ends the mod-init registration window. Safe to call when already {@link Phase#CLOSED}
     * (for example after a nested {@link DatapackReloadScope}).
     */
    @ApiStatus.Internal
    public static void close() {
        if (currentPhase.get() == Phase.CLOSED) {
            return;
        }
        currentPhase.set(Phase.CLOSED);
        MariesLib.LOGGER.info("[MarieLib] Registration phase: CLOSED");
    }

    @ApiStatus.Internal
    public static DatapackReloadScope openForDatapackReload() {
        currentPhase.set(Phase.DATAPACK_RELOAD);
        MariesLib.LOGGER.info("[MarieLib] Registration phase: DATAPACK_RELOAD");
        return new DatapackReloadScope();
    }

    public static final class DatapackReloadScope implements AutoCloseable {
        @Override
        public void close() {
            currentPhase.set(Phase.CLOSED);
            MariesLib.LOGGER.info("[MarieLib] Registration phase: CLOSED");
        }
    }
}
