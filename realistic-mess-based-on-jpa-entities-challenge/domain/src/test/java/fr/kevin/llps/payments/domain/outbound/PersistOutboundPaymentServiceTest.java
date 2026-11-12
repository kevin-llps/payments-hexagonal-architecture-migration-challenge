package fr.kevin.llps.payments.domain.outbound;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.domain.outbound.command.PersistOutboundPaymentCommand;
import fr.kevin.llps.payments.domain.outbound.port.out.SaveOutboundPayment;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class PersistOutboundPaymentServiceTest {

    private static final Clock fixedClock = Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneId.of("Europe/Paris"));

    @Mock
    private SaveOutboundPayment saveOutboundPayment;

    private PersistOutboundPaymentService persistOutboundPaymentService;

    @BeforeEach
    void createService() {
        persistOutboundPaymentService = new PersistOutboundPaymentService(saveOutboundPayment, fixedClock);
    }

    @Test
    void should_save_the_event_as_a_full_payment() {
        // GIVEN (data of KafkaListenerUnitTest.outbound_onMessage_persistsRow)
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000904");
        PersistOutboundPaymentCommand command = new PersistOutboundPaymentCommand(id, "OUT-1", new BigDecimal("300.00"),
                "EUR", "DE89", "ACME", "ABNANL2A", LocalDate.of(2026, 10, 18), "SHA", "SENT");
        ArgumentCaptor<OutboundPayment> saved = ArgumentCaptor.forClass(OutboundPayment.class);

        // WHEN
        persistOutboundPaymentService.persist(command);

        // THEN
        verify(saveOutboundPayment).save(saved.capture());
        verifyNoMoreInteractions(saveOutboundPayment);
        LocalDateTime consumerTime = LocalDateTime.of(2026, 11, 12, 11, 0);
        assertThat(saved.getValue()).isEqualTo(OutboundPayment.builder(id)
                .instructionId("OUT-1")
                .money(Money.of(new BigDecimal("300.00"), "EUR"))
                .debtorIban("DE89")
                .debtorName("ACME")
                .destinationBic("ABNANL2A")
                .requestedExecutionDate(LocalDate.of(2026, 10, 18))
                .chargesType("SHA")
                .status("SENT")
                .createdAt(consumerTime)
                .updatedAt(consumerTime)
                .build());
    }

    @Test
    void should_set_created_at_and_updated_at_to_consumer_time_on_every_message() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_set_created_at_and_updated_at_to_consumer_time_on_every_message;
        // the payload's createdAt is not part of the command, the fixed clock replaces the currentTimeMillis window)
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000911");
        PersistOutboundPaymentCommand command = new PersistOutboundPaymentCommand(id, "OUT-11", new BigDecimal("1.00"),
                "EUR", "", "", "", null, "SHA", "SENT");
        ArgumentCaptor<OutboundPayment> saved = ArgumentCaptor.forClass(OutboundPayment.class);

        // WHEN
        persistOutboundPaymentService.persist(command);

        // THEN
        verify(saveOutboundPayment).save(saved.capture());
        assertThat(saved.getValue())
                .returns(Optional.of(LocalDateTime.of(2026, 11, 12, 11, 0)), OutboundPayment::createdAt)
                .returns(Optional.of(LocalDateTime.of(2026, 11, 12, 11, 0)), OutboundPayment::updatedAt);
    }
}
