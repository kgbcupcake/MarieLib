package dev.marie.MariesLib.tagaudit.model;

import dev.marie.MariesLib.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

/**
 * A single detected tag-audit problem, produced by a {@link dev.marie.MariesLib.tagaudit.rule.TagRule}.
 * Carries no suggested fix — see {@link TagFixSuggestion} for that, kept as a
 * separate type since an issue can be flagged without a confident correction
 * (e.g. "this item is tagged in two categories" may not have one obvious fix).
 *
 * <p>Reusable infrastructure — the consuming mod defines what "currentTag"
 * means (nutrient category, EMC tier, anything keyed by a tag file the
 * consuming mod owns).</p>
 *
 * @param issueId     a unique id for this issue instance, used to reference
 *                     it from a {@link TagFixSuggestion}
 * @param itemId      the item this issue is about
 * @param currentTag  the tag/category the item currently has, as the
 *                     consuming mod identifies it
 * @param ruleId      which rule produced this issue, for traceability/filtering
 * @param confidence  0.0-1.0, the rule's confidence that this is a real problem
 * @param severity    see {@link TagAuditSeverity}
 * @param reason      a short, human-readable explanation suitable for a report
 */
@ApiStatus.Stable
public record TagIssue(
        String issueId,
        ResourceLocation itemId,
        String currentTag,
        String ruleId,
        float confidence,
        TagAuditSeverity severity,
        String reason
) {
    public TagIssue {
        if (issueId == null || issueId.isBlank()) {
            throw new IllegalArgumentException("TagIssue: issueId must not be blank");
        }
        if (itemId == null) {
            throw new IllegalArgumentException("TagIssue: itemId must not be null");
        }
        if (currentTag == null || currentTag.isBlank()) {
            throw new IllegalArgumentException("TagIssue: currentTag must not be blank");
        }
        if (ruleId == null || ruleId.isBlank()) {
            throw new IllegalArgumentException("TagIssue: ruleId must not be blank");
        }
        if (reason == null) {
            reason = "";
        }
        confidence = Math.max(0f, Math.min(1f, confidence));
    }
}
