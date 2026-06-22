package dev.marie.MariesLib.tagaudit;

import dev.marie.MariesLib.api.ApiStatus;
import dev.marie.MariesLib.core.MariesLib;
import dev.marie.MariesLib.tagaudit.model.TagAuditContext;
import dev.marie.MariesLib.tagaudit.model.TagFixSuggestion;
import dev.marie.MariesLib.tagaudit.model.TagIssue;
import dev.marie.MariesLib.tagaudit.model.TagReport;
import dev.marie.MariesLib.tagaudit.registry.TagRuleRegistry;
import dev.marie.MariesLib.tagaudit.rule.TagRule;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs every registered {@link TagRule} against a supplied {@link TagAuditContext}
 * and produces a {@link TagReport}. Reusable infrastructure — has no idea what a
 * "tag" represents; the consuming mod's TagAuditContext implementation and TagRule
 * implementations carry all the actual domain meaning.
 */
@ApiStatus.Stable
public final class TagScanner {

    private TagScanner() {}

    /**
     * Runs all registered rules against the given context.
     *
     * @param context the consuming mod's implementation of TagAuditContext,
     *                giving rules access to its real tag/namespace/inference data
     * @return a TagReport with every issue and suggestion found; never null
     */
    public static TagReport scan(TagAuditContext context) {
        if (context == null) {
            throw new IllegalArgumentException("TagScanner.scan: context must not be null");
        }
        List<TagRule> rules = TagRuleRegistry.getAll();
        List<TagIssue> allIssues = new ArrayList<>();
        List<String> rulesRun = new ArrayList<>();

        for (TagRule rule : rules) {
            try {
                List<TagIssue> issues = rule.findIssues(context);
                if (issues != null) {
                    allIssues.addAll(issues);
                }
                rulesRun.add(rule.ruleId());
            } catch (Exception ex) {
                MariesLib.LOGGER.warn("[TagScanner] Rule '{}' failed during findIssues: {}", rule.ruleId(), ex.toString());
                MariesLib.LOGGER.debug("[TagScanner] Failure details", ex);
            }
        }

        List<TagFixSuggestion> allSuggestions = new ArrayList<>();
        List<TagIssue> immutableIssues = List.copyOf(allIssues);
        for (TagRule rule : rules) {
            try {
                List<TagFixSuggestion> suggestions = rule.suggestFixes(context, immutableIssues);
                if (suggestions != null) {
                    allSuggestions.addAll(suggestions);
                }
            } catch (Exception ex) {
                MariesLib.LOGGER.warn("[TagScanner] Rule '{}' failed during suggestFixes: {}", rule.ruleId(), ex.toString());
                MariesLib.LOGGER.debug("[TagScanner] Failure details", ex);
            }
        }

        MariesLib.LOGGER.info(
                "[TagScanner] Scan complete: {} rule(s) run, {} issue(s), {} suggestion(s)",
                rulesRun.size(), allIssues.size(), allSuggestions.size());

        return new TagReport(allIssues, allSuggestions, rulesRun, System.currentTimeMillis());
    }
}
