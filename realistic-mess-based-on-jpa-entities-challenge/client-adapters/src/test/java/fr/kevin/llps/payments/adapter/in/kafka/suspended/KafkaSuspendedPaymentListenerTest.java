package fr.kevin.llps.payments.adapter.in.kafka.suspended;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.annotation.Transactional;

import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.suspended.command.PersistSuspendedPaymentCommand;
import fr.kevin.llps.payments.domain.suspended.port.in.PersistSuspendedPayment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class KafkaSuspendedPaymentListenerTest {

    @Mock
    private PersistSuspendedPayment persistSuspendedPayment;

    @InjectMocks
    private KafkaSuspendedPaymentListener kafkaSuspendedPaymentListener;

    @Test
    void should_persist_the_event_and_keep_a_null_released_at() {
        // GIVEN (remapped from KafkaListenerUnitTest.suspended_onMessage_nullReleasedAt_staysNull)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000902","amount":5000.00,"currency":"EUR",
                 "debtorIban":"DE89","debtorName":"ACME","creditorIban":"FR76","creditorName":"Dupont",
                 "holdReason":"SANCTIONS_SCREENING","complianceCaseRef":"CASE-1","reviewStatus":"PENDING",
                 "heldAt":"2026-10-17 11:00:00","releasedAt":null}
                """;

        // WHEN
        kafkaSuspendedPaymentListener.onMessage(payload);

        // THEN
        PersistSuspendedPaymentCommand command = capturePersist();
        assertThat(command.id()).isEqualTo(PaymentId.parse("00000000-0000-0000-0000-000000000902"));
        assertThat(command.complianceCaseRef()).isEqualTo("CASE-1");
        assertThat(command.heldAt()).isEqualTo(LocalDateTime.of(2026, 10, 17, 11, 0));
        assertThat(command.releasedAt()).isNull();
    }

    @Test
    void should_swallow_a_malformed_json_event_without_persisting_it() {
        // GIVEN (remapped from KafkaListenerUnitTest.suspended_onMessage_malformedJson_swallowed)
        String malformed = "{bad";

        // WHEN / THEN
        assertThatCode(() -> kafkaSuspendedPaymentListener.onMessage(malformed)).doesNotThrowAnyException();
        verify(persistSuspendedPayment, never()).persist(any());
    }

    @Test
    void should_map_missing_fields_to_empty_strings_and_json_nulls_to_null_literal() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_map_missing_suspended_fields_to_empty_strings_and_json_nulls_to_null_literal)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000917","holdReason":null,"amount":null}
                """;

        // WHEN
        kafkaSuspendedPaymentListener.onMessage(payload);

        // THEN
        PersistSuspendedPaymentCommand command = capturePersist();
        assertThat(command.holdReason()).isEqualTo("null");
        assertThat(command.currency()).isEmpty();
        assertThat(command.complianceCaseRef()).isEmpty();
        assertThat(command.reviewStatus()).isEmpty();
        assertThat(command.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(command.heldAt()).isNull();
        assertThat(command.releasedAt()).isNull();
    }

    @Test
    void should_parse_held_at_iso_utc_value_without_time_zone_conversion() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_parse_suspended_held_at_iso_utc_value_without_time_zone_conversion)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000923","heldAt":"2026-10-17T11:00:00Z","releasedAt":"null"}
                """;

        // WHEN
        kafkaSuspendedPaymentListener.onMessage(payload);

        // THEN
        PersistSuspendedPaymentCommand command = capturePersist();
        assertThat(command.heldAt()).isEqualTo(LocalDateTime.of(2026, 10, 17, 11, 0));
        assertThat(command.releasedAt()).isNull();
    }

    @Test
    void should_swallow_an_exception_raised_while_persisting() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_swallow_exception_raised_by_merge)
        doThrow(new IllegalStateException("db down")).when(persistSuspendedPayment).persist(any());
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000914","amount":1.00,"currency":"EUR"}
                """;

        // WHEN / THEN
        assertThatCode(() -> kafkaSuspendedPaymentListener.onMessage(payload)).doesNotThrowAnyException();
        verify(persistSuspendedPayment).persist(any());
    }

    @Test
    void should_listen_to_its_own_topic_with_its_own_transactional_consumer_group() throws Exception {
        // GIVEN (remapped from the suspended row of KafkaListenerUnitTest.should_listen_to_its_own_topic_with_its_own_transactional_consumer_group)
        Class<?> type = KafkaSuspendedPaymentListener.class;

        // WHEN
        Method onMessage = type.getMethod("onMessage", String.class);

        // THEN
        KafkaListener kafkaListener = onMessage.getAnnotation(KafkaListener.class);
        assertThat(kafkaListener.topics()).containsExactly("pay.suspended");
        assertThat(kafkaListener.groupId()).isEqualTo("jpa-suspended-group");
        assertThat(onMessage.isAnnotationPresent(Transactional.class)).isTrue();
    }

    private PersistSuspendedPaymentCommand capturePersist() {
        ArgumentCaptor<PersistSuspendedPaymentCommand> captor = ArgumentCaptor.forClass(PersistSuspendedPaymentCommand.class);
        verify(persistSuspendedPayment).persist(captor.capture());
        verifyNoMoreInteractions(persistSuspendedPayment);
        return captor.getValue();
    }
}
