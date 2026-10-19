package fr.kevin.llps.payments.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import fr.kevin.llps.payments.entity.InboundPayment;
import fr.kevin.llps.payments.entity.InstantPayment;
import fr.kevin.llps.payments.entity.OutboundPayment;
import fr.kevin.llps.payments.entity.Recall;
import fr.kevin.llps.payments.entity.SuspendedPayment;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class KafkaListenerUnitTest {

    @Mock
    private EntityManager em;

    @InjectMocks
    private InstantPaymentListener instantListener;
    @InjectMocks
    private SuspendedPaymentListener suspendedListener;
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
    void suspended_onMessage_nullReleasedAt_staysNull() {
        suspendedListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000902","amount":5000.00,"currency":"EUR",
                 "debtorIban":"DE89","debtorName":"ACME","creditorIban":"FR76","creditorName":"Dupont",
                 "holdReason":"SANCTIONS_SCREENING","complianceCaseRef":"CASE-1","reviewStatus":"PENDING",
                 "heldAt":"2026-10-17 11:00:00","releasedAt":null}
                """);

        SuspendedPayment payment = captureMerge(SuspendedPayment.class);
        assertThat(payment.complianceCaseRef).isEqualTo("CASE-1");
        assertThat(payment.heldAt).isEqualTo(Timestamp.valueOf("2026-10-17 11:00:00"));
        assertThat(payment.releasedAt).isNull();
    }

    @Test
    void suspended_onMessage_malformedJson_swallowed() {
        assertThatCode(() -> suspendedListener.onMessage("{bad")).doesNotThrowAnyException();
        verify(em, never()).merge(org.mockito.ArgumentMatchers.any(SuspendedPayment.class));
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

    @SuppressWarnings("unchecked")
    private <T> T captureMerge(Class<T> type) {
        ArgumentCaptor<T> captor = ArgumentCaptor.forClass(type);
        verify(em).merge(captor.capture());
        return captor.getValue();
    }
}
