package fr.kevin.llps.payments.mess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentsGodServiceUnitTest {

    @Mock
    private JdbcTemplate jdbc;
    @Mock
    private DataSource dataSource;

    @InjectMocks
    private PaymentsGodService service;

    @BeforeEach
    void resetStaticState() {
        PaymentsGodService.QUERY_CACHE.clear();
        PaymentsGodService.QUERY_COUNTS.clear();
        PaymentsGodService.RECENT_ERRORS.clear();
    }

    // ---------------- instant search ----------------

    @Test
    void searchInstantPayments_computesWarningAndActionable() {
        Map<String, Object> pendingHigh = instantRow("1", new BigDecimal("20000.00"), "PENDING", null);
        Map<String, Object> completed = instantRow("2", new BigDecimal("10.00"), "COMPLETED", null);
        Map<String, Object> rejected = instantRow("3", new BigDecimal("20000.00"), "REJECTED", null);
        when(jdbc.queryForList(anyString())).thenReturn(List.of(pendingHigh, completed, rejected));

        List<Map<String, Object>> result = service.searchInstantPayments(null, null, null, null, null, null, null, null);

        assertThat(result.get(0).get("warning")).isEqualTo("HIGH_AMOUNT_PENDING");
        assertThat(result.get(0).get("actionable")).isEqualTo(Boolean.TRUE);
        assertThat(result.get(1).get("warning")).isNull();
        assertThat(result.get(1).get("actionable")).isEqualTo(Boolean.FALSE);
        assertThat(result.get(2).get("warning")).isEqualTo("HIGH_AMOUNT");
        assertThat(result.get(2).get("actionable")).isEqualTo(Boolean.FALSE);
    }

    @Test
    void searchInstantPayments_buildsAllFilterClauses() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of());

        service.searchInstantPayments("E2E-1", "DE89", "FR76", "completed", "eur",
                "2026-10-01T00:00:00", "2026-10-31T00:00:00", 10);

        String sql = captureSql();
        assertThat(sql)
                .contains("FROM instant_payments")
                .contains("end_to_end_id LIKE '%E2E-1%'")
                .contains("UPPER(debtor_iban) LIKE '%DE89%'")
                .contains("UPPER(creditor_iban) LIKE '%FR76%'")
                .contains("status = 'COMPLETED'")
                .contains("currency = 'EUR'")
                .contains("execution_datetime >= '2026-10-01 00:00:00'")
                .contains("LIMIT 10");
    }

    @Test
    void searchInstantPayments_sanitizesQuotesAndSemicolons() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of());

        service.searchInstantPayments("E2E'DROP;TABLE", null, null, null, null, null, null, null);

        String sql = captureSql();
        assertThat(sql).contains("E2EDROPTABLE");
        assertThat(sql).doesNotContain("'DROP'");
        assertThat(sql).doesNotContain(";");
    }

    @Test
    void searchInstantPayments_appliesLimitBoundaries() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of());

        service.searchInstantPayments(null, null, null, null, null, null, null, null);
        assertThat(captureSql()).contains("LIMIT 100");

        service.searchInstantPayments(null, null, null, null, null, null, null, 0);
        assertThat(captureSql()).contains("LIMIT 100");

        service.searchInstantPayments(null, null, null, null, null, null, null, 999);
        assertThat(captureSql()).contains("LIMIT 500");
    }

    @Test
    void searchInstantPayments_secondCallUsesCache() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of(instantRow("1", new BigDecimal("1.00"), "PENDING", null)));

        service.searchInstantPayments("E2E-1", null, null, null, null, null, null, null);
        List<Map<String, Object>> second = service.searchInstantPayments("E2E-1", null, null, null, null, null, null, null);

        verify(jdbc, times(1)).queryForList(anyString());
        assertThat(second).hasSize(1);
    }

    @Test
    void searchInstantPayments_repositoryFailure_returnsEmptyAndRecordsError() {
        when(jdbc.queryForList(anyString())).thenThrow(new RuntimeException("db down"));

        List<Map<String, Object>> result = service.searchInstantPayments(null, null, null, null, null, null, null, null);

        assertThat(result).isEmpty();
        assertThat((List<?>) service.getInternalStats().get("recentErrors")).isNotEmpty();
    }

    // ---------------- suspended search ----------------

    @Test
    void searchSuspendedPayments_computesSlaBreach() {
        Map<String, Object> breached = suspendedRow("1", "PENDING", new java.sql.Timestamp(
                System.currentTimeMillis() - 72L * 3600_000L));
        Map<String, Object> fresh = suspendedRow("2", "PENDING", new java.sql.Timestamp(
                System.currentTimeMillis() - 1L * 3600_000L));
        Map<String, Object> released = suspendedRow("3", "RELEASED", new java.sql.Timestamp(
                System.currentTimeMillis() - 72L * 3600_000L));
        when(jdbc.queryForList(anyString())).thenReturn(List.of(breached, fresh, released));

        List<Map<String, Object>> result = service.searchSuspendedPayments(null, null, null, null, null, null);

        assertThat(result.get(0).get("slaBreached")).isEqualTo(Boolean.TRUE);
        assertThat(result.get(1).get("slaBreached")).isEqualTo(Boolean.FALSE);
        assertThat(result.get(1).get("actionable")).isEqualTo(Boolean.TRUE);
        assertThat(result.get(2).get("actionable")).isEqualTo(Boolean.FALSE);
    }

    // ---------------- inbound search ----------------

    @Test
    void searchInboundPayments_computesStaleFlag() {
        Map<String, Object> stale = inboundRow("1", java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(5)), "RECEIVED");
        Map<String, Object> fresh = inboundRow("2", java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(1)), "RECEIVED");
        when(jdbc.queryForList(anyString())).thenReturn(List.of(stale, fresh));

        List<Map<String, Object>> result = service.searchInboundPayments(null, null, null, null, null, null, null);

        assertThat(result.get(0).get("stale")).isEqualTo(Boolean.TRUE);
        assertThat((Long) result.get(0).get("ageInDays")).isGreaterThanOrEqualTo(5L);
        assertThat(result.get(1).get("stale")).isEqualTo(Boolean.FALSE);
    }

    // ---------------- outbound search ----------------

    @Test
    void searchOutboundPayments_computesFeeAndAction() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of(
                outboundRow("1", new BigDecimal("1000.00"), "OUR", "SENT"),
                outboundRow("2", new BigDecimal("1000.00"), "SHA", "ACCEPTED"),
                outboundRow("3", new BigDecimal("1000.00"), "BEN", "REJECTED"),
                outboundRow("4", new BigDecimal("1000.00"), "SHA", "DRAFT")));

        List<Map<String, Object>> result = service.searchOutboundPayments(null, null, null, null, null, null, null);

        assertThat((BigDecimal) result.get(0).get("estimatedFee")).isEqualByComparingTo("20.00");
        assertThat(result.get(0).get("action")).isEqualTo("MONITOR");
        assertThat((BigDecimal) result.get(1).get("estimatedFee")).isEqualByComparingTo("0.50");
        assertThat(result.get(1).get("action")).isNull();
        assertThat((BigDecimal) result.get(2).get("estimatedFee")).isEqualByComparingTo("0.00");
        assertThat(result.get(2).get("action")).isEqualTo("CHECK_REJECT_REASON");
        assertThat(result.get(3).get("action")).isEqualTo("SEND");
    }

    // ---------------- recall search ----------------

    @Test
    void searchRecalls_computesOverdueFlag() {
        Map<String, Object> overdue = recallRow("1", java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(2)), "INITIATED");
        Map<String, Object> done = recallRow("2", java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(2)), "COMPLETED");
        when(jdbc.queryForList(anyString())).thenReturn(List.of(overdue, done));

        List<Map<String, Object>> result = service.searchRecalls(null, null, null, null, null, null, null, null);

        assertThat(result.get(0).get("overdue")).isEqualTo(Boolean.TRUE);
        assertThat((Long) result.get(0).get("daysLeft")).isNegative();
        assertThat(result.get(1).get("overdue")).isEqualTo(Boolean.FALSE);
    }

    // ---------------- details ----------------

    @Test
    void getPaymentDetails_instant() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", "1");
        row.put("end_to_end_id", "E2E-1");
        row.put("amount", new BigDecimal("10.00"));
        row.put("execution_datetime", java.sql.Timestamp.valueOf("2026-10-15 09:15:00"));
        when(jdbc.queryForMap(anyString(), any(Object[].class))).thenReturn(row);

        Map<String, Object> details = service.getPaymentDetails("instant", "1");

        assertThat(details.get("type")).isEqualTo("INSTANT");
        assertThat(details.get("endToEndId")).isEqualTo("E2E-1");
        assertThat(details.get("executionDatetime")).isEqualTo("2026-10-15 09:15:00");
    }

    @Test
    void getPaymentDetails_outbound_computesFee() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", "1");
        row.put("amount", new BigDecimal("1000.00"));
        row.put("charges_type", "OUR");
        when(jdbc.queryForMap(anyString(), any(Object[].class))).thenReturn(row);

        Map<String, Object> details = service.getPaymentDetails("outbound-payment", "1");

        assertThat(details.get("type")).isEqualTo("OUTBOUND");
        assertThat((BigDecimal) details.get("estimatedFee")).isEqualByComparingTo("20.00");
    }

    @Test
    void getPaymentDetails_recall_and_suspended_and_inbound() {
        Map<String, Object> recall = new HashMap<>();
        recall.put("id", "1");
        recall.put("recall_type", "R-COR");
        when(jdbc.queryForMap(anyString(), any(Object[].class))).thenReturn(recall);
        assertThat(service.getPaymentDetails("recall", "1").get("type")).isEqualTo("RECALL");

        Map<String, Object> suspended = new HashMap<>();
        suspended.put("id", "1");
        suspended.put("hold_reason", "MANUAL_REVIEW");
        when(jdbc.queryForMap(anyString(), any(Object[].class))).thenReturn(suspended);
        assertThat(service.getPaymentDetails("suspended", "1").get("type")).isEqualTo("SUSPENDED");

        Map<String, Object> inbound = new HashMap<>();
        inbound.put("id", "1");
        inbound.put("credit_transfer_ref", "CTR-1");
        when(jdbc.queryForMap(anyString(), any(Object[].class))).thenReturn(inbound);
        assertThat(service.getPaymentDetails("inbound", "1").get("type")).isEqualTo("INBOUND");
    }

    @Test
    void getPaymentDetails_unknownType_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.getPaymentDetails("wat", "1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getPaymentDetails_missing_returnsNull() {
        when(jdbc.queryForMap(anyString(), any(Object[].class))).thenThrow(new EmptyResultDataAccessException(1));
        assertThat(service.getPaymentDetails("instant", "nope")).isNull();
    }

    @Test
    void getPaymentDetails_repositoryCrash_throwsRuntimeException() {
        when(jdbc.queryForMap(anyString(), any(Object[].class))).thenThrow(new RuntimeException("boom"));
        assertThatThrownBy(() -> service.getPaymentDetails("instant", "1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("could not load instant payment");
    }

    // ---------------- ingest ----------------

    @Test
    void ingest_instant_persistsRow() {
        service.ingest("pay.instant", """
                {"id":"00000000-0000-0000-0000-000000000901","endToEndId":"E2E-1","uetr":"uetr-1",
                 "amount":42.00,"currency":"EUR","debtorIban":"DE89","debtorName":"ACME",
                 "creditorIban":"FR76","creditorName":"Dupont","executionDateTime":"2026-10-17 10:00:00",
                 "status":"COMPLETED","scheme":"SCT INST"}
                """);
        assertThat(captureExecute())
                .contains("INSERT INTO instant_payments")
                .contains("'E2E-1'")
                .contains("ON CONFLICT (id) DO UPDATE");
    }

    @Test
    void ingest_suspended_handlesNullReleasedAt() {
        service.ingest("pay.suspended", """
                {"id":"00000000-0000-0000-0000-000000000902","amount":5000.00,"currency":"EUR",
                 "debtorIban":"DE89","debtorName":"ACME","creditorIban":"FR76","creditorName":"Dupont",
                 "holdReason":"SANCTIONS_SCREENING","complianceCaseRef":"CASE-1","reviewStatus":"PENDING",
                 "heldAt":"2026-10-17 11:00:00","releasedAt":null}
                """);
        assertThat(captureExecute())
                .contains("INSERT INTO suspended_payments")
                .contains("'CASE-1'")
                .doesNotContain("'null'");
    }

    @Test
    void ingest_inbound_persistsRow() {
        service.ingest("pay.inbound", """
                {"id":"00000000-0000-0000-0000-000000000903","creditTransferRef":"CTR-1","amount":10.00,
                 "currency":"EUR","creditorIban":"DE89","creditorName":"ACME","originBic":"BNPAFRPP",
                 "valueDate":"2026-10-17","settlementDate":"2026-10-17","status":"SETTLED"}
                """);
        assertThat(captureExecute())
                .contains("INSERT INTO inbound_payments")
                .contains("'CTR-1'")
                .contains("'2026-10-17'");
    }

    @Test
    void ingest_outbound_persistsRow() {
        service.ingest("pay.outbound", """
                {"id":"00000000-0000-0000-0000-000000000904","instructionId":"OUT-1","amount":300.00,
                 "currency":"EUR","debtorIban":"DE89","debtorName":"ACME","destinationBic":"ABNANL2A",
                 "requestedExecutionDate":"2026-10-18","chargesType":"SHA","status":"SENT"}
                """);
        assertThat(captureExecute())
                .contains("INSERT INTO outbound_payments")
                .contains("'OUT-1'")
                .contains("'SHA'");
    }

    @Test
    void ingest_recall_persistsRow() {
        service.ingest("pay.recall", """
                {"id":"00000000-0000-0000-0000-000000000905","recallReference":"RCL-1","recallType":"R-COR",
                 "originalPaymentId":"00000000-0000-0000-0000-000000000904","originalPaymentType":"OUTBOUND",
                 "reasonCode":"AC04","amount":300.00,"currency":"EUR","initiatedAt":"2026-10-17 12:00:00",
                 "deadline":"2026-10-19","status":"INITIATED"}
                """);
        assertThat(captureExecute())
                .contains("INSERT INTO recalls")
                .contains("'RCL-1'")
                .contains("'AC04'");
    }

    @Test
    void ingest_unknownTopic_doesNothing() {
        service.ingest("pay.unknown", "{}");
        verify(jdbc, times(0)).execute(anyString());
    }

    @Test
    void ingest_malformedJson_swallowed() {
        service.ingest("pay.instant", "not json");
        verify(jdbc, times(0)).execute(anyString());
        assertThat((List<?>) service.getInternalStats().get("recentErrors")).isNotEmpty();
    }

    // ---------------- legacy export ----------------

    @Test
    void exportLegacyCsv_buildsCsv() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet resultSet = mock(ResultSet.class);
        ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnName(1)).thenReturn("id");
        when(metaData.getColumnName(2)).thenReturn("status");
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString(1)).thenReturn("r1");
        when(resultSet.getString(2)).thenReturn("INITIATED");

        String csv = service.exportLegacyCsv("recall");

        assertThat(csv).contains("id;status;").contains("r1;INITIATED;");
    }

    @Test
    void exportLegacyCsv_databaseFailure_returnsError() throws Exception {
        when(dataSource.getConnection()).thenThrow(new java.sql.SQLException("no connection"));
        assertThat(service.exportLegacyCsv("instant")).isEqualTo("ERROR");
    }

    // ---------------- stats ----------------

    @Test
    void getInternalStats_reportsCountsAndCache() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of());
        service.searchInstantPayments(null, null, null, null, null, null, null, null);

        Map<String, Object> stats = service.getInternalStats();

        assertThat(stats).containsKeys("queryCounts", "cacheSize", "recentErrors");
        assertThat((Integer) stats.get("cacheSize")).isPositive();
        assertThat((Map<String, Object>) stats.get("queryCounts")).containsKey("searchInstantPayments");
    }

    // ---------------- helpers ----------------

    private String captureSql() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jdbc, org.mockito.Mockito.atLeastOnce()).queryForList(captor.capture());
        return captor.getValue();
    }

    private String captureExecute() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).execute(captor.capture());
        return captor.getValue();
    }

    private Map<String, Object> instantRow(String id, BigDecimal amount, String status, java.sql.Timestamp dt) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("amount", amount);
        row.put("status", status);
        row.put("execution_datetime", dt);
        return row;
    }

    private Map<String, Object> suspendedRow(String id, String reviewStatus, java.sql.Timestamp heldAt) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("review_status", reviewStatus);
        row.put("held_at", heldAt);
        return row;
    }

    private Map<String, Object> inboundRow(String id, java.sql.Date valueDate, String status) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("value_date", valueDate);
        row.put("status", status);
        return row;
    }

    private Map<String, Object> outboundRow(String id, BigDecimal amount, String chargesType, String status) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("amount", amount);
        row.put("charges_type", chargesType);
        row.put("status", status);
        return row;
    }

    private Map<String, Object> recallRow(String id, java.sql.Date deadline, String status) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", id);
        row.put("deadline", deadline);
        row.put("status", status);
        return row;
    }
}
