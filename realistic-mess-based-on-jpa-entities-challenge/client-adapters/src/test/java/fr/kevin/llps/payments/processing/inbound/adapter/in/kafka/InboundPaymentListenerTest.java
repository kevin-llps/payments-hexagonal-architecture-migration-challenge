package fr.kevin.llps.payments.processing.inbound.adapter.in.kafka;

import fr.kevin.llps.payments.processing.inbound.port.in.RecordInboundPaymentCommand;
import fr.kevin.llps.payments.processing.inbound.port.in.RecordInboundPaymentUseCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link InboundPaymentListener} message handling; carried over from the legacy KafkaListenerUnitTest.
 */
@ExtendWith(MockitoExtension.class)
class InboundPaymentListenerTest {

    @Mock
    private RecordInboundPaymentUseCase recordInboundPayment;
    @InjectMocks
    private InboundPaymentListener inboundListener;

    @Test
    void inbound_onMessage_nullSettlementDate_staysNull() {
        inboundListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000903","creditTransferRef":"CTR-1","amount":10.00,
                 "currency":"EUR","creditorIban":"DE89","creditorName":"ACME","originBic":"BNPAFRPP",
                 "valueDate":"2026-10-17","settlementDate":null,"status":"RECEIVED"}
                """);

        RecordInboundPaymentCommand payment = captureInbound();
        assertThat(payment.creditTransferRef()).isEqualTo("CTR-1");
        assertThat(payment.valueDate()).isEqualTo(LocalDate.of(2026, 10, 17));
        assertThat(payment.settlementDate()).isNull();
    }

    @Test
    void inbound_onMessage_dateTimeValues_areTruncatedToDate() {
        inboundListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000903","amount":10.00,
                 "valueDate":"2026-10-17T23:59:59Z","settlementDate":"2026-10-18 01:00:00"}
                """);

        RecordInboundPaymentCommand payment = captureInbound();
        assertThat(payment.valueDate()).isEqualTo(LocalDate.of(2026, 10, 17));
        assertThat(payment.settlementDate()).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    @Test
    void inbound_onMessage_malformedJson_swallowed() {
        assertThatCode(() -> inboundListener.onMessage("{not valid")).doesNotThrowAnyException();
        verify(recordInboundPayment, never()).record(any());
    }

    private RecordInboundPaymentCommand captureInbound() {
        ArgumentCaptor<RecordInboundPaymentCommand> captor = ArgumentCaptor.forClass(RecordInboundPaymentCommand.class);
        verify(recordInboundPayment).record(captor.capture());
        return captor.getValue();
    }

    @Test
    void listener_consumesItsTopicWithDedicatedConsumerGroup() throws Exception {
        KafkaListener annotation = InboundPaymentListener.class.getMethod("onMessage", String.class)
                .getAnnotation(KafkaListener.class);

        assertThat(annotation.topics()).containsExactly("pay.inbound");
        assertThat(annotation.groupId()).isEqualTo("jpa-inbound-group");
    }
}
