package fr.kevin.llps.payments.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

import fr.kevin.llps.payments.dto.InboundPaymentResponse;
import fr.kevin.llps.payments.dto.InboundPaymentSearchRequest;
import fr.kevin.llps.payments.dto.InstantPaymentResponse;
import fr.kevin.llps.payments.dto.InstantPaymentSearchRequest;
import fr.kevin.llps.payments.dto.OutboundPaymentSearchRequest;
import fr.kevin.llps.payments.dto.RecallSearchRequest;
import fr.kevin.llps.payments.dto.SuspendedPaymentSearchRequest;
import fr.kevin.llps.payments.model.OutboundPaymentRow;
import fr.kevin.llps.payments.model.RecallRow;
import fr.kevin.llps.payments.model.SuspendedPaymentRow;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentRepositoryUnitTest {

    @Mock
    private JdbcTemplate jdbc;

    private InstantPaymentRepository instantRepository;
    private SuspendedPaymentRepository suspendedRepository;
    private InboundPaymentRepository inboundRepository;
    private OutboundPaymentRepository outboundRepository;
    private RecallRepository recallRepository;

    @BeforeEach
    void setUp() {
        instantRepository = new InstantPaymentRepository();
        suspendedRepository = new SuspendedPaymentRepository();
        inboundRepository = new InboundPaymentRepository();
        outboundRepository = new OutboundPaymentRepository();
        recallRepository = new RecallRepository();
        ReflectionTestUtils.setField(instantRepository, "jdbc", jdbc);
        ReflectionTestUtils.setField(suspendedRepository, "jdbc", jdbc);
        ReflectionTestUtils.setField(inboundRepository, "jdbc", jdbc);
        ReflectionTestUtils.setField(outboundRepository, "jdbc", jdbc);
        ReflectionTestUtils.setField(recallRepository, "jdbc", jdbc);
    }

    // ---------------- instant ----------------

    @Test
    void instantSearch_buildsAllFilterClauses() {
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(Collections.emptyList());
        InstantPaymentSearchRequest request = new InstantPaymentSearchRequest();
        request.endToEndId = "E2E-1";
        request.debtorIban = "de89";
        request.creditorIban = "fr76";
        request.status = "completed";
        request.currency = "eur";
        request.executionFrom = "2026-10-01T00:00:00";
        request.executionTo = "2026-10-31T00:00:00";
        request.limit = 10;

        instantRepository.search(request);

        assertThat(captureTwoArgSql())
                .contains("FROM instant_payments")
                .contains("end_to_end_id = 'E2E-1'")
                .contains("UPPER(debtor_iban) LIKE '%DE89%'")
                .contains("UPPER(creditor_iban) LIKE '%FR76%'")
                .contains("status = 'COMPLETED'")
                .contains("currency = 'EUR'")
                .contains("execution_datetime >= '2026-10-01T00:00:00'")
                .contains("execution_datetime <= '2026-10-31T00:00:00 23:59:59'")
                .contains("LIMIT 10");
    }

    @Test
    void instantSearch_noFilters_usesDefaultLimit() {
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(Collections.emptyList());

        instantRepository.search(new InstantPaymentSearchRequest());

        assertThat(captureTwoArgSql()).contains("LIMIT 50");
    }

    @Test
    void instantSearch_mapsRowToResponse() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("id")).thenReturn("id-1");
        when(rs.getString("end_to_end_id")).thenReturn("E2E-1");
        when(rs.getString("uetr")).thenReturn("uetr-1");
        when(rs.getBigDecimal("amount")).thenReturn(new BigDecimal("42.00"));
        when(rs.getString("currency")).thenReturn("EUR");
        when(rs.getString("debtor_iban")).thenReturn("DE89");
        when(rs.getString("debtor_name")).thenReturn("ACME");
        when(rs.getString("creditor_iban")).thenReturn("FR76");
        when(rs.getString("creditor_name")).thenReturn("Dupont");
        when(rs.getTimestamp("execution_datetime")).thenReturn(Timestamp.valueOf("2026-10-15 09:15:00"));
        when(rs.getString("status")).thenReturn("COMPLETED");
        when(rs.getString("scheme")).thenReturn("SCT INST");
        when(jdbc.query(anyString(), any(RowMapper.class))).thenAnswer(invocation -> {
            RowMapper<InstantPaymentResponse> rowMapper = invocation.getArgument(1);
            return List.of(rowMapper.mapRow(rs, 0));
        });

        InstantPaymentResponse response = instantRepository.search(new InstantPaymentSearchRequest()).get(0);

        assertThat(response.id).isEqualTo("id-1");
        assertThat(response.endToEndId).isEqualTo("E2E-1");
        assertThat(response.amount).isEqualByComparingTo("42.00");
        assertThat(response.executionDatetime).isEqualTo("2026-10-15T09:15:00");
        assertThat(response.scheme).isEqualTo("SCT INST");
    }

    @Test
    void instantFindById_mapsRow() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("id")).thenReturn("id-1");
        when(rs.getBigDecimal("amount")).thenReturn(new BigDecimal("1.00"));
        when(rs.getTimestamp("execution_datetime")).thenReturn(Timestamp.valueOf("2026-10-15 09:15:00"));
        when(jdbc.queryForObject(anyString(), any(Object[].class), any(RowMapper.class))).thenAnswer(invocation -> {
            RowMapper<InstantPaymentResponse> rowMapper = invocation.getArgument(2);
            return rowMapper.mapRow(rs, 0);
        });

        assertThat(instantRepository.findById("id-1").id).isEqualTo("id-1");
    }

    @Test
    void instantFindById_missing_returnsNull() {
        when(jdbc.queryForObject(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));
        assertThat(instantRepository.findById("nope")).isNull();
    }

    // ---------------- suspended ----------------

    @Test
    void suspendedSearch_buildsParameterizedClauses() {
        when(jdbc.query(anyString(), any(Object[].class), any(RowMapper.class))).thenReturn(Collections.emptyList());
        SuspendedPaymentSearchRequest request = new SuspendedPaymentSearchRequest();
        request.debtorIban = "DE89";
        request.holdReason = "aml_threshold";
        request.reviewStatus = "pending";
        request.heldFrom = "2026-10-01T00:00:00";
        request.heldTo = "2026-10-31T00:00:00";
        request.limit = 5;

        suspendedRepository.search(request);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).query(sqlCaptor.capture(), argsCaptor.capture(), any(RowMapper.class));
        assertThat(sqlCaptor.getValue())
                .contains("debtor_iban = ?")
                .contains("hold_reason = ?")
                .contains("review_status = ?")
                .contains("held_at >= ?")
                .contains("held_at <= ?")
                .contains("LIMIT 5");
        assertThat(argsCaptor.getValue()).contains("DE89", "AML_THRESHOLD", "PENDING");
    }

    @Test
    void suspendedFindById_mapsRowAndHandlesMissing() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("id")).thenReturn("s1");
        when(rs.getBigDecimal("amount")).thenReturn(new BigDecimal("10.00"));
        when(rs.getTimestamp("held_at")).thenReturn(Timestamp.valueOf("2026-10-14 08:00:00"));
        when(jdbc.queryForObject(anyString(), any(Object[].class), any(RowMapper.class))).thenAnswer(invocation -> {
            RowMapper<SuspendedPaymentRow> rowMapper = invocation.getArgument(2);
            return rowMapper.mapRow(rs, 0);
        });

        SuspendedPaymentRow row = suspendedRepository.findById("s1");
        assertThat(row.id).isEqualTo("s1");
        assertThat(row.heldAt).isEqualTo(Timestamp.valueOf("2026-10-14 08:00:00"));

        when(jdbc.queryForObject(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));
        assertThat(suspendedRepository.findById("nope")).isNull();
    }

    // ---------------- inbound ----------------

    @Test
    void inboundSearch_buildsClausesAndDefaultLimit() {
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(Collections.emptyList());
        InboundPaymentSearchRequest request = new InboundPaymentSearchRequest();
        request.creditorIban = "DE89";
        request.originBic = "bnpa";
        request.status = "settled";
        request.currency = "eur";
        request.valueDateFrom = "2026-10-01";
        request.valueDateTo = "2026-10-31";

        inboundRepository.search(request);

        assertThat(captureTwoArgSql())
                .contains("creditor_iban = 'DE89'")
                .contains("UPPER(origin_bic) LIKE '%BNPA%'")
                .contains("status = 'SETTLED'")
                .contains("value_date >= '2026-10-01'")
                .contains("value_date <= '2026-10-31'")
                .contains("LIMIT 50");
    }

    @Test
    void inboundFindById_mapsRowAndHandlesMissing() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("id")).thenReturn("i1");
        when(rs.getDate("value_date")).thenReturn(java.sql.Date.valueOf("2026-10-17"));
        when(rs.getDate("settlement_date")).thenReturn(java.sql.Date.valueOf("2026-10-17"));
        when(jdbc.queryForObject(anyString(), any(Object[].class), any(RowMapper.class))).thenAnswer(invocation -> {
            RowMapper<InboundPaymentResponse> rowMapper = invocation.getArgument(2);
            return rowMapper.mapRow(rs, 0);
        });

        InboundPaymentResponse response = inboundRepository.findById("i1");
        assertThat(response.valueDate).isEqualTo("2026-10-17");
        assertThat(response.settlementDate).isEqualTo("2026-10-17");

        when(jdbc.queryForObject(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));
        assertThat(inboundRepository.findById("nope")).isNull();
    }

    // ---------------- outbound ----------------

    @Test
    void outboundSearch_buildsParameterizedClauses() {
        when(jdbc.query(anyString(), any(Object[].class), any(RowMapper.class))).thenReturn(Collections.emptyList());
        OutboundPaymentSearchRequest request = new OutboundPaymentSearchRequest();
        request.debtorIban = "DE89";
        request.destinationBic = "abna";
        request.chargesType = "sha";
        request.status = "sent";
        request.executionFrom = "2026-10-01";
        request.executionTo = "2026-10-31";
        request.limit = 20;

        outboundRepository.search(request);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> argsCaptor = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).query(sqlCaptor.capture(), argsCaptor.capture(), any(RowMapper.class));
        assertThat(sqlCaptor.getValue())
                .contains("debtor_iban = ?")
                .contains("destination_bic LIKE ?")
                .contains("charges_type = ?")
                .contains("status = ?")
                .contains("requested_execution_date >= ?")
                .contains("LIMIT 20");
        assertThat(argsCaptor.getValue()).contains("DE89", "%ABNA%", "SHA", "SENT");
    }

    @Test
    void outboundFindById_mapsRowAndHandlesMissing() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("id")).thenReturn("o1");
        when(rs.getBigDecimal("amount")).thenReturn(new BigDecimal("300.00"));
        when(rs.getDate("requested_execution_date")).thenReturn(java.sql.Date.valueOf("2026-10-18"));
        when(jdbc.queryForObject(anyString(), any(Object[].class), any(RowMapper.class))).thenAnswer(invocation -> {
            RowMapper<OutboundPaymentRow> rowMapper = invocation.getArgument(2);
            return rowMapper.mapRow(rs, 0);
        });

        OutboundPaymentRow row = outboundRepository.findById("o1");
        assertThat(row.id).isEqualTo("o1");
        assertThat(row.requestedExecutionDate).isEqualTo(java.sql.Date.valueOf("2026-10-18"));

        when(jdbc.queryForObject(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));
        assertThat(outboundRepository.findById("nope")).isNull();
    }

    // ---------------- recalls ----------------

    @Test
    void recallSearch_alwaysUsesHardcodedLimit() {
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(Collections.emptyList());
        RecallSearchRequest request = new RecallSearchRequest();
        request.recallType = "r-cor";
        request.originalPaymentId = "00000000-0000-0000-0000-000000000401";
        request.status = "initiated";
        request.limit = 5;

        recallRepository.search(request);

        String sql = captureTwoArgSql();
        assertThat(sql)
                .contains("recall_type = 'R-COR'")
                .contains("original_payment_id = '00000000-0000-0000-0000-000000000401'")
                .contains("status = 'INITIATED'")
                .contains("LIMIT 50");
        assertThat(sql).doesNotContain("LIMIT 5 ");
    }

    @Test
    void recallFindById_mapsRowAndHandlesMissing() throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("id")).thenReturn("r1");
        when(rs.getString("recall_type")).thenReturn("R-COR");
        when(rs.getDate("deadline")).thenReturn(java.sql.Date.valueOf("2026-10-18"));
        when(jdbc.queryForObject(anyString(), any(Object[].class), any(RowMapper.class))).thenAnswer(invocation -> {
            RowMapper<RecallRow> rowMapper = invocation.getArgument(2);
            return rowMapper.mapRow(rs, 0);
        });

        RecallRow row = recallRepository.findById("r1");
        assertThat(row.recallType).isEqualTo("R-COR");
        assertThat(row.deadline).isEqualTo(java.sql.Date.valueOf("2026-10-18"));

        when(jdbc.queryForObject(anyString(), any(Object[].class), any(RowMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));
        assertThat(recallRepository.findById("nope")).isNull();
    }

    // ---------------- helpers ----------------

    @SuppressWarnings("unchecked")
    private String captureTwoArgSql() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(captor.capture(), any(RowMapper.class));
        return captor.getValue();
    }
}
