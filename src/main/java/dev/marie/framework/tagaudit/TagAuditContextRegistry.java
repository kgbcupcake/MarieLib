package dev.marie.MariesLib.tagaudit;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.tagaudit.model.TagAuditContext;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@ApiStatus.Internal
public final class TagAuditContextRegistry {

    private static final Map<String, TagAuditContext> CONTEXTS = new LinkedHashMap<>();

    private TagAuditContextRegistry() {}

    public static void register(String modId, TagAuditContext context) {
        CONTEXTS.put(modId, context);
    }

    public static @Nullable TagAuditContext get(String modId) {
        return CONTEXTS.get(modId);
    }

    public static Map<String, TagAuditContext> getAll() {
        return Collections.unmodifiableMap(CONTEXTS);
    }
}
