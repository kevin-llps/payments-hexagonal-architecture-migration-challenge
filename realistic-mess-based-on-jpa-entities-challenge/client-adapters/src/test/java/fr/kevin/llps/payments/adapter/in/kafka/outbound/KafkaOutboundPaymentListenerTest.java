package fr.kevin.llps.payments.adapter.in.kafka.outbound;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.annotation.Transactional;

import fr.kevin.llps.payments.domain.outbound.command.PersistOutboundPaymentCommand;
import fr.kevin.llps.payments.domain.outbound.port.in.PersistOutboundPayment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class KafkaOutboundPaymentListenerTest {

    @Mock
    private PersistOutboundPayment persistOutboundPayment;

    @InjectMocks
    private KafkaOutboundPaymentListener kafkaOutboundPaymentListener;

    @Test
    void should_persist_a_complete_outbound_event() {
        // GIVEN (remapped from KafkaListenerUnitTest.outbound_onMessage_persistsRow)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000904","instructionId":"OUT-1","amount":300.00,
                 "currency":"EUR","debtorIban":"DE89","debtorName":"ACME","destinationBic":"ABNANL2A",
                 "requestedExecutionDate":"2026-10-18","chargesType":"SHA","status":"SENT"}
                """;

        // WHEN
        kafkaOutboundPaymentListener.onMessage(payload);

        // THEN
        PersistOutboundPaymentCommand command = capturePersist();
        assertThat(command.instructionId()).isEqualTo("OUT-1");
        assertThat(command.amount()).isEqualByComparingTo("300.00");
        assertThat(command.requestedExecutionDate()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(command.chargesType()).isEqualTo("SHA");
    }

    @Test
    void should_swallow_a_malformed_json_event_without_persisting_it() {
        // GIVEN (remapped from KafkaListenerUnitTest.outbound_onMessage_malformedJson_swallowed)
        String malformed = "nope";

        // WHEN / THEN
        assertThatCode(() -> kafkaOutboundPaymentListener.onMessage(malformed)).doesNotThrowAnyException();
        verify(persistOutboundPayment, never()).persist(any());
    }

    @Test
    void should_skip_outbound_event_with_unparsable_requested_execution_date() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_skip_outbound_event_with_unparsable_requested_execution_date)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000913","amount":1.00,"currency":"EUR",
                 "requestedExecutionDate":"18/10/2026"}
                """;

        // WHEN
        kafkaOutboundPaymentListener.onMessage(payload);

        // THEN
        verifyNoInteractions(persistOutboundPayment);
    }

    @Test
    void should_map_missing_fields_to_empty_strings_and_json_nulls_to_null_literal() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_map_missing_outbound_fields_to_empty_strings_and_json_nulls_to_null_literal)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000919","chargesType":null,"amount":null}
                """;

        // WHEN
        kafkaOutboundPaymentListener.onMessage(payload);

        // THEN
        PersistOutboundPaymentCommand command = capturePersist();
        assertThat(command.chargesType()).isEqualTo("null");
        assertThat(command.instructionId()).isEmpty();
        assertThat(command.destinationBic()).isEmpty();
        assertThat(command.status()).isEmpty();
        assertThat(command.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(command.requestedExecutionDate()).isNull();
    }

    @ParameterizedTest(name = "[{index}] requestedExecutionDate {0} -> {1}")
    @CsvSource(value = {
            "2026-10-18, 2026-10-18",
            "2026-10-18T23:59:59Z, 2026-10-18",
            "null, NULL",
            "'', NULL"
    }, nullValues = "NULL")
    void should_parse_outbound_date_from_its_first_ten_characters(String raw, LocalDate expected) {
        // GIVEN (remapped from KafkaListenerUnitTest, same rows; expectations as equal LocalDate values)
        String payload = "{\"id\":\"00000000-0000-0000-0000-000000000924\",\"requestedExecutionDate\":\"" + raw + "\"}";

        // WHEN
        kafkaOutboundPaymentListener.onMessage(payload);

        // THEN
        assertThat(capturePersist().requestedExecutionDate()).isEqualTo(expected);
    }

    @Test
    void should_persist_outbound_event_with_unknown_charges_type_as_is() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_persist_outbound_event_with_unknown_charges_type_as_is)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000931","chargesType":"xx","destinationBic":"abnanl2a"}
                """;

        // WHEN
        kafkaOutboundPaymentListener.onMessage(payload);

        // THEN
        PersistOutboundPaymentCommand command = capturePersist();
        assertThat(command.chargesType()).isEqualTo("xx");
        assertThat(command.destinationBic()).isEqualTo("abnanl2a");
    }

    @Test
    void should_ignore_the_created_at_field_of_the_event() {
        // GIVEN (part of KafkaListenerUnitTest.should_set_created_at_and_updated_at_to_consumer_time_on_every_message:
        // the payload createdAt never reaches the command; consumer time is set by PersistOutboundPaymentService)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000911","instructionId":"OUT-11","amount":1.00,
                 "currency":"EUR","chargesType":"SHA","status":"SENT","createdAt":"2020-01-01T00:00:00"}
                """;

        // WHEN
        kafkaOutboundPaymentListener.onMessage(payload);

        // THEN
        assertThat(capturePersist().instructionId()).isEqualTo("OUT-11");
    }

    @Test
    void should_swallow_an_exception_raised_while_persisting() {
        // GIVEN (BR-ING-05, same contract as the other listeners)
        doThrow(new IllegalStateException("db down")).when(persistOutboundPayment).persist(any());
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000934"}
                """;

        // WHEN / THEN
        assertThatCode(() -> kafkaOutboundPaymentListener.onMessage(payload)).doesNotThrowAnyException();
        verify(persistOutboundPayment).persist(any());
    }

    @Test
    void should_listen_to_its_own_topic_with_its_own_transactional_consumer_group() throws Exception {
        // GIVEN (remapped from the outbound row of KafkaListenerUnitTest.should_listen_to_its_own_topic_with_its_own_transactional_consumer_group)
        Class<?> type = KafkaOutboundPaymentListener.class;

        // WHEN
        Method onMessage = type.getMethod("onMessage", String.class);

        // THEN
        KafkaListener kafkaListener = onMessage.getAnnotation(KafkaListener.class);
        assertThat(kafkaListener.topics()).containsExactly("pay.outbound");
        assertThat(kafkaListener.groupId()).isEqualTo("jpa-outbound-group");
        assertThat(onMessage.isAnnotationPresent(Transactional.class)).isTrue();
    }

    private PersistOutboundPaymentCommand capturePersist() {
        ArgumentCaptor<PersistOutboundPaymentCommand> captor = ArgumentCaptor.forClass(PersistOutboundPaymentCommand.class);
        verify(persistOutboundPayment).persist(captor.capture());
        verifyNoMoreInteractions(persistOutboundPayment);
        return captor.getValue();
    }
}
