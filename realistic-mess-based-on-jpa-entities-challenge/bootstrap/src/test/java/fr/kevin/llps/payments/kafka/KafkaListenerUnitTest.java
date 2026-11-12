package fr.kevin.llps.payments.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.transaction.annotation.Transactional;

import fr.kevin.llps.payments.entity.InboundPayment;
import fr.kevin.llps.payments.entity.OutboundPayment;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class KafkaListenerUnitTest {

    @Mock
    private EntityManager em;

    @InjectMocks
    private InboundPaymentListener inboundListener;
    @InjectMocks
    private OutboundPaymentMessageConsumer outboundConsumer;

    @Test
    void inbound_onMessage_nullSettlementDate_staysNull() {
        inboundListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000903","creditTransferRef":"CTR-1","amount":10.00,
                 "currency":"EUR","creditorIban":"DE89","creditorName":"ACME","originBic":"BNPAFRPP",
                 "valueDate":"2026-10-17","settlementDate":null,"status":"RECEIVED"}
                """);

        InboundPayment payment = captureMerge(InboundPayment.class);
        assertThat(payment.creditTransferRef).isEqualTo("CTR-1");
        assertThat(payment.valueDate).isEqualTo(Date.valueOf("2026-10-17"));
        assertThat(payment.settlementDate).isNull();
    }

    @Test
    void inbound_onMessage_malformedJson_swallowed() {
        assertThatCode(() -> inboundListener.onMessage("{not valid")).doesNotThrowAnyException();
        verify(em, never()).merge(org.mockito.ArgumentMatchers.any(InboundPayment.class));
    }

    @Test
    void outbound_onMessage_persistsRow() {
        outboundConsumer.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000904","instructionId":"OUT-1","amount":300.00,
                 "currency":"EUR","debtorIban":"DE89","debtorName":"ACME","destinationBic":"ABNANL2A",
                 "requestedExecutionDate":"2026-10-18","chargesType":"SHA","status":"SENT"}
                """);

        OutboundPayment payment = captureMerge(OutboundPayment.class);
        assertThat(payment.instructionId).isEqualTo("OUT-1");
        assertThat(payment.amount).isEqualByComparingTo("300.00");
        assertThat(payment.requestedExecutionDate).isEqualTo(Date.valueOf("2026-10-18"));
        assertThat(payment.chargesType).isEqualTo("SHA");
    }

    @Test
    void outbound_onMessage_malformedJson_swallowed() {
        assertThatCode(() -> outboundConsumer.onMessage("nope")).doesNotThrowAnyException();
        verify(em, never()).merge(org.mockito.ArgumentMatchers.any(OutboundPayment.class));
    }

    // ---------------- characterization: topics and consumer groups (BR-ING-01, BR-ING-02, BR-ING-06) ----------------

    @ParameterizedTest(name = "[{index}] {0} listens to {1} in group {2}")
    @CsvSource({
            "fr.kevin.llps.payments.kafka.InboundPaymentListener, pay.inbound, jpa-inbound-group",
            "fr.kevin.llps.payments.kafka.OutboundPaymentMessageConsumer, pay.outbound, jpa-outbound-group"
    })
    void should_listen_to_its_own_topic_with_its_own_transactional_consumer_group(String listenerClass, String topic,
            String groupId) throws Exception {
        // GIVEN
        Class<?> type = Class.forName(listenerClass);

        // WHEN
        Method onMessage = type.getMethod("onMessage", String.class);

        // THEN
        KafkaListener kafkaListener = onMessage.getAnnotation(KafkaListener.class);
        assertThat(kafkaListener.topics()).containsExactly(topic);
        assertThat(kafkaListener.groupId()).isEqualTo(groupId);
        assertThat(onMessage.isAnnotationPresent(Transactional.class)).isTrue();
    }

    // ---------------- characterization: audit timestamps (BR-ING-04) ----------------

    @Test
    void should_set_created_at_and_updated_at_to_consumer_time_on_every_message() {
        // GIVEN
        long before = System.currentTimeMillis();

        // WHEN
        outboundConsumer.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000911","instructionId":"OUT-11","amount":1.00,
                 "currency":"EUR","chargesType":"SHA","status":"SENT","createdAt":"2020-01-01T00:00:00"}
                """);

        // THEN
        long after = System.currentTimeMillis();
        OutboundPayment payment = captureMerge(OutboundPayment.class);
        assertThat(payment.createdAt.getTime()).isBetween(before, after);
        assertThat(payment.updatedAt.getTime()).isBetween(before, after);
    }

    // ---------------- characterization: skipped messages (BR-ING-05, BR-ING-11) ----------------

    @Test
    void should_skip_inbound_event_with_invalid_id() {
        // GIVEN
        String payload = """
                {"id":"not-a-uuid","creditTransferRef":"CTR-X","amount":1.00,"currency":"EUR"}
                """;

        // WHEN
        inboundListener.onMessage(payload);

        // THEN
        verifyNoMoreInteractions(em);
    }

    @Test
    void should_skip_outbound_event_with_unparsable_requested_execution_date() {
        // GIVEN
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000913","amount":1.00,"currency":"EUR",
                 "requestedExecutionDate":"18/10/2026"}
                """;

        // WHEN
        outboundConsumer.onMessage(payload);

        // THEN
        verifyNoMoreInteractions(em);
    }

    // ---------------- characterization: missing and null fields (BR-ING-07) ----------------

    @Test
    void should_map_missing_inbound_fields_to_empty_strings_and_json_nulls_to_null_literal() {
        // GIVEN
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000918","originBic":null}
                """;

        // WHEN
        inboundListener.onMessage(payload);

        // THEN
        InboundPayment payment = captureMerge(InboundPayment.class);
        assertThat(payment.originBic).isEqualTo("null");
        assertThat(payment.creditTransferRef).isEmpty();
        assertThat(payment.currency).isEmpty();
        assertThat(payment.status).isEmpty();
        assertThat(payment.amount).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(payment.valueDate).isNull();
        assertThat(payment.settlementDate).isNull();
    }

    @Test
    void should_map_missing_outbound_fields_to_empty_strings_and_json_nulls_to_null_literal() {
        // GIVEN
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000919","chargesType":null,"amount":null}
                """;

        // WHEN
        outboundConsumer.onMessage(payload);

        // THEN
        OutboundPayment payment = captureMerge(OutboundPayment.class);
        assertThat(payment.chargesType).isEqualTo("null");
        assertThat(payment.instructionId).isEmpty();
        assertThat(payment.destinationBic).isEmpty();
        assertThat(payment.status).isEmpty();
        assertThat(payment.amount).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(payment.requestedExecutionDate).isNull();
    }

    // ---------------- characterization: timestamp and date parsing (BR-ING-08, BR-ING-09) ----------------

    @ParameterizedTest(name = "[{index}] requestedExecutionDate {0} -> {1}")
    @CsvSource(value = {
            "2026-10-18, 2026-10-18",
            "2026-10-18T23:59:59Z, 2026-10-18",
            "null, NULL",
            "'', NULL"
    }, nullValues = "NULL")
    void should_parse_outbound_date_from_its_first_ten_characters(String raw, String expected) {
        // GIVEN
        String payload = "{\"id\":\"00000000-0000-0000-0000-000000000924\",\"requestedExecutionDate\":\"" + raw + "\"}";

        // WHEN
        outboundConsumer.onMessage(payload);

        // THEN
        OutboundPayment payment = captureMerge(OutboundPayment.class);
        assertThat(payment.requestedExecutionDate).isEqualTo(expected == null ? null : Date.valueOf(expected));
    }

    @Test
    void should_parse_inbound_and_recall_style_dates_from_their_first_ten_characters() {
        // GIVEN
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000925","valueDate":"2026-10-17T08:00:00",
                 "settlementDate":"2026-10-18T00:00:00Z"}
                """;

        // WHEN
        inboundListener.onMessage(payload);

        // THEN
        InboundPayment payment = captureMerge(InboundPayment.class);
        assertThat(payment.valueDate).isEqualTo(Date.valueOf("2026-10-17"));
        assertThat(payment.settlementDate).isEqualTo(Date.valueOf("2026-10-18"));
    }

    // ---------------- characterization: no business validation on ingestion (BR-ING-12, BR-SHR-10, BR-RCL-07) ----------------

    @Test
    void should_persist_outbound_event_with_unknown_charges_type_as_is() {
        // GIVEN
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000931","chargesType":"xx","destinationBic":"abnanl2a"}
                """;

        // WHEN
        outboundConsumer.onMessage(payload);

        // THEN
        OutboundPayment payment = captureMerge(OutboundPayment.class);
        assertThat(payment.chargesType).isEqualTo("xx");
        assertThat(payment.destinationBic).isEqualTo("abnanl2a");
    }

    @SuppressWarnings("unchecked")
    private <T> T captureMerge(Class<T> type) {
        ArgumentCaptor<T> captor = ArgumentCaptor.forClass(type);
        verify(em).merge(captor.capture());
        return captor.getValue();
    }
}