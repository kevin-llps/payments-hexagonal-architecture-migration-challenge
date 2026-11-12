package fr.kevin.llps.payments.domain.suspended;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.suspended.command.RecordSuspendedPaymentCommand;
import fr.kevin.llps.payments.domain.suspended.port.out.SaveSuspendedPayment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class RecordSuspendedPaymentServiceTest {

    private static final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneId.of("Europe/Paris"));

    @Mock
    private SaveSuspendedPayment saveSuspendedPayment;

    private RecordSuspendedPaymentService recordSuspendedPaymentService;

    @BeforeEach
    void createService() {
        recordSuspendedPaymentService = new RecordSuspendedPaymentService(saveSuspendedPayment, fixedClock);
    }

    @Test
    void should_save_the_event_as_a_full_payment_with_consumer_time_audit_timestamps() {
        // GIVEN
        PaymentId id = PaymentId.of(UUID.fromString("00000000-0000-0000-0000-000000000902"));
        RecordSuspendedPaymentCommand command = new RecordSuspendedPaymentCommand(id, new BigDecimal("5000.00"), "EUR",
                "DE89", "ACME", "FR76", "Dupont", "SANCTIONS_SCREENING", "CASE-1", "PENDING",
                LocalDateTime.of(2026, 10, 17, 11, 0), null);
        ArgumentCaptor<SuspendedPayment> saved = ArgumentCaptor.forClass(SuspendedPayment.class);

        // WHEN
        recordSuspendedPaymentService.record(command);

        // THEN
        verify(saveSuspendedPayment).save(saved.capture());
        verifyNoMoreInteractions(saveSuspendedPayment);
        LocalDateTime consumerTime = LocalDateTime.of(2026, 11, 12, 11, 0);
        assertThat(saved.getValue()).isEqualTo(SuspendedPayment.builder(id)
                .money(Money.of(new BigDecimal("5000.00"), "EUR"))
                .debtorIban("DE89")
                .debtorName("ACME")
                .creditorIban("FR76")
                .creditorName("Dupont")
                .holdReason("SANCTIONS_SCREENING")
                .complianceCaseRef("CASE-1")
                .reviewStatus("PENDING")
                .heldAt(LocalDateTime.of(2026, 10, 17, 11, 0))
                .createdAt(consumerTime)
                .updatedAt(consumerTime)
                .build());
    }
}
