package dev.marie.MariesLib.tagaudit.model;

import dev.marie.MariesLib.api.ApiStatus;

import java.util.List;

/**
 * The collected output of one {@link dev.marie.MariesLib.tagaudit.TagScanner}
 * run — every issue found across all registered rules, plus every suggested
 * fix produced for those issues. Reusable infrastructure — purely a data
 * carrier, no behavior.
 *
 * @param issues       every issue found, across all rules that ran
 * @param suggestions  every fix suggestion produced, across all rules that ran
 * @param rulesRun     the ruleIds that actually executed during this scan
 * @param timestamp    epoch millis when this report was generated
 */
@ApiStatus.Stable
public record TagReport(
        List<TagIssue> issues,
        List<TagFixSuggestion> suggestions,
        List<String> rulesRun,
        long timestamp
) {
    public TagReport {
        issues = issues == null ? List.of() : List.copyOf(issues);
        suggestions = suggestions == null ? List.of() : List.copyOf(suggestions);
        rulesRun = rulesRun == null ? List.of() : List.copyOf(rulesRun);
    }
}
