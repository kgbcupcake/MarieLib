package dev.marie.MariesLib.tagaudit.model;

import dev.marie.MariesLib.api.ApiStatus;

/** Severity level of a {@link TagIssue} found during a tag audit. */
@ApiStatus.Stable
public enum TagAuditSeverity {
    LOW,
    MEDIUM,
    HIGH
}
