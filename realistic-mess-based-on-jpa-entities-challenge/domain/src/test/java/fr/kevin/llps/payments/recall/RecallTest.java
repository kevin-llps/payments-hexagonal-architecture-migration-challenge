package fr.kevin.llps.payments.recall;

import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.DomainInvariantViolationException;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecallTest {

    private static final PaymentId ID = PaymentId.parse("00000000-0000-0000-0000-000000000905");
    private static final PaymentId ORIGINAL_ID = PaymentId.parse("00000000-0000-0000-0000-000000000904");
    private static final LocalDateTime INITIATED_AT = LocalDateTime.of(2026, 10, 17, 12, 0);
    private static final LocalDate DEADLINE = LocalDate.of(2026, 10, 19);
    private static final AuditTimestamps AUDIT = AuditTimestamps.recordedAt(LocalDateTime.of(2026, 10, 25, 9, 0));

    @Test
    void record_should_build_the_recall_referencing_its_original_payment() {
        // GIVEN / WHEN
        Recall recall = Recall.record(ID, "RCL-1", "R-COR", ORIGINAL_ID, "OUTBOUND", "AC04",
                new BigDecimal("300.00"), "EUR", INITIATED_AT, DEADLINE, "INITIATED", AUDIT);

        // THEN
        assertThat(recall).isEqualTo(new Recall(ID, "RCL-1", "R-COR", new OriginalPaymentRef(ORIGINAL_ID, "OUTBOUND"),
                "AC04", new BigDecimal("300.00"), "EUR", INITIATED_AT, DEADLINE, "INITIATED", AUDIT));
    }

    @Test
    void record_should_reject_a_recall_without_original_payment_id() {
        // GIVEN / WHEN / THEN
        assertThatThrownBy(() -> Recall.record(ID, "RCL-1", "R-COR", null, "OUTBOUND", "AC04",
                new BigDecimal("300.00"), "EUR", INITIATED_AT, DEADLINE, "INITIATED", AUDIT))
                .isInstanceOf(DomainInvariantViolationException.class)
                .hasMessage("A recall must reference its original payment");
    }
}
