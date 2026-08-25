package dev.marie.framework.registry;

import dev.marie.framework.api.ApiStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Keyed registry with a mutable registration phase and an immutable frozen runtime phase.
 *
 * <p>After {@link #freeze()}, read paths use only immutable snapshots and do not allocate.
 * Mutation attempts throw {@link IllegalStateException} including the registry {@link #name()}
 * in the message.</p>
 */
@ApiStatus.Internal
public abstract class AbstractRegistry<K, V> {

    private final String name;
    private final LinkedHashMap<K, V> mutable = new LinkedHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private int duplicateAttemptCount;

    private volatile boolean frozen;
    private Map<K, V> frozenEntries = Map.of();
    private List<K> frozenKeys = List.of();
    private List<V> frozenValues = List.of();
    private Instant freezeTime = Instant.EPOCH;

    protected AbstractRegistry(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public final String name() {
        return name;
    }

    /**
     * Runs {@code action} while holding the write lock so reset → repopulate → freeze is atomic.
     */
    public final void runWrite(Runnable action) {
        lock.writeLock().lock();
        try {
            action.run();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Registers a key-value pair in insertion order. Not valid after {@link #freeze()}.
     *
     * @throws IllegalArgumentException if {@code key} or {@code value} is null
     * @throws IllegalStateException    if the registry is frozen or the key is already present
     */
    public final void register(K key, V value) {
        lock.writeLock().lock();
        try {
            registerUnlocked(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes a single key-value pair, if present. Not valid after {@link #freeze()}. Unlike
     * {@link #reset()}, this does not clear the registry — it removes exactly one entry and
     * leaves everything else in place.
     *
     * <pre>{@code
     * boolean removed = registry.unregister("hundred_blocks_mined");
     * }</pre>
     *
     * @param key the key to remove
     * @return {@code true} if an entry for {@code key} was present and removed, {@code false} if
     *         {@code key} was null or not present
     * @throws IllegalStateException if the registry is frozen
     */
    public final boolean unregister(K key) {
        lock.writeLock().lock();
        try {
            if (frozen) {
                throw new IllegalStateException(name + ": cannot unregister while frozen");
            }
            if (key == null) {
                return false;
            }
            return mutable.remove(key) != null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public final V get(K key) {
        if (key == null) {
            return null;
        }
        lock.readLock().lock();
        try {
            if (frozen) {
                return frozenEntries.get(key);
            }
            return mutable.get(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public final boolean contains(K key) {
        if (key == null) {
            return false;
        }
        lock.readLock().lock();
        try {
            if (frozen) {
                return frozenEntries.containsKey(key);
            }
            return mutable.containsKey(key);
        } finally {
            lock.readLock().unlock();
        }
    }

    public final List<V> values() {
        lock.readLock().lock();
        try {
            return valuesUnlocked();
        } finally {
            lock.readLock().unlock();
        }
    }

    public final List<K> keys() {
        lock.readLock().lock();
        try {
            return keysUnlocked();
        } finally {
            lock.readLock().unlock();
        }
    }

    public final Map<K, V> entries() {
        lock.readLock().lock();
        try {
            if (frozen) {
                return frozenEntries;
            }
            return Collections.unmodifiableMap(new LinkedHashMap<>(mutable));
        } finally {
            lock.readLock().unlock();
        }
    }

    public final int size() {
        lock.readLock().lock();
        try {
            if (frozen) {
                return frozenEntries.size();
            }
            return mutable.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Builds immutable snapshots, locks the registry, and invokes {@link #onFreeze()}.
     * Idempotent if already frozen.
     */
    public final void freeze() {
        lock.writeLock().lock();
        try {
            freezeUnlocked();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Temporarily reopens a frozen registry for re-registration, restoring the frozen entries
     * into the mutable map so {@link #upsert(Object, Object)}/{@link #register(Object, Object)}
     * calls succeed instead of throwing. Callers must re-{@link #freeze()} immediately after the
     * re-registration window closes (in a {@code finally} block) to restore the freeze guarantee.
     * No-op if not currently frozen.
     */
    @ApiStatus.Internal
    public final void unfreeze() {
        lock.writeLock().lock();
        try {
            if (!frozen) {
                return;
            }
            mutable.clear();
            mutable.putAll(frozenEntries);
            frozen = false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clears all state, unlocks the registry, and invokes {@link #onReset()}.
     */
    public final void reset() {
        lock.writeLock().lock();
        try {
            resetUnlocked();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public final boolean isFrozen() {
        lock.readLock().lock();
        try {
            return frozen;
        } finally {
            lock.readLock().unlock();
        }
    }

    public final RegistrySnapshot createSnapshot() {
        lock.readLock().lock();
        try {
            return new RegistrySnapshot(name, sizeUnlocked(), freezeTime, duplicateAttemptCount);
        } finally {
            lock.readLock().unlock();
        }
    }

    public final void registerUnlocked(K key, V value) {
        if (frozen) {
            throw new IllegalStateException(name + ": cannot register while frozen");
        }
        if (key == null) {
            throw new IllegalArgumentException(name + ": key cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException(name + ": value cannot be null");
        }
        validateEntry(key, value);
        if (mutable.containsKey(key)) {
            noteDuplicateAttempt();
            throw new IllegalStateException(name + ": duplicate key: " + key);
        }
        mutable.put(key, value);
    }

    /**
     * Registers a key-value pair, replacing any existing entry for {@code key} instead of
     * throwing. Not valid after {@link #freeze()}. Intended for registries where re-registration
     * with the same key is a legitimate, expected event (e.g. a mod re-registering its definitions
     * on every reload) rather than a programming error — {@link #register(Object, Object)} remains
     * the default duplicate-throwing behavior for every other registry.
     *
     * @throws IllegalArgumentException if {@code key} or {@code value} is null
     * @throws IllegalStateException    if the registry is frozen
     */
    public final void upsert(K key, V value) {
        lock.writeLock().lock();
        try {
            if (frozen) {
                throw new IllegalStateException(name + ": cannot register while frozen");
            }
            if (key == null) {
                throw new IllegalArgumentException(name + ": key cannot be null");
            }
            if (value == null) {
                throw new IllegalArgumentException(name + ": value cannot be null");
            }
            validateEntry(key, value);
            mutable.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public final void resetUnlocked() {
        mutable.clear();
        duplicateAttemptCount = 0;
        frozen = false;
        frozenEntries = Map.of();
        frozenKeys = List.of();
        frozenValues = List.of();
        freezeTime = Instant.EPOCH;
        onReset();
    }

    public final void freezeUnlocked() {
        if (frozen) {
            return;
        }
        Map<K, V> snap = Collections.unmodifiableMap(new LinkedHashMap<>(mutable));
        List<K> keysSnap = List.copyOf(snap.keySet());
        List<V> valuesSnap = List.copyOf(snap.values());
        Instant now = Instant.now();
        this.frozenEntries = snap;
        this.frozenKeys = keysSnap;
        this.frozenValues = valuesSnap;
        this.freezeTime = now;
        this.frozen = true;
        onFreeze();
    }

    public final List<V> valuesUnlocked() {
        if (frozen) {
            return frozenValues;
        }
        return Collections.unmodifiableList(new ArrayList<>(mutable.values()));
    }

    public final List<K> keysUnlocked() {
        if (frozen) {
            return frozenKeys;
        }
        return Collections.unmodifiableList(new ArrayList<>(mutable.keySet()));
    }

    private int sizeUnlocked() {
        if (frozen) {
            return frozenEntries.size();
        }
        return mutable.size();
    }

    protected void validateEntry(K key, V value) {}

    protected void onFreeze() {}

    protected void onReset() {}

    protected final void noteDuplicateAttempt() {
        duplicateAttemptCount++;
    }
}
