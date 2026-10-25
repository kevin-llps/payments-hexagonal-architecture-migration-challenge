package fr.kevin.llps.payments.shared.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class AuditTimestampsTest {

    @Test
    void recorded_at_should_set_created_at_and_updated_at_to_now() {
        // GIVEN
        LocalDateTime now = LocalDateTime.of(2026, 10, 25, 14, 30, 5);

        // WHEN
        AuditTimestamps audit = AuditTimestamps.recordedAt(now);

        // THEN
        assertThat(audit).isEqualTo(new AuditTimestamps(now, now));
    }
}
