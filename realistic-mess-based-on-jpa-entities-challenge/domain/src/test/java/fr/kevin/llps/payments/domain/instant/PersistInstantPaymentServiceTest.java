package fr.kevin.llps.payments.domain.instant;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.domain.instant.command.PersistInstantPaymentCommand;
import fr.kevin.llps.payments.domain.instant.port.out.SaveInstantPayment;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class PersistInstantPaymentServiceTest {

    private static final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneId.of("Europe/Paris"));

    @Mock
    private SaveInstantPayment saveInstantPayment;

    private PersistInstantPaymentService persistInstantPaymentService;

    @BeforeEach
    void createService() {
        persistInstantPaymentService = new PersistInstantPaymentService(saveInstantPayment, fixedClock);
    }

    @Test
    void should_save_the_event_as_a_full_payment_with_consumer_time_audit_timestamps() {
        // GIVEN
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000901");
        PersistInstantPaymentCommand command = new PersistInstantPaymentCommand(id, "E2E-1", "uetr-1",
                new BigDecimal("42.00"), "EUR", "DE89", "ACME", "FR76", "Dupont", LocalDateTime.of(2026, 10, 17, 10, 0),
                "COMPLETED", "SCT INST");
        ArgumentCaptor<InstantPayment> saved = ArgumentCaptor.forClass(InstantPayment.class);

        // WHEN
        persistInstantPaymentService.persist(command);

        // THEN
        verify(saveInstantPayment).save(saved.capture());
        verifyNoMoreInteractions(saveInstantPayment);
        LocalDateTime consumerTime = LocalDateTime.of(2026, 11, 12, 11, 0);
        assertThat(saved.getValue()).isEqualTo(InstantPayment.builder(id)
                .endToEndId("E2E-1")
                .uetr("uetr-1")
                .money(Money.of(new BigDecimal("42.00"), "EUR"))
                .debtorIban("DE89")
                .debtorName("ACME")
                .creditorIban("FR76")
                .creditorName("Dupont")
                .executionDatetime(LocalDateTime.of(2026, 10, 17, 10, 0))
                .status("COMPLETED")
                .scheme("SCT INST")
                .createdAt(consumerTime)
                .updatedAt(consumerTime)
                .build());
    }
}
