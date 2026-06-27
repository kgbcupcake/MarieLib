package dev.marie.MariesLib.tagaudit.rule;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.tagaudit.model.TagAuditContext;
import dev.marie.MariesLib.tagaudit.model.TagFixSuggestion;
import dev.marie.MariesLib.tagaudit.model.TagIssue;

import java.util.List;

/**
 * A rule that inspects a {@link TagAuditContext} for issues and produces
 * fix suggestions. Implement and register via the TagAuditContext pipeline.
 */
@ApiStatus.Stable
public interface TagRule {

    /** Stable identifier for this rule. */
    String ruleId();

    /** Returns all issues found in the given context. Must not throw. */
    List<TagIssue> findIssues(TagAuditContext context);

    /** Suggests fixes for the given issues. Must not throw. */
    List<TagFixSuggestion> suggestFixes(TagAuditContext context, List<TagIssue> issues);
}
