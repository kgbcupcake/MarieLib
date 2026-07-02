package dev.marie.framework.tagaudit.model;

import dev.marie.framework.api.ApiStatus;

/**
 * A suggested fix for a {@link TagIssue}, produced by a
 * {@link dev.marie.framework.tagaudit.rule.TagRule}.
 *
 * @param issueId      the issue this suggestion addresses
 * @param suggestedTag the tag category the item should be moved to
 * @param ruleId       the rule that produced this suggestion
 * @param confidence   confidence in the suggested fix (0–1)
 * @param message      human-readable rationale
 */
@ApiStatus.Stable
public record TagFixSuggestion(
        String issueId,
        String suggestedTag,
        String ruleId,
        float confidence,
        String reason
) {}
