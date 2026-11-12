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
import fr.kevin.llps.payments.entity.InstantPayment;
import fr.kevin.llps.payments.entity.OutboundPayment;
import fr.kevin.llps.payments.entity.Recall;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class KafkaListenerUnitTest {

    @Mock
    private EntityManager em;

    @InjectMocks
    private InstantPaymentListener instantListener;
    @InjectMocks
    private InboundPaymentListener inboundListener;
    @InjectMocks
    private OutboundPaymentMessageConsumer outboundConsumer;
    @InjectMocks
    private RecallListener recallListener;

    @Test
    void instant_onMessage_persistsRow() {
        instantListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000901","endToEndId":"E2E-1","uetr":"uetr-1",
                 "amount":42.00,"currency":"EUR","debtorIban":"DE89","debtorName":"ACME",
                 "creditorIban":"FR76","creditorName":"Dupont","executionDateTime":"2026-10-17 10:00:00",
                 "status":"COMPLETED","scheme":"SCT INST"}
                """);

        InstantPayment payment = captureMerge(InstantPayment.class);
        assertThat(payment.id).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000901"));
        assertThat(payment.endToEndId).isEqualTo("E2E-1");
        assertThat(payment.amount).isEqualByComparingTo("42.00");
        assertThat(payment.executionDatetime).isEqualTo(Timestamp.valueOf("2026-10-17 10:00:00"));
        assertThat(payment.scheme).isEqualTo("SCT INST");
        assertThat(payment.createdAt).isNotNull();
    }

    @Test
    void instant_onMessage_malformedJson_swallowed() {
        assertThatCode(() -> instantListener.onMessage("not json")).doesNotThrowAnyException();
        verify(em, never()).merge(org.mockito.ArgumentMatchers.any(InstantPayment.class));
    }

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

    @Test
    void recall_onMessage_persistsRow() {
        recallListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000905","recallReference":"RCL-1","recallType":"R-COR",
                 "originalPaymentId":"00000000-0000-0000-0000-000000000904","originalPaymentType":"OUTBOUND",
                 "reasonCode":"AC04","amount":300.00,"currency":"EUR","initiatedAt":"2026-10-17 12:00:00",
                 "deadline":"2026-10-19","status":"INITIATED"}
                """);

        Recall recall = captureMerge(Recall.class);
        assertThat(recall.recallReference).isEqualTo("RCL-1");
        assertThat(recall.originalPaymentId).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000904"));
        assertThat(recall.initiatedAt).isEqualTo(Timestamp.valueOf("2026-10-17 12:00:00"));
        assertThat(recall.deadline).isEqualTo(Date.valueOf("2026-10-19"));
        assertThat(recall.reasonCode).isEqualTo("AC04");
    }

    @Test
    void recall_onMessage_malformedJson_swallowed() {
        assertThatCode(() -> recallListener.onMessage("<>")).doesNotThrowAnyException();
        verify(em, never()).merge(org.mockito.ArgumentMatchers.any(Recall.class));
    }

    // ---------------- characterization: topics and consumer groups (BR-ING-01, BR-ING-02, BR-ING-06) ----------------

    @ParameterizedTest(name = "[{index}] {0} listens to {1} in group {2}")
    @CsvSource({
            "fr.kevin.llps.payments.kafka.InstantPaymentListener, pay.instant, jpa-instant-group",
            "fr.kevin.llps.payments.kafka.InboundPaymentListener, pay.inbound, jpa-inbound-group",
            "fr.kevin.llps.payments.kafka.OutboundPaymentMessageConsumer, pay.outbound, jpa-outbound-group",
            "fr.kevin.llps.payments.kafka.RecallListener, pay.recall, jpa-recall-group"
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
    void should_skip_instant_event_without_id() {
        // GIVEN
        String payload = """
                {"endToEndId":"E2E-NOID","amount":1.00,"currency":"EUR"}
                """;

        // WHEN
        instantListener.onMessage(payload);

        // THEN
        verifyNoMoreInteractions(em);
    }

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
    void should_skip_instant_event_with_unparsable_execution_date_time() {
        // GIVEN
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000912","amount":1.00,"currency":"EUR",
                 "executionDateTime":"yesterday"}
                """;

        // WHEN
        instantListener.onMessage(payload);

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

    @ParameterizedTest(name = "[{index}] originalPaymentId {0} -> recall dropped")
    @ValueSource(strings = {
            "\"originalPaymentId\":\"not-a-uuid\",",
            "\"originalPaymentId\":null,",
            ""
    })
    void should_drop_recall_event_when_original_payment_id_is_not_a_uuid(String originalPaymentIdField) {
        // GIVEN
        String payload = "{\"id\":\"00000000-0000-0000-0000-000000000915\"," + originalPaymentIdField
                + "\"recallType\":\"R-COR\",\"amount\":1.00,\"currency\":\"EUR\"}";

        // WHEN
        recallListener.onMessage(payload);

        // THEN
        verifyNoMoreInteractions(em);
    }

    // ---------------- characterization: missing and null fields (BR-ING-07) ----------------

    @Test
    void should_map_missing_instant_fields_to_empty_strings_and_json_nulls_to_null_literal() {
        // GIVEN
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000916","currency":null,"status":null}
                """;

        // WHEN
        instantListener.onMessage(payload);

        // THEN
        InstantPayment payment = captureMerge(InstantPayment.class);
        assertThat(payment.currency).isEqualTo("null");
        assertThat(payment.status).isEqualTo("null");
        assertThat(payment.endToEndId).isEmpty();
        assertThat(payment.uetr).isEmpty();
        assertThat(payment.debtorIban).isEmpty();
        assertThat(payment.debtorName).isEmpty();
        assertThat(payment.creditorIban).isEmpty();
        assertThat(payment.creditorName).isEmpty();
        assertThat(payment.amount).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(payment.executionDatetime).isNull();
    }

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

    @Test
    void should_map_missing_recall_fields_to_empty_strings_and_json_nulls_to_null_literal() {
        // GIVEN
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000920",
                 "originalPaymentId":"00000000-0000-0000-0000-000000000401","reasonCode":null}
                """;

        // WHEN
        recallListener.onMessage(payload);

        // THEN
        Recall recall = captureMerge(Recall.class);
        assertThat(recall.reasonCode).isEqualTo("null");
        assertThat(recall.recallReference).isEmpty();
        assertThat(recall.recallType).isEmpty();
        assertThat(recall.originalPaymentType).isEmpty();
        assertThat(recall.currency).isEmpty();
        assertThat(recall.amount).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(recall.initiatedAt).isNull();
        assertThat(recall.deadline).isNull();
    }

    // ---------------- characterization: timestamp and date parsing (BR-ING-08, BR-ING-09) ----------------

    @ParameterizedTest(name = "[{index}] executionDateTime {0} -> {1}")
    @CsvSource(value = {
            "2026-10-17T10:00:00Z, 2026-10-17 10:00:00",
            "2026-10-17T10:00:00, 2026-10-17 10:00:00",
            "2026-10-17 10:00:00, 2026-10-17 10:00:00",
            "2026-10-17, 2026-10-17 00:00:00",
            "null, NULL"
    }, nullValues = "NULL")
    void should_parse_instant_execution_date_time_without_time_zone_conversion(String raw, String expected) {
        // GIVEN
        String payload = "{\"id\":\"00000000-0000-0000-0000-000000000921\",\"executionDateTime\":\"" + raw + "\"}";

        // WHEN
        instantListener.onMessage(payload);

        // THEN
        InstantPayment payment = captureMerge(InstantPayment.class);
        assertThat(payment.executionDatetime).isEqualTo(expected == null ? null : Timestamp.valueOf(expected));
    }

    @ParameterizedTest(name = "[{index}] initiatedAt {0} -> {1}")
    @CsvSource({
            "2026-10-17T12:00:00Z, 2026-10-17 12:00:00",
            "2026-10-17, 2026-10-17 00:00:00"
    })
    void should_parse_recall_initiated_at_with_same_timestamp_rules(String raw, String expected) {
        // GIVEN
        String payload = "{\"id\":\"00000000-0000-0000-0000-000000000922\","
                + "\"originalPaymentId\":\"00000000-0000-0000-0000-000000000401\",\"initiatedAt\":\"" + raw + "\"}";

        // WHEN
        recallListener.onMessage(payload);

        // THEN
        Recall recall = captureMerge(Recall.class);
        assertThat(recall.initiatedAt).isEqualTo(Timestamp.valueOf(expected));
    }

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

    // ---------------- characterization: instant scheme and field naming (BR-ING-10) ----------------

    @ParameterizedTest(name = "[{index}] scheme field {0} -> SCT INST")
    @ValueSource(strings = {"", "\"scheme\":null,"})
    void should_default_scheme_to_sct_inst_when_missing_or_null(String schemeField) {
        // GIVEN
        String payload = "{" + schemeField + "\"id\":\"00000000-0000-0000-0000-000000000926\"}";

        // WHEN
        instantListener.onMessage(payload);

        // THEN
        assertThat(captureMerge(InstantPayment.class).scheme).isEqualTo("SCT INST");
    }

    @Test
    void should_keep_scheme_given_by_the_event() {
        // GIVEN
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000927","scheme":"SCT"}
                """;

        // WHEN
        instantListener.onMessage(payload);

        // THEN
        assertThat(captureMerge(InstantPayment.class).scheme).isEqualTo("SCT");
    }

    @Test
    void should_ignore_api_style_execution_datetime_field_name_in_events() {
        // GIVEN
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000928","executionDatetime":"2026-10-17 10:00:00"}
                """;

        // WHEN
        instantListener.onMessage(payload);

        // THEN
        assertThat(captureMerge(InstantPayment.class).executionDatetime).isNull();
    }

    // ---------------- characterization: no business validation on ingestion (BR-ING-12, BR-SHR-10, BR-RCL-07) ----------------

    @Test
    void should_persist_instant_event_with_invalid_iban_and_unknown_status_as_is() {
        // GIVEN
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000929","debtorIban":"X","creditorIban":"Y",
                 "status":"whatever","currency":"eur","amount":-5.00}
                """;

        // WHEN
        instantListener.onMessage(payload);

        // THEN
        InstantPayment payment = captureMerge(InstantPayment.class);
        assertThat(payment.debtorIban).isEqualTo("X");
        assertThat(payment.creditorIban).isEqualTo("Y");
        assertThat(payment.status).isEqualTo("whatever");
        assertThat(payment.currency).isEqualTo("eur");
        assertThat(payment.amount).isEqualByComparingTo("-5.00");
    }

    @Test
    void should_persist_recall_with_unknown_type_without_checking_original_payment() {
        // GIVEN
        String payload = """
                {"id":"00000000-0000-0000-0000-000000000930","recallType":"R-XXX",
                 "originalPaymentId":"99999999-9999-9999-9999-999999999999","originalPaymentType":"CHEQUE",
                 "status":"unknown"}
                """;

        // WHEN
        recallListener.onMessage(payload);

        // THEN
        Recall recall = captureMerge(Recall.class);
        assertThat(recall.recallType).isEqualTo("R-XXX");
        assertThat(recall.originalPaymentId).isEqualTo(UUID.fromString("99999999-9999-9999-9999-999999999999"));
        assertThat(recall.originalPaymentType).isEqualTo("CHEQUE");
        assertThat(recall.status).isEqualTo("unknown");
        verifyNoMoreInteractions(em);
    }

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