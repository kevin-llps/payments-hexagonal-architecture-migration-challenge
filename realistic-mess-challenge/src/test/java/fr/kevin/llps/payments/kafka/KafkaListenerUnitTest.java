package fr.kevin.llps.payments.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class KafkaListenerUnitTest {

    @Mock
    private JdbcTemplate jdbc;

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

        String sql = captureSql();
        assertThat(sql)
                .contains("INSERT INTO instant_payments")
                .contains("'E2E-1'")
                .contains("42")
                .contains("'2026-10-17 10:00:00'")
                .contains("ON CONFLICT (id) DO UPDATE");
    }

    @Test
    void instant_onMessage_malformedJson_swallowed() {
        assertThatCode(() -> instantListener.onMessage("not json")).doesNotThrowAnyException();
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void suspended_onMessage_nullReleasedAt_usesUnquotedNull() {
        suspendedListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000902","amount":5000.00,"currency":"EUR",
                 "debtorIban":"DE89","debtorName":"ACME","creditorIban":"FR76","creditorName":"Dupont",
                 "holdReason":"SANCTIONS_SCREENING","complianceCaseRef":"CASE-1","reviewStatus":"PENDING",
                 "heldAt":"2026-10-17 11:00:00","releasedAt":null}
                """);

        String sql = captureSql();
        assertThat(sql)
                .contains("INSERT INTO suspended_payments")
                .contains("'CASE-1'")
                .contains("'2026-10-17 11:00:00'")
                .doesNotContain("'null'");
    }

    @Test
    void suspended_onMessage_malformedJson_swallowed() {
        assertThatCode(() -> suspendedListener.onMessage("{bad")).doesNotThrowAnyException();
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void inbound_onMessage_nullSettlementDate_usesUnquotedNull() {
        inboundListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000903","creditTransferRef":"CTR-1","amount":10.00,
                 "currency":"EUR","creditorIban":"DE89","creditorName":"ACME","originBic":"BNPAFRPP",
                 "valueDate":"2026-10-17","settlementDate":null,"status":"RECEIVED"}
                """);

        String sql = captureSql();
        assertThat(sql)
                .contains("INSERT INTO inbound_payments")
                .contains("'CTR-1'")
                .contains("'2026-10-17'")
                .doesNotContain("'null'");
    }

    @Test
    void inbound_onMessage_malformedJson_swallowed() {
        assertThatCode(() -> inboundListener.onMessage("{not valid")).doesNotThrowAnyException();
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void outbound_onMessage_persistsRow() {
        outboundConsumer.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000904","instructionId":"OUT-1","amount":300.00,
                 "currency":"EUR","debtorIban":"DE89","debtorName":"ACME","destinationBic":"ABNANL2A",
                 "requestedExecutionDate":"2026-10-18","chargesType":"SHA","status":"SENT"}
                """);

        String sql = captureSql();
        assertThat(sql)
                .contains("INSERT INTO outbound_payments")
                .contains("'OUT-1'")
                .contains("'2026-10-18'")
                .contains("'SHA'");
    }

    @Test
    void outbound_onMessage_malformedJson_swallowed() {
        assertThatCode(() -> outboundConsumer.onMessage("nope")).doesNotThrowAnyException();
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void recall_onMessage_persistsRow() {
        recallListener.onMessage("""
                {"id":"00000000-0000-0000-0000-000000000905","recallReference":"RCL-1","recallType":"R-COR",
                 "originalPaymentId":"00000000-0000-0000-0000-000000000904","originalPaymentType":"OUTBOUND",
                 "reasonCode":"AC04","amount":300.00,"currency":"EUR","initiatedAt":"2026-10-17 12:00:00",
                 "deadline":"2026-10-19","status":"INITIATED"}
                """);

        String sql = captureSql();
        assertThat(sql)
                .contains("INSERT INTO recalls")
                .contains("'RCL-1'")
                .contains("'2026-10-19'")
                .contains("'AC04'");
    }

    @Test
    void recall_onMessage_malformedJson_swallowed() {
        assertThatCode(() -> recallListener.onMessage("<>")).doesNotThrowAnyException();
        verify(jdbc, never()).execute(anyString());
    }

    private String captureSql() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).execute(captor.capture());
        return captor.getValue();
    }
}
