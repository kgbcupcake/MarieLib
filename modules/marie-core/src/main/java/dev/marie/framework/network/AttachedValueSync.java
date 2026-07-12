package dev.marie.framework.network;

import dev.marie.framework.api.ApiStatus;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Tracks the last-synced value of type {@code T} and invokes a consumer-supplied broadcaster
 * only when the value actually changes, per the supplied {@code changeDetector}.
 * <p>Not thread-safe — intended for per-entity, single-thread (server tick) use, one instance
 * per synced value per entity (e.g. held in an attachment or a per-entity map).</p>
 */
@ApiStatus.Experimental
public final class AttachedValueSync<T> {

    private final Consumer<T> broadcaster;
    private final BiPredicate<T, T> changeDetector;
    private T lastSynced;
    private boolean hasSynced;

    public AttachedValueSync(Consumer<T> broadcaster) {
        this(broadcaster, (a, b) -> !java.util.Objects.equals(a, b));
    }

    public AttachedValueSync(Consumer<T> broadcaster, BiPredicate<T, T> changeDetector) {
        this.broadcaster = broadcaster;
        this.changeDetector = changeDetector;
    }

    /**
     * Call after mutating the underlying value. Broadcasts and records {@code newValue} as the
     * new baseline only if it differs from the last synced value (or nothing has synced yet).
     */
    public void syncIfChanged(T newValue) {
        if (!hasSynced || changeDetector.test(lastSynced, newValue)) {
            broadcaster.accept(newValue);
            lastSynced = newValue;
            hasSynced = true;
        }
    }

    /** Forces the next {@link #syncIfChanged} call to broadcast regardless of value equality. */
    public void reset() {
        hasSynced = false;
        lastSynced = null;
    }
}
