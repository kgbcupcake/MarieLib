package dev.marie.MariesLib.tagaudit.model;

import dev.marie.MariesLib.api.ApiStatus;

import java.util.List;

@ApiStatus.Stable
public record TagReport(
        String timestamp,
        List<String> rulesRun,
        List<TagIssue> issues,
        List<TagFixSuggestion> suggestions
) {}
