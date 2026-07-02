package dev.marie.framework.tagaudit.model;

import dev.marie.framework.api.ApiStatus;

/** Severity level of a {@link TagIssue} found during a tag audit. */
@ApiStatus.Stable
public enum TagAuditSeverity {
    LOW,
    MEDIUM,
    HIGH
}
