package fr.kevin.llps.payments.domain.inbound;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.domain.inbound.command.PersistInboundPaymentCommand;
import fr.kevin.llps.payments.domain.inbound.port.out.SaveInboundPayment;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class PersistInboundPaymentServiceTest {

    private static final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneId.of("Europe/Paris"));

    @Mock
    private SaveInboundPayment saveInboundPayment;

    private PersistInboundPaymentService persistInboundPaymentService;

    @BeforeEach
    void createService() {
        persistInboundPaymentService = new PersistInboundPaymentService(saveInboundPayment, fixedClock);
    }

    @Test
    void should_save_the_event_as_a_full_payment_with_consumer_time_audit_timestamps() {
        // GIVEN
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000903");
        PersistInboundPaymentCommand command = new PersistInboundPaymentCommand(id, "CTR-1", new BigDecimal("10.00"),
                "EUR", "DE89", "ACME", "BNPAFRPP", LocalDate.of(2026, 10, 17), null, "RECEIVED");
        ArgumentCaptor<InboundPayment> saved = ArgumentCaptor.forClass(InboundPayment.class);

        // WHEN
        persistInboundPaymentService.persist(command);

        // THEN
        verify(saveInboundPayment).save(saved.capture());
        verifyNoMoreInteractions(saveInboundPayment);
        LocalDateTime consumerTime = LocalDateTime.of(2026, 11, 12, 11, 0);
        assertThat(saved.getValue()).isEqualTo(InboundPayment.builder(id)
                .creditTransferRef("CTR-1")
                .money(Money.of(new BigDecimal("10.00"), "EUR"))
                .creditorIban("DE89")
                .creditorName("ACME")
                .originBic("BNPAFRPP")
                .valueDate(LocalDate.of(2026, 10, 17))
                .status("RECEIVED")
                .createdAt(consumerTime)
                .updatedAt(consumerTime)
                .build());
    }
}
