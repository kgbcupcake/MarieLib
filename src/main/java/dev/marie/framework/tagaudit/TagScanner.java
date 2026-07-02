package dev.marie.framework.tagaudit;

import dev.marie.framework.api.ApiStatus;
import dev.marie.framework.tagaudit.model.TagAuditContext;
import dev.marie.framework.tagaudit.model.TagFixSuggestion;
import dev.marie.framework.tagaudit.model.TagIssue;
import dev.marie.framework.tagaudit.model.TagReport;
import dev.marie.framework.tagaudit.rule.TagRule;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@ApiStatus.Stable
public final class TagScanner {

    private TagScanner() {}

    public static TagReport scan(TagAuditContext context) {
        List<TagRule> rules = TagRuleRegistry.getAll();
        List<String> rulesRun = new ArrayList<>();
        List<TagIssue> allIssues = new ArrayList<>();

        for (TagRule rule : rules) {
            rulesRun.add(rule.ruleId());
            List<TagIssue> issues = rule.findIssues(context);
            if (issues != null) {
                allIssues.addAll(issues);
            }
        }

        List<TagFixSuggestion> allSuggestions = new ArrayList<>();
        for (TagRule rule : rules) {
            List<TagFixSuggestion> suggestions = rule.suggestFixes(context, allIssues);
            if (suggestions != null) {
                allSuggestions.addAll(suggestions);
            }
        }

        String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());

        return new TagReport(timestamp, rulesRun, allIssues, allSuggestions);
    }
}
