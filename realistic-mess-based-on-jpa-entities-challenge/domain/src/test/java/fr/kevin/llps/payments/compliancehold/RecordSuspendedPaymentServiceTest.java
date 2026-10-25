package fr.kevin.llps.payments.compliancehold;

import fr.kevin.llps.payments.compliancehold.port.in.RecordSuspendedPaymentCommand;
import fr.kevin.llps.payments.compliancehold.port.out.SuspendedPaymentRecordRepository;
import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.MalformedValueException;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RecordSuspendedPaymentServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-25T09:15:30.123456789Z"), ZoneOffset.UTC);

    @Mock
    private SuspendedPaymentRecordRepository repository;

    private RecordSuspendedPaymentService service;

    @BeforeEach
    void setUp() {
        service = new RecordSuspendedPaymentService(repository, CLOCK);
    }

    @Test
    void record_should_upsert_the_payment_stamped_now_with_millisecond_precision() {
        // GIVEN
        RecordSuspendedPaymentCommand command = command("00000000-0000-0000-0000-000000000902");

        // WHEN
        service.record(command);

        // THEN
        LocalDateTime now = LocalDateTime.of(2026, 10, 25, 9, 15, 30, 123_000_000);
        verify(repository).upsert(new SuspendedPayment(PaymentId.parse("00000000-0000-0000-0000-000000000902"),
                Money.of(new BigDecimal("5000.00"), "EUR"), "DE89", "ACME", "FR76", "Dupont", "SANCTIONS_SCREENING",
                "CASE-1", "PENDING", LocalDateTime.of(2026, 10, 17, 11, 0), null, new AuditTimestamps(now, now)));
    }

    // Regression check: BR-23 comes from PaymentId.parse; the service must not upsert when the id is invalid.
    @ParameterizedTest(name = "[{index}] id=\"{0}\"")
    @ValueSource(strings = {"", "not-a-uuid"})
    void record_should_reject_an_invalid_id_without_upserting(String id) {
        assertThatThrownBy(() -> service.record(command(id))).isInstanceOf(MalformedValueException.class);
        verifyNoInteractions(repository);
    }

    private static RecordSuspendedPaymentCommand command(String id) {
        return new RecordSuspendedPaymentCommand(id, new BigDecimal("5000.00"), "EUR", "DE89", "ACME", "FR76", "Dupont",
                "SANCTIONS_SCREENING", "CASE-1", "PENDING", LocalDateTime.of(2026, 10, 17, 11, 0), null);
    }
}
