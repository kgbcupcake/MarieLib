package dev.marie.MariesLib.tagaudit;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.tagaudit.rule.TagRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ApiStatus.Internal
public final class TagRuleRegistry {

    private static final List<TagRule> RULES = new ArrayList<>();

    private TagRuleRegistry() {}

    public static void register(TagRule rule) {
        RULES.add(rule);
    }

    public static List<TagRule> getAll() {
        return Collections.unmodifiableList(RULES);
    }
}
