package dev.marie.MariesLib.tagaudit.registry;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.registry.AbstractRegistry;
import dev.marie.MariesLib.tagaudit.rule.TagRule;

import java.util.List;
import java.util.Map;

/**
 * Registry of {@link TagRule} implementations. Reusable infrastructure —
 * register-once semantics, mirroring {@code ExportResolverRegistry}'s shape.
 * Rules are behavior (code), not data, so there is no config/datapack/KubeJS
 * override stack for registration itself — a rule's own internal parameters
 * (thresholds, weights) are each rule's own concern, not this registry's.
 */
@ApiStatus.Internal
public final class TagRuleRegistry {

    private static final class Core extends AbstractRegistry<String, TagRule> {
        Core() {
            super("TagRuleRegistry");
        }
    }

    private static final Core INSTANCE = new Core();

    private TagRuleRegistry() {}

    public static void register(TagRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("TagRuleRegistry: rule must not be null");
        }
        String ruleId = rule.ruleId();
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("TagRuleRegistry: rule.ruleId() must not be blank");
        }
        if (INSTANCE.contains(ruleId)) {
            throw new IllegalArgumentException("TagRule already registered: " + ruleId);
        }
        INSTANCE.register(ruleId, rule);
    }

    public static TagRule get(String ruleId) {
        return INSTANCE.get(ruleId);
    }

    public static List<TagRule> getAll() {
        return INSTANCE.values();
    }

    public static Map<String, TagRule> entries() {
        return INSTANCE.entries();
    }
}
