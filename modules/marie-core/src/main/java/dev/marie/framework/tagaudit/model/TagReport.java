package dev.marie.framework.tagaudit.model;

import dev.marie.framework.api.ApiStatus;

import java.util.List;

@ApiStatus.Stable
public record TagReport(
        String timestamp,
        List<String> rulesRun,
        List<TagIssue> issues,
        List<TagFixSuggestion> suggestions
) {}
