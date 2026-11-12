package fr.kevin.llps.payments.adapter.in.kafka.inbound;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.annotation.Transactional;

import fr.kevin.llps.payments.domain.inbound.command.PersistInboundPaymentCommand;
import fr.kevin.llps.payments.domain.inbound.port.in.PersistInboundPayment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class KafkaInboundPaymentListenerTest {

    @Mock
    private PersistInboundPayment persistInboundPayment;

    @InjectMocks
    private KafkaInboundPaymentListener kafkaInboundPaymentListener;

    @Test
    void should_persist_the_event_and_keep_a_null_settlement_date() {
        // GIVEN (remapped from KafkaListenerUnitTest.inbound_onMessage_nullSettlementDate_staysNull)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000903","creditTransferRef":"CTR-1","amount":10.00,
                 "currency":"EUR","creditorIban":"DE89","creditorName":"ACME","originBic":"BNPAFRPP",
                 "valueDate":"2026-10-17","settlementDate":null,"status":"RECEIVED"}
                """;

        // WHEN
        kafkaInboundPaymentListener.onMessage(payload);

        // THEN
        PersistInboundPaymentCommand command = capturePersist();
        assertThat(command.creditTransferRef()).isEqualTo("CTR-1");
        assertThat(command.valueDate()).isEqualTo(LocalDate.of(2026, 10, 17));
        assertThat(command.settlementDate()).isNull();
    }

    @Test
    void should_swallow_a_malformed_json_event_without_persisting_it() {
        // GIVEN (remapped from KafkaListenerUnitTest.inbound_onMessage_malformedJson_swallowed)
        String malformed = "{not valid";

        // WHEN / THEN
        assertThatCode(() -> kafkaInboundPaymentListener.onMessage(malformed)).doesNotThrowAnyException();
        verify(persistInboundPayment, never()).persist(any());
    }

    @Test
    void should_skip_inbound_event_with_invalid_id() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_skip_inbound_event_with_invalid_id)
        String payload = """
                {"id":"not-a-uuid","creditTransferRef":"CTR-X","amount":1.00,"currency":"EUR"}
                """;

        // WHEN
        kafkaInboundPaymentListener.onMessage(payload);

        // THEN
        verifyNoInteractions(persistInboundPayment);
    }

    @Test
    void should_map_missing_fields_to_empty_strings_and_json_nulls_to_null_literal() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_map_missing_inbound_fields_to_empty_strings_and_json_nulls_to_null_literal)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000918","originBic":null}
                """;

        // WHEN
        kafkaInboundPaymentListener.onMessage(payload);

        // THEN
        PersistInboundPaymentCommand command = capturePersist();
        assertThat(command.originBic()).isEqualTo("null");
        assertThat(command.creditTransferRef()).isEmpty();
        assertThat(command.currency()).isEmpty();
        assertThat(command.status()).isEmpty();
        assertThat(command.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(command.valueDate()).isNull();
        assertThat(command.settlementDate()).isNull();
    }

    @Test
    void should_parse_dates_from_their_first_ten_characters() {
        // GIVEN (remapped from KafkaListenerUnitTest.should_parse_inbound_and_recall_style_dates_from_their_first_ten_characters)
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000925","valueDate":"2026-10-17T08:00:00",
                 "settlementDate":"2026-10-18T00:00:00Z"}
                """;

        // WHEN
        kafkaInboundPaymentListener.onMessage(payload);

        // THEN
        PersistInboundPaymentCommand command = capturePersist();
        assertThat(command.valueDate()).isEqualTo(LocalDate.of(2026, 10, 17));
        assertThat(command.settlementDate()).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    @Test
    void should_swallow_an_exception_raised_while_persisting() {
        // GIVEN (BR-ING-05, same contract as the other listeners)
        doThrow(new IllegalStateException("db down")).when(persistInboundPayment).persist(any());
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000933"}
                """;

        // WHEN / THEN
        assertThatCode(() -> kafkaInboundPaymentListener.onMessage(payload)).doesNotThrowAnyException();
        verify(persistInboundPayment).persist(any());
    }

    @Test
    void should_listen_to_its_own_topic_with_its_own_transactional_consumer_group() throws Exception {
        // GIVEN (remapped from the inbound row of KafkaListenerUnitTest.should_listen_to_its_own_topic_with_its_own_transactional_consumer_group)
        Class<?> type = KafkaInboundPaymentListener.class;

        // WHEN
        Method onMessage = type.getMethod("onMessage", String.class);

        // THEN
        KafkaListener kafkaListener = onMessage.getAnnotation(KafkaListener.class);
        assertThat(kafkaListener.topics()).containsExactly("pay.inbound");
        assertThat(kafkaListener.groupId()).isEqualTo("jpa-inbound-group");
        assertThat(onMessage.isAnnotationPresent(Transactional.class)).isTrue();
    }

    private PersistInboundPaymentCommand capturePersist() {
        ArgumentCaptor<PersistInboundPaymentCommand> captor = ArgumentCaptor.forClass(PersistInboundPaymentCommand.class);
        verify(persistInboundPayment).persist(captor.capture());
        verifyNoMoreInteractions(persistInboundPayment);
        return captor.getValue();
    }
}
