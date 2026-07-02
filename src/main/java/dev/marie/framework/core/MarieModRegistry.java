package dev.marie.framework.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import dev.marie.framework.api.ApiStatus;

/**
 * Insertion-ordered registry of all mods that have registered a
 * {@link MarieLibContext}. Supports the upcoming multi-mod config UI
 * and any feature that needs to enumerate consuming mods.
 */
@ApiStatus.Experimental
public final class MarieModRegistry {

    private static final Map<String, MarieLibContext> MODS = new LinkedHashMap<>();

    private MarieModRegistry() {}

    public static synchronized void register(MarieLibContext ctx) {
        MODS.put(ctx.modId(), ctx);
    }

    @Nullable
    public static synchronized MarieLibContext get(String modId) {
        return MODS.get(modId);
    }

    public static synchronized MarieLibContext getOrThrow(String modId) {
        MarieLibContext ctx = MODS.get(modId);
        if (ctx == null) {
            throw new IllegalStateException("No MarieLibContext registered for mod: " + modId);
        }
        return ctx;
    }

    public static synchronized List<MarieLibContext> getAll() {
        return Collections.unmodifiableList(List.copyOf(MODS.values()));
    }

    public static synchronized MarieLibContext getPrimary() {
        MarieLibContext ctx = getPrimaryOrNull();
        if (ctx == null) {
            throw new IllegalStateException("No MarieLibContext has been registered");
        }
        return ctx;
    }

    @Nullable
    public static synchronized MarieLibContext getPrimaryOrNull() {
        if (MODS.isEmpty()) {
            return null;
        }
        return MODS.values().iterator().next();
    }

    public static synchronized boolean isRegistered() {
        return !MODS.isEmpty();
    }

    public static synchronized boolean isRegistered(String modId) {
        return MODS.containsKey(modId);
    }
}
