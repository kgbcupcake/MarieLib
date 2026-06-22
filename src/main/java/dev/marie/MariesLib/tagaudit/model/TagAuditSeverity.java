package dev.marie.MariesLib.tagaudit.model;

import dev.marie.MariesLib.api.ApiStatus;

/**
 * Severity of a TagIssue, used for sorting/filtering in reports. Reusable
 * infrastructure — the consuming mod decides what severity to assign per
 * issue; MarieLib has no opinion on what makes something LOW vs HIGH.
 */
@ApiStatus.Stable
public enum TagAuditSeverity {
    LOW,
    MEDIUM,
    HIGH
}
