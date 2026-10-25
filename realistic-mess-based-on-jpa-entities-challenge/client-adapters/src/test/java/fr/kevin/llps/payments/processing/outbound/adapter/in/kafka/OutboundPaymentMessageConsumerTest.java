package fr.kevin.llps.payments.processing.outbound.adapter.in.kafka;

import fr.kevin.llps.payments.processing.outbound.OutboundPayment;
import fr.kevin.llps.payments.processing.outbound.RecordOutboundPaymentService;
import fr.kevin.llps.payments.processing.outbound.port.out.OutboundPaymentRecordRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;

import java.time.Clock;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link OutboundPaymentMessageConsumer} message handling; carried over from the legacy KafkaListenerUnitTest.
 */
@ExtendWith(MockitoExtension.class)
class OutboundPaymentMessageConsumerTest {

    @Mock
    private OutboundPaymentRecordRepository outboundRepository;

    // real use case: the persistence failure is raised by the repository, as em.merge did
    private OutboundPaymentMessageConsumer outboundConsumer;

    @BeforeEach
    void wireOutboundConsumer() {
        outboundConsumer = new OutboundPaymentMessageConsumer(
                new RecordOutboundPaymentService(outboundRepository, Clock.systemDefaultZone()));
    }

    @Test
    void outbound_onMessage_persistsRow() {
        outboundConsumer.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000904","instructionId":"OUT-1","amount":300.00,
                 "currency":"EUR","debtorIban":"DE89","debtorName":"ACME","destinationBic":"ABNANL2A",
                 "requestedExecutionDate":"2026-10-18","chargesType":"SHA","status":"SENT"}
                """);

        ArgumentCaptor<OutboundPayment> captor = ArgumentCaptor.forClass(OutboundPayment.class);
        verify(outboundRepository).upsert(captor.capture());
        OutboundPayment payment = captor.getValue();
        assertThat(payment.instructionId()).isEqualTo("OUT-1");
        assertThat(payment.amount().amount()).isEqualByComparingTo("300.00");
        assertThat(payment.requestedExecutionDate()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(payment.chargesType().code()).isEqualTo("SHA");
    }

    @Test
    void outbound_onMessage_malformedJson_swallowed() {
        assertThatCode(() -> outboundConsumer.onMessage("nope")).doesNotThrowAnyException();
        verify(outboundRepository, never()).upsert(any());
    }

    @Test
    void outbound_onMessage_persistenceFailure_swallowed() {
        doThrow(new RuntimeException("db down")).when(outboundRepository).upsert(any());

        assertThatCode(() -> outboundConsumer.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000904","amount":300.00,"chargesType":"SHA"}
                """)).doesNotThrowAnyException();
    }

    @Test
    void listener_consumesItsTopicWithDedicatedConsumerGroup() throws Exception {
        KafkaListener annotation = OutboundPaymentMessageConsumer.class.getMethod("onMessage", String.class)
                .getAnnotation(KafkaListener.class);

        assertThat(annotation.topics()).containsExactly("pay.outbound");
        assertThat(annotation.groupId()).isEqualTo("jpa-outbound-group");
    }
}
