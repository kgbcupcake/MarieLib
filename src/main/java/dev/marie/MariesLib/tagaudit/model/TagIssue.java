package dev.marie.MariesLib.tagaudit.model;

import dev.marie.MariesLib.api.ApiStatus;
import net.minecraft.resources.ResourceLocation;

/**
 * A single issue detected by a {@link dev.marie.MariesLib.tagaudit.rule.TagRule}.
 *
 * @param issueId    stable identifier for this specific issue instance
 * @param itemId     the item that has the issue
 * @param currentTag the tag category the item is currently assigned to
 * @param ruleId     the rule that produced this issue
 * @param confidence rule-specific confidence score (0–1)
 * @param severity   how serious the issue is
 * @param message    human-readable description
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
) {}
