package fr.kevin.llps.payments.adapter.in.kafka.instant;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.annotation.Transactional;

import fr.kevin.llps.payments.domain.instant.command.PersistInstantPaymentCommand;
import fr.kevin.llps.payments.domain.instant.port.in.PersistInstantPayment;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class KafkaInstantPaymentListenerTest {

    @Mock
    private PersistInstantPayment persistInstantPayment;

    @InjectMocks
    private KafkaInstantPaymentListener kafkaInstantPaymentListener;

    @Test
    void should_persist_a_complete_instant_event() {
        // GIVEN (remapped from KafkaListenerUnitTest.instant_onMessage_persistsRow)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000901","endToEndId":"E2E-1","uetr":"uetr-1",
                 "amount":42.00,"currency":"EUR","debtorIban":"DE89","debtorName":"ACME",
                 "creditorIban":"FR76","creditorName":"Dupont","executionDateTime":"2026-10-17 10:00:00",
                 "status":"COMPLETED","scheme":"SCT INST"}
                """;

        // WHEN
        kafkaInstantPaymentListener.onMessage(payload);

        // THEN
        PersistInstantPaymentCommand command = capturePersist();
        assertThat(command.id()).isEqualTo(PaymentId.parse("00000000-0000-0000-0000-000000000901"));
        assertThat(command.endToEndId()).isEqualTo("E2E-1");
        assertThat(command.amount()).isEqualByComparingTo("42.00");
        assertThat(command.executionDatetime()).isEqualTo(LocalDateTime.of(2026, 10, 17, 10, 0));
        assertThat(command.scheme()).isEqualTo("SCT INST");
    }

    @Test
    void should_swallow_a_malformed_json_event_without_persisting_it() {
        // GIVEN (remapped from KafkaListenerUnitTest.instant_onMessage_malformedJson_swallowed)
        String malformed = "not json";

        // WHEN / THEN
        assertThatCode(() -> kafkaInstantPaymentListener.onMessage(malformed)).doesNotThrowAnyException();
        verify(persistInstantPayment, never()).persist(any());
    }

    @Test
    void should_skip_instant_event_without_id() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_skip_instant_event_without_id)
        String payload = """
                {"endToEndId":"E2E-NOID","amount":1.00,"currency":"EUR"}
                """;

        // WHEN
        kafkaInstantPaymentListener.onMessage(payload);

        // THEN
        verifyNoInteractions(persistInstantPayment);
    }

    @Test
    void should_skip_instant_event_with_unparsable_execution_date_time() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_skip_instant_event_with_unparsable_execution_date_time)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000912","amount":1.00,"currency":"EUR",
                 "executionDateTime":"yesterday"}
                """;

        // WHEN
        kafkaInstantPaymentListener.onMessage(payload);

        // THEN
        verifyNoInteractions(persistInstantPayment);
    }

    @Test
    void should_map_missing_fields_to_empty_strings_and_json_nulls_to_null_literal() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_map_missing_instant_fields_to_empty_strings_and_json_nulls_to_null_literal)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000916","currency":null,"status":null}
                """;

        // WHEN
        kafkaInstantPaymentListener.onMessage(payload);

        // THEN
        PersistInstantPaymentCommand command = capturePersist();
        assertThat(command.currency()).isEqualTo("null");
        assertThat(command.status()).isEqualTo("null");
        assertThat(command.endToEndId()).isEmpty();
        assertThat(command.uetr()).isEmpty();
        assertThat(command.debtorIban()).isEmpty();
        assertThat(command.debtorName()).isEmpty();
        assertThat(command.creditorIban()).isEmpty();
        assertThat(command.creditorName()).isEmpty();
        assertThat(command.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(command.executionDatetime()).isNull();
    }

    @ParameterizedTest(name = "[{index}] executionDateTime {0} -> {1}")
    @CsvSource(value = {
            "2026-10-17T10:00:00Z, 2026-10-17T10:00:00",
            "2026-10-17T10:00:00, 2026-10-17T10:00:00",
            "2026-10-17 10:00:00, 2026-10-17T10:00:00",
            "2026-10-17, 2026-10-17T00:00:00",
            "null, NULL"
    }, nullValues = "NULL")
    void should_parse_instant_execution_date_time_without_time_zone_conversion(String raw, LocalDateTime expected) {
        // GIVEN (remapped from KafkaListenerUnitTest, same rows; expectations as equal LocalDateTime values)
        String payload = "{\"id\":\"00000000-0000-0000-0000-000000000921\",\"executionDateTime\":\"" + raw + "\"}";

        // WHEN
        kafkaInstantPaymentListener.onMessage(payload);

        // THEN
        assertThat(capturePersist().executionDatetime()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "[{index}] scheme field {0} -> SCT INST")
    @ValueSource(strings = {"", "\"scheme\":null,"})
    void should_default_scheme_to_sct_inst_when_missing_or_null(String schemeField) {
        // GIVEN (remapped from KafkaListenerUnitTest, same rows)
        String payload = "{" + schemeField + "\"id\":\"00000000-0000-0000-0000-000000000926\"}";

        // WHEN
        kafkaInstantPaymentListener.onMessage(payload);

        // THEN
        assertThat(capturePersist().scheme()).isEqualTo("SCT INST");
    }

    @Test
    void should_keep_scheme_given_by_the_event() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_keep_scheme_given_by_the_event)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000927","scheme":"SCT"}
                """;

        // WHEN
        kafkaInstantPaymentListener.onMessage(payload);

        // THEN
        assertThat(capturePersist().scheme()).isEqualTo("SCT");
    }

    @Test
    void should_ignore_api_style_execution_datetime_field_name_in_events() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_ignore_api_style_execution_datetime_field_name_in_events)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000928","executionDatetime":"2026-10-17 10:00:00"}
                """;

        // WHEN
        kafkaInstantPaymentListener.onMessage(payload);

        // THEN
        assertThat(capturePersist().executionDatetime()).isNull();
    }

    @Test
    void should_persist_instant_event_with_invalid_iban_and_unknown_status_as_is() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_persist_instant_event_with_invalid_iban_and_unknown_status_as_is)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000929","debtorIban":"X","creditorIban":"Y",
                 "status":"whatever","currency":"eur","amount":-5.00}
                """;

        // WHEN
        kafkaInstantPaymentListener.onMessage(payload);

        // THEN
        PersistInstantPaymentCommand command = capturePersist();
        assertThat(command.debtorIban()).isEqualTo("X");
        assertThat(command.creditorIban()).isEqualTo("Y");
        assertThat(command.status()).isEqualTo("whatever");
        assertThat(command.currency()).isEqualTo("eur");
        assertThat(command.amount()).isEqualByComparingTo("-5.00");
    }

    @Test
    void should_swallow_an_exception_raised_while_persisting() {
        // GIVEN (BR-ING-05, same contract as the other listeners)
        doThrow(new IllegalStateException("db down")).when(persistInstantPayment).persist(any());
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000932"}
                """;

        // WHEN / THEN
        assertThatCode(() -> kafkaInstantPaymentListener.onMessage(payload)).doesNotThrowAnyException();
        verify(persistInstantPayment).persist(any());
    }

    @Test
    void should_listen_to_its_own_topic_with_its_own_transactional_consumer_group() throws Exception {
        // GIVEN (remapped from the instant row of KafkaListenerUnitTest.should_listen_to_its_own_topic_with_its_own_transactional_consumer_group)
        Class<?> type = KafkaInstantPaymentListener.class;

        // WHEN
        Method onMessage = type.getMethod("onMessage", String.class);

        // THEN
        KafkaListener kafkaListener = onMessage.getAnnotation(KafkaListener.class);
        assertThat(kafkaListener.topics()).containsExactly("pay.instant");
        assertThat(kafkaListener.groupId()).isEqualTo("jpa-instant-group");
        assertThat(onMessage.isAnnotationPresent(Transactional.class)).isTrue();
    }

    private PersistInstantPaymentCommand capturePersist() {
        ArgumentCaptor<PersistInstantPaymentCommand> captor = ArgumentCaptor.forClass(PersistInstantPaymentCommand.class);
        verify(persistInstantPayment).persist(captor.capture());
        verifyNoMoreInteractions(persistInstantPayment);
        return captor.getValue();
    }
}
