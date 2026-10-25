package fr.kevin.llps.payments.compliancehold.adapter.in.kafka;

import fr.kevin.llps.payments.compliancehold.port.in.RecordSuspendedPaymentCommand;
import fr.kevin.llps.payments.compliancehold.port.in.RecordSuspendedPaymentUseCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link SuspendedPaymentListener} message handling; carried over from the legacy KafkaListenerUnitTest.
 */
@ExtendWith(MockitoExtension.class)
class SuspendedPaymentListenerTest {

    @Mock
    private RecordSuspendedPaymentUseCase recordSuspendedPayment;
    @InjectMocks
    private SuspendedPaymentListener suspendedListener;

    @Test
    void suspended_onMessage_nullReleasedAt_staysNull() {
        suspendedListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000902","amount":5000.00,"currency":"EUR",
                 "debtorIban":"DE89","debtorName":"ACME","creditorIban":"FR76","creditorName":"Dupont",
                 "holdReason":"SANCTIONS_SCREENING","complianceCaseRef":"CASE-1","reviewStatus":"PENDING",
                 "heldAt":"2026-10-17 11:00:00","releasedAt":null}
                """);

        ArgumentCaptor<RecordSuspendedPaymentCommand> captor = ArgumentCaptor.forClass(RecordSuspendedPaymentCommand.class);
        verify(recordSuspendedPayment).record(captor.capture());
        RecordSuspendedPaymentCommand payment = captor.getValue();
        assertThat(payment.complianceCaseRef()).isEqualTo("CASE-1");
        assertThat(payment.heldAt()).isEqualTo(LocalDateTime.of(2026, 10, 17, 11, 0, 0));
        assertThat(payment.releasedAt()).isNull();
    }

    @Test
    void suspended_onMessage_malformedJson_swallowed() {
        assertThatCode(() -> suspendedListener.onMessage("{bad")).doesNotThrowAnyException();
        verify(recordSuspendedPayment, never()).record(any());
    }

    @Test
    void listener_consumesItsTopicWithDedicatedConsumerGroup() throws Exception {
        KafkaListener annotation = SuspendedPaymentListener.class.getMethod("onMessage", String.class)
                .getAnnotation(KafkaListener.class);

        assertThat(annotation.topics()).containsExactly("pay.suspended");
        assertThat(annotation.groupId()).isEqualTo("jpa-suspended-group");
    }
}
