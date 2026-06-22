package dev.marie.MariesLib.tagaudit.rule;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.tagaudit.model.TagAuditContext;
import dev.marie.MariesLib.tagaudit.model.TagFixSuggestion;
import dev.marie.MariesLib.tagaudit.model.TagIssue;

import java.util.List;

/**
 * A consuming-mod-provided rule that inspects tag data via a {@link TagAuditContext}
 * and produces issues and/or fix suggestions. Reusable infrastructure — MarieLib's
 * {@link dev.marie.MariesLib.tagaudit.TagScanner} runs registered rules and collects
 * their output; it has no opinion on what makes a tag "wrong."
 *
 * <p>Register via {@code MarieAPI.registerTagRule} (added in a later prompt).</p>
 */
@ApiStatus.Stable
public interface TagRule {

    /**
     * A unique identifier for this rule (e.g. {@code "nourished_namespace_bias"}),
     * used in issues'/suggestions' {@code ruleId} field and for filtering/enabling
     * individual rules via config.
     */
    String ruleId();

    /**
     * Runs this rule against the given context and returns any issues found.
     * Should return an empty list, not null, if nothing is found.
     */
    List<TagIssue> findIssues(TagAuditContext context);

    /**
     * Optionally proposes fixes for issues this rule (or another rule) found.
     * Default implementation returns an empty list — a rule may only detect
     * problems without proposing fixes, which is fine; fix-proposing is opt-in.
     *
     * @param context the same context passed to {@link #findIssues}
     * @param issues  all issues found during this scan run (from every rule,
     *                not just this one), so a rule can propose fixes for
     *                issues other rules detected if it has relevant logic to do so
     */
    default List<TagFixSuggestion> suggestFixes(TagAuditContext context, List<TagIssue> issues) {
        return List.of();
    }
}
