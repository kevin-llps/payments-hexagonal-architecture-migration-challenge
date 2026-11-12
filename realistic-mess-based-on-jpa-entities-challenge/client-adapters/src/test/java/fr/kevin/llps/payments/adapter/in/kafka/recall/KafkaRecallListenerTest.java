package fr.kevin.llps.payments.adapter.in.kafka.recall;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
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

import fr.kevin.llps.payments.domain.recall.command.PersistRecallCommand;
import fr.kevin.llps.payments.domain.recall.port.in.PersistRecall;
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
class KafkaRecallListenerTest {

    @Mock
    private PersistRecall persistRecall;

    @InjectMocks
    private KafkaRecallListener kafkaRecallListener;

    @Test
    void should_persist_a_complete_recall_event() {
        // GIVEN (remapped from KafkaListenerUnitTest.recall_onMessage_persistsRow)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000905","recallReference":"RCL-1","recallType":"R-COR",
                 "originalPaymentId":"00000000-0000-0000-0000-000000000904","originalPaymentType":"OUTBOUND",
                 "reasonCode":"AC04","amount":300.00,"currency":"EUR","initiatedAt":"2026-10-17 12:00:00",
                 "deadline":"2026-10-19","status":"INITIATED"}
                """;

        // WHEN
        kafkaRecallListener.onMessage(payload);

        // THEN
        PersistRecallCommand command = capturePersist();
        assertThat(command.recallReference()).isEqualTo("RCL-1");
        assertThat(command.originalPaymentId()).isEqualTo(PaymentId.parse("00000000-0000-0000-0000-000000000904"));
        assertThat(command.initiatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 17, 12, 0));
        assertThat(command.deadline()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(command.reasonCode()).isEqualTo("AC04");
    }

    @Test
    void should_swallow_a_malformed_json_event_without_persisting_it() {
        // GIVEN (remapped from KafkaListenerUnitTest.recall_onMessage_malformedJson_swallowed)
        String malformed = "<>";

        // WHEN / THEN
        assertThatCode(() -> kafkaRecallListener.onMessage(malformed)).doesNotThrowAnyException();
        verify(persistRecall, never()).persist(any());
    }

    @ParameterizedTest(name = "[{index}] originalPaymentId {0} -> recall dropped")
    @ValueSource(strings = {
            "\"originalPaymentId\":\"not-a-uuid\",",
            "\"originalPaymentId\":null,",
            ""
    })
    void should_drop_recall_event_when_original_payment_id_is_not_a_uuid(String originalPaymentIdField) {
        // GIVEN (remapped from KafkaListenerUnitTest, same rows)
        String payload = "{\"id\":\"00000000-0000-0000-0000-000000000915\"," + originalPaymentIdField
                + "\"recallType\":\"R-COR\",\"amount\":1.00,\"currency\":\"EUR\"}";

        // WHEN
        kafkaRecallListener.onMessage(payload);

        // THEN
        verifyNoInteractions(persistRecall);
    }

    @Test
    void should_map_missing_fields_to_empty_strings_and_json_nulls_to_null_literal() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_map_missing_recall_fields_to_empty_strings_and_json_nulls_to_null_literal)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000920",
                 "originalPaymentId":"00000000-0000-0000-0000-000000000401","reasonCode":null}
                """;

        // WHEN
        kafkaRecallListener.onMessage(payload);

        // THEN
        PersistRecallCommand command = capturePersist();
        assertThat(command.reasonCode()).isEqualTo("null");
        assertThat(command.recallReference()).isEmpty();
        assertThat(command.recallType()).isEmpty();
        assertThat(command.originalPaymentType()).isEmpty();
        assertThat(command.currency()).isEmpty();
        assertThat(command.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(command.initiatedAt()).isNull();
        assertThat(command.deadline()).isNull();
    }

    @ParameterizedTest(name = "[{index}] initiatedAt {0} -> {1}")
    @CsvSource({
            "2026-10-17T12:00:00Z, 2026-10-17T12:00:00",
            "2026-10-17, 2026-10-17T00:00:00"
    })
    void should_parse_recall_initiated_at_with_same_timestamp_rules(String raw, LocalDateTime expected) {
        // GIVEN (remapped from KafkaListenerUnitTest, same rows; expectations as equal LocalDateTime values)
        String payload = "{\"id\":\"00000000-0000-0000-0000-000000000922\","
                + "\"originalPaymentId\":\"00000000-0000-0000-0000-000000000401\",\"initiatedAt\":\"" + raw + "\"}";

        // WHEN
        kafkaRecallListener.onMessage(payload);

        // THEN
        assertThat(capturePersist().initiatedAt()).isEqualTo(expected);
    }

    @Test
    void should_persist_a_recall_with_unknown_type_without_checking_the_original_payment() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_persist_recall_with_unknown_type_without_checking_original_payment)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000930","recallType":"R-XXX",
                 "originalPaymentId":"99999999-9999-9999-9999-999999999999","originalPaymentType":"CHEQUE",
                 "status":"unknown"}
                """;

        // WHEN
        kafkaRecallListener.onMessage(payload);

        // THEN
        PersistRecallCommand command = capturePersist();
        assertThat(command.recallType()).isEqualTo("R-XXX");
        assertThat(command.originalPaymentId()).isEqualTo(PaymentId.parse("99999999-9999-9999-9999-999999999999"));
        assertThat(command.originalPaymentType()).isEqualTo("CHEQUE");
        assertThat(command.status()).isEqualTo("unknown");
    }

    @Test
    void should_swallow_an_exception_raised_while_persisting() {
        // GIVEN (BR-ING-05, same contract as the other listeners)
        doThrow(new IllegalStateException("db down")).when(persistRecall).persist(any());
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000931","originalPaymentId":"00000000-0000-0000-0000-000000000401"}
                """;

        // WHEN / THEN
        assertThatCode(() -> kafkaRecallListener.onMessage(payload)).doesNotThrowAnyException();
        verify(persistRecall).persist(any());
    }

    @Test
    void should_listen_to_its_own_topic_with_its_own_transactional_consumer_group() throws Exception {
        // GIVEN (remapped from the recall row of KafkaListenerUnitTest.should_listen_to_its_own_topic_with_its_own_transactional_consumer_group)
        Class<?> type = KafkaRecallListener.class;

        // WHEN
        Method onMessage = type.getMethod("onMessage", String.class);

        // THEN
        KafkaListener kafkaListener = onMessage.getAnnotation(KafkaListener.class);
        assertThat(kafkaListener.topics()).containsExactly("pay.recall");
        assertThat(kafkaListener.groupId()).isEqualTo("jpa-recall-group");
        assertThat(onMessage.isAnnotationPresent(Transactional.class)).isTrue();
    }

    private PersistRecallCommand capturePersist() {
        ArgumentCaptor<PersistRecallCommand> captor = ArgumentCaptor.forClass(PersistRecallCommand.class);
        verify(persistRecall).persist(captor.capture());
        verifyNoMoreInteractions(persistRecall);
        return captor.getValue();
    }
}
