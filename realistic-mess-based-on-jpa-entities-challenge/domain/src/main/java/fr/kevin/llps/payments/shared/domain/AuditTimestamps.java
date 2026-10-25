package fr.kevin.llps.payments.shared.domain;

import java.time.LocalDateTime;

/**
 * Technical audit dates of a stored record. Both are reset every time the record is ingested.
 */
public record AuditTimestamps(LocalDateTime createdAt, LocalDateTime updatedAt) {

    public static AuditTimestamps recordedAt(LocalDateTime now) {
        return new AuditTimestamps(now, now);
    }
}
