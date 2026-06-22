package dev.marie.MariesLib.tagaudit.model;

import dev.marie.MariesLib.api.ApiStatus;

/**
 * A proposed correction for a {@link TagIssue}, kept as a separate type so a
 * fix can be reviewed/approved independently of the issue it addresses, and
 * so a rule (or a different rule) can propose more than one candidate fix for
 * the same issue if it's genuinely ambiguous.
 *
 * @param issueId       the {@link TagIssue#issueId()} this suggestion addresses
 * @param suggestedTag  the tag/category this suggestion proposes moving the item to
 * @param ruleId        which rule produced this suggestion
 * @param confidence    0.0-1.0, confidence in this specific suggestion
 * @param reason        a short, human-readable explanation suitable for a report
 */
@ApiStatus.Stable
public record TagFixSuggestion(
        String issueId,
        String suggestedTag,
        String ruleId,
        float confidence,
        String reason
) {
    public TagFixSuggestion {
        if (issueId == null || issueId.isBlank()) {
            throw new IllegalArgumentException("TagFixSuggestion: issueId must not be blank");
        }
        if (suggestedTag == null || suggestedTag.isBlank()) {
            throw new IllegalArgumentException("TagFixSuggestion: suggestedTag must not be blank");
        }
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("TagFixSuggestion: ruleId must not be blank");
        }
        if (reason == null) {
            reason = "";
        }
        confidence = Math.max(0f, Math.min(1f, confidence));
    }
}
