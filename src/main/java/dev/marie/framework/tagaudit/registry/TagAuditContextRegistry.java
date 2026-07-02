package dev.marie.MariesLib.tagaudit.registry;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.registry.AbstractRegistry;
import dev.marie.MariesLib.tagaudit.model.TagAuditContext;

/**
 * Registry of TagAuditContext implementations, keyed by the consuming mod's
 * modId, so {@code /marieslib audit_tags <modid>} can look up which mod's
 * context to scan against. Mirrors how ExportResolverRegistry/ConfigValidator
 * are looked up by id.
 */
@ApiStatus.Internal
public final class TagAuditContextRegistry {

    private static final class Core extends AbstractRegistry<String, TagAuditContext> {
        Core() {
            super("TagAuditContextRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private TagAuditContextRegistry() {}

    public static void register(String modId, TagAuditContext context) {
        if (modId == null || modId.isBlank()) {
            throw new IllegalArgumentException("TagAuditContextRegistry: modId must not be blank");
        }
        if (context == null) {
            throw new IllegalArgumentException("TagAuditContextRegistry: context must not be null");
        }
        if (INSTANCE.contains(modId)) {
            throw new IllegalArgumentException("TagAuditContext already registered for modId: " + modId);
        }
        INSTANCE.register(modId, context);
    }

    public static TagAuditContext get(String modId) {
        return INSTANCE.get(modId);
    }
}
