package fr.kevin.llps.payments.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.kafka.annotation.KafkaListener;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.processing.inbound.adapter.in.kafka.InboundPaymentListener;
import fr.kevin.llps.payments.processing.inbound.port.in.RecordInboundPaymentCommand;
import fr.kevin.llps.payments.processing.inbound.port.in.RecordInboundPaymentUseCase;
import fr.kevin.llps.payments.processing.instant.InstantPayment;
import fr.kevin.llps.payments.processing.instant.RecordInstantPaymentService;
import fr.kevin.llps.payments.processing.instant.adapter.in.kafka.InstantPaymentListener;
import fr.kevin.llps.payments.processing.instant.port.out.InstantPaymentRecordRepository;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import fr.kevin.llps.payments.entity.OutboundPayment;
import fr.kevin.llps.payments.compliancehold.adapter.in.kafka.SuspendedPaymentListener;
import fr.kevin.llps.payments.compliancehold.port.in.RecordSuspendedPaymentCommand;
import fr.kevin.llps.payments.compliancehold.port.in.RecordSuspendedPaymentUseCase;
import fr.kevin.llps.payments.recall.adapter.in.kafka.RecallListener;
import fr.kevin.llps.payments.recall.port.in.RecordRecallCommand;
import fr.kevin.llps.payments.recall.port.in.RecordRecallUseCase;
import fr.kevin.llps.payments.shared.domain.MalformedValueException;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class KafkaListenerUnitTest {

    @Mock
    private EntityManager em;
    @Mock
    private RecordRecallUseCase recordRecall;
    @Mock
    private RecordSuspendedPaymentUseCase recordSuspendedPayment;
    @Mock
    private RecordInboundPaymentUseCase recordInboundPayment;

    @Mock
    private InstantPaymentRecordRepository instantRepository;
    // real use case: scheme default and audit timestamps are part of what the instant listener tests assert
    private InstantPaymentListener instantListener;
    @InjectMocks
    private SuspendedPaymentListener suspendedListener;
    @InjectMocks
    private InboundPaymentListener inboundListener;
    @InjectMocks
    private OutboundPaymentMessageConsumer outboundConsumer;
    @InjectMocks
    private RecallListener recallListener;

    @BeforeEach
    void wireInstantListener() {
        instantListener = new InstantPaymentListener(
                new RecordInstantPaymentService(instantRepository, Clock.systemDefaultZone()));
    }

    @Test
    void instant_onMessage_persistsRow() {
        instantListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000901","endToEndId":"E2E-1","uetr":"uetr-1",
                 "amount":42.00,"currency":"EUR","debtorIban":"DE89","debtorName":"ACME",
                 "creditorIban":"FR76","creditorName":"Dupont","executionDateTime":"2026-10-17 10:00:00",
                 "status":"COMPLETED","scheme":"SCT INST"}
                """);

        InstantPayment payment = captureInstant();
        assertThat(payment.id()).isEqualTo(PaymentId.parse("00000000-0000-0000-0000-000000000901"));
        assertThat(payment.endToEndId()).isEqualTo("E2E-1");
        assertThat(payment.amount().amount()).isEqualByComparingTo("42.00");
        assertThat(payment.executionDatetime()).isEqualTo(LocalDateTime.of(2026, 10, 17, 10, 0, 0));
        assertThat(payment.scheme()).isEqualTo("SCT INST");
        assertThat(payment.audit().createdAt()).isNotNull();
    }

    @Test
    void instant_onMessage_malformedJson_swallowed() {
        assertThatCode(() -> instantListener.onMessage("not json")).doesNotThrowAnyException();
        verify(instantRepository, never()).upsert(any());
    }

    @ParameterizedTest(name = "[{index}] scheme fragment \"{0}\" defaults to SCT INST")
    @ValueSource(strings = {"", ",\"scheme\":null"})
    void instant_onMessage_missingOrNullScheme_defaultsToSctInst(String schemeFragment) {
        instantListener.onMessage("{\"id\":\"00000000-0000-0000-0000-000000000901\",\"amount\":1.00"
                + schemeFragment + "}");

        assertThat(captureInstant().scheme()).isEqualTo("SCT INST");
    }

    @ParameterizedTest(name = "[{index}] executionDateTime \"{0}\" -> {1}")
    @CsvSource({
            "2026-10-17T10:00:00Z, 2026-10-17T10:00:00",
            "2026-10-17T10:00:00, 2026-10-17T10:00:00",
            "2026-10-17, 2026-10-17T00:00:00"
    })
    void instant_onMessage_executionDateTime_stripsTAndZWithoutTimezoneConversion(String raw, LocalDateTime expected) {
        instantListener.onMessage("{\"id\":\"00000000-0000-0000-0000-000000000901\",\"amount\":1.00,"
                + "\"executionDateTime\":\"" + raw + "\"}");

        assertThat(captureInstant().executionDatetime()).isEqualTo(expected);
    }

    @Test
    void instant_onMessage_missingStringFieldsAreEmpty_nullStringFieldsAreLiteralNull_missingAmountIsZero() {
        instantListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000901","debtorName":null}
                """);

        InstantPayment payment = captureInstant();
        assertThat(payment.endToEndId()).isEmpty();
        assertThat(payment.status()).isEmpty();
        assertThat(payment.debtorName()).isEqualTo("null");
        assertThat(payment.amount().amount()).isEqualByComparingTo("0");
        assertThat(payment.executionDatetime()).isNull();
    }

    @Test
    void instant_onMessage_setsCreatedAtAndUpdatedAtToIngestionTime() {
        long before = System.currentTimeMillis();

        instantListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000901","amount":1.00}
                """);

        InstantPayment payment = captureInstant();
        long after = System.currentTimeMillis();
        assertThat(Timestamp.valueOf(payment.audit().createdAt()).getTime()).isBetween(before, after);
        assertThat(Timestamp.valueOf(payment.audit().updatedAt()).getTime()).isBetween(before, after);
    }

    @ParameterizedTest(name = "[{index}] id fragment \"{0}\" drops the message")
    @ValueSource(strings = {"\"id\":\"not-a-uuid\",", ""})
    void instant_onMessage_invalidOrMissingId_isDropped(String idFragment) {
        assertThatCode(() -> instantListener.onMessage("{" + idFragment + "\"amount\":1.00}"))
                .doesNotThrowAnyException();
        verify(instantRepository, never()).upsert(any());
    }

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
    void outbound_onMessage_persistenceFailure_swallowed() {
        when(em.merge(any(OutboundPayment.class))).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> outboundConsumer.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000904","amount":300.00,"chargesType":"SHA"}
                """)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "[{index}] originalPaymentId fragment \"{0}\" drops the recall")
    @ValueSource(strings = {"", ",\"originalPaymentId\":\"not-a-uuid\""})
    void recall_onMessage_missingOrInvalidOriginalPaymentId_isDropped(String originalIdFragment) {
        // the use case rejects the reference (RecordRecallServiceTest); the listener must swallow the rejection
        doThrow(new MalformedValueException("Invalid payment id", null)).when(recordRecall).record(any());

        assertThatCode(() -> recallListener.onMessage("{\"id\":\"00000000-0000-0000-0000-000000000905\""
                + originalIdFragment + "}")).doesNotThrowAnyException();
        assertThat(captureRecall().originalPaymentId()).isIn("", "not-a-uuid");
    }

    @Test
    void recall_onMessage_dateOnlyInitiatedAt_defaultsToMidnight() {
        recallListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000905",
                 "originalPaymentId":"11111111-1111-1111-1111-111111111111","initiatedAt":"2026-10-17",
                 "deadline":"2026-10-19T00:00:00Z"}
                """);

        RecordRecallCommand recall = captureRecall();
        assertThat(recall.initiatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 17, 0, 0, 0));
        assertThat(recall.deadline()).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    @ParameterizedTest(name = "[{index}] {0} listens on {1} with group {2}")
    @CsvSource({
            "fr.kevin.llps.payments.processing.instant.adapter.in.kafka.InstantPaymentListener, pay.instant, jpa-instant-group",
            "fr.kevin.llps.payments.compliancehold.adapter.in.kafka.SuspendedPaymentListener, pay.suspended, jpa-suspended-group",
            "fr.kevin.llps.payments.processing.inbound.adapter.in.kafka.InboundPaymentListener, pay.inbound, jpa-inbound-group",
            "fr.kevin.llps.payments.kafka.OutboundPaymentMessageConsumer, pay.outbound, jpa-outbound-group",
            "fr.kevin.llps.payments.recall.adapter.in.kafka.RecallListener, pay.recall, jpa-recall-group"
    })
    void listener_consumesItsTopicWithDedicatedConsumerGroup(String listenerClass, String topic, String groupId)
            throws Exception {
        KafkaListener annotation = Class.forName(listenerClass).getMethod("onMessage", String.class)
                .getAnnotation(KafkaListener.class);

        assertThat(annotation.topics()).containsExactly(topic);
        assertThat(annotation.groupId()).isEqualTo(groupId);
    }

    @Test
    void recall_onMessage_persistsRow() {
        recallListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000905","recallReference":"RCL-1","recallType":"R-COR",
                 "originalPaymentId":"00000000-0000-0000-0000-000000000904","originalPaymentType":"OUTBOUND",
                 "reasonCode":"AC04","amount":300.00,"currency":"EUR","initiatedAt":"2026-10-17 12:00:00",
                 "deadline":"2026-10-19","status":"INITIATED"}
                """);

        RecordRecallCommand recall = captureRecall();
        assertThat(recall.recallReference()).isEqualTo("RCL-1");
        assertThat(recall.originalPaymentId()).isEqualTo("00000000-0000-0000-0000-000000000904");
        assertThat(recall.initiatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 17, 12, 0, 0));
        assertThat(recall.deadline()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(recall.reasonCode()).isEqualTo("AC04");
    }

    @Test
    void recall_onMessage_malformedJson_swallowed() {
        assertThatCode(() -> recallListener.onMessage("<>")).doesNotThrowAnyException();
        verify(recordRecall, never()).record(any());
    }

    private RecordRecallCommand captureRecall() {
        ArgumentCaptor<RecordRecallCommand> captor = ArgumentCaptor.forClass(RecordRecallCommand.class);
        verify(recordRecall).record(captor.capture());
        return captor.getValue();
    }

    private RecordInboundPaymentCommand captureInbound() {
        ArgumentCaptor<RecordInboundPaymentCommand> captor = ArgumentCaptor.forClass(RecordInboundPaymentCommand.class);
        verify(recordInboundPayment).record(captor.capture());
        return captor.getValue();
    }

    private InstantPayment captureInstant() {
        ArgumentCaptor<InstantPayment> captor = ArgumentCaptor.forClass(InstantPayment.class);
        verify(instantRepository).upsert(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private <T> T captureMerge(Class<T> type) {
        ArgumentCaptor<T> captor = ArgumentCaptor.forClass(type);
        verify(em).merge(captor.capture());
        return captor.getValue();
    }
}
