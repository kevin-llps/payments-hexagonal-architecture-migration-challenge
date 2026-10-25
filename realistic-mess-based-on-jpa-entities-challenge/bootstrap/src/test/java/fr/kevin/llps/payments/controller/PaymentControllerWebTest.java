package fr.kevin.llps.payments.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.kevin.llps.payments.compliancehold.SuspendedPayment;
import fr.kevin.llps.payments.compliancehold.adapter.in.rest.SuspendedPaymentController;
import fr.kevin.llps.payments.compliancehold.port.in.SuspendedPaymentQueries;
import fr.kevin.llps.payments.processing.inbound.InboundPayment;
import fr.kevin.llps.payments.processing.inbound.adapter.in.rest.InboundPaymentController;
import fr.kevin.llps.payments.processing.inbound.port.in.InboundPaymentQueries;
import fr.kevin.llps.payments.processing.inbound.port.in.InboundPaymentView;
import fr.kevin.llps.payments.processing.instant.InstantPayment;
import fr.kevin.llps.payments.processing.instant.adapter.in.rest.InstantPaymentController;
import fr.kevin.llps.payments.processing.instant.port.in.InstantPaymentQueries;
import fr.kevin.llps.payments.processing.outbound.ChargesType;
import fr.kevin.llps.payments.processing.outbound.DetailFeeEstimate;
import fr.kevin.llps.payments.processing.outbound.OutboundPayment;
import fr.kevin.llps.payments.processing.outbound.OutboundPaymentQueryService;
import fr.kevin.llps.payments.processing.outbound.SearchFeeEstimate;
import fr.kevin.llps.payments.processing.outbound.adapter.in.rest.OutboundPaymentController;
import fr.kevin.llps.payments.processing.outbound.port.in.OutboundPaymentQueries;
import fr.kevin.llps.payments.processing.outbound.port.out.OutboundPaymentSearchRepository;
import fr.kevin.llps.payments.recall.adapter.in.rest.RecallController;
import fr.kevin.llps.payments.recall.port.in.RecallQueries;
import fr.kevin.llps.payments.recall.OriginalPaymentRef;
import fr.kevin.llps.payments.recall.Recall;
import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.DomainInvariantViolationException;
import fr.kevin.llps.payments.shared.domain.InvalidSearchFilterException;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@WebMvcTest(controllers = {
        InstantPaymentController.class,
        SuspendedPaymentController.class,
        InboundPaymentController.class,
        OutboundPaymentController.class,
        RecallController.class
})
@Import(PaymentControllerWebTest.OutboundQueriesConfiguration.class)
class PaymentControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private InstantPaymentQueries instantQueries;
    @MockitoBean
    private SuspendedPaymentQueries suspendedQueries;
    @MockitoBean
    private InboundPaymentQueries inboundQueries;
    // outbound: the real query service and both tariffs, only the repository port is mocked, so the fee tables
    // and the details contract stay asserted at the HTTP level
    @MockitoBean
    private OutboundPaymentSearchRepository outboundRepository;
    @MockitoBean
    private RecallQueries recallQueries;
    @MockitoBean
    private EntityManagerFactory entityManagerFactory;
    @MockitoBean
    private EntityManager em;

    @BeforeEach
    void wireEntityManagerFactory() {
        lenient().when(entityManagerFactory.createEntityManager()).thenReturn(em);
    }

    // ---------------- instant ----------------

    private static final String INSTANT_ID = "00000000-0000-0000-0000-000000000101";
    private static final String MISSING_INSTANT_ID = "00000000-0000-0000-0000-000000000199";

    @Test
    void instantSearch_returnsRows() throws Exception {
        when(instantQueries.search(any())).thenReturn(List.of(instant(INSTANT_ID, null)));

        mvc.perform(get("/api/v1/instant-payments/search").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(INSTANT_ID));
    }

    @Test
    void instantSearch_serviceFailure_returns500() throws Exception {
        when(instantQueries.search(any())).thenThrow(new RuntimeException("boom"));

        mvc.perform(get("/api/v1/instant-payments/search"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void instantDetails_foundAndMissing() throws Exception {
        when(instantQueries.get(PaymentId.parse(INSTANT_ID))).thenReturn(Optional.of(instant(INSTANT_ID, null)));
        when(instantQueries.get(PaymentId.parse(MISSING_INSTANT_ID))).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/instant-payments/" + INSTANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INSTANT_ID));
        mvc.perform(get("/api/v1/instant-payments/" + MISSING_INSTANT_ID))
                .andExpect(status().isNotFound());
    }

    // moved from PaymentRepositoryUnitTest.instantSearch_appliesAllFilters_andFormatsExecutionDatetime:
    // rendering is a REST adapter concern (D-02)
    @Test
    void instantSearch_formatsExecutionDatetime() throws Exception {
        when(instantQueries.search(any())).thenReturn(List.of(
                instant(INSTANT_ID, LocalDateTime.of(2026, 10, 15, 9, 15, 0))));

        mvc.perform(get("/api/v1/instant-payments/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].endToEndId").value("E2E-T1"))
                .andExpect(jsonPath("$[0].amount").value(1500.00))
                .andExpect(jsonPath("$[0].executionDatetime").value("2026-10-15T09:15:00"))
                .andExpect(jsonPath("$[0].scheme").value("SCT INST"));
    }

    // moved from PaymentRepositoryUnitTest.instantFindById_foundAndMissing (rendering on details)
    @Test
    void instantDetails_formatsExecutionDatetime() throws Exception {
        when(instantQueries.get(PaymentId.parse(INSTANT_ID))).thenReturn(Optional.of(
                instant(INSTANT_ID, LocalDateTime.of(2026, 10, 15, 9, 15, 0))));

        mvc.perform(get("/api/v1/instant-payments/" + INSTANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionDatetime").value("2026-10-15T09:15:00"));
    }

    private static InstantPayment instant(String id, LocalDateTime executionDatetime) {
        return new InstantPayment(PaymentId.parse(id), "E2E-T1", "uetr-E2E-T1", Money.of(new BigDecimal("1500.00"), "EUR"),
                "DE89370400440532013000", "ACME", "FR7630006000011234567890189", "Dupont", executionDatetime,
                "COMPLETED", "SCT INST", null);
    }

    // ---------------- suspended ----------------

    private static final String SUSPENDED_ID = "00000000-0000-0000-0000-000000000201";
    private static final String MISSING_SUSPENDED_ID = "00000000-0000-0000-0000-000000000299";

    @Test
    void suspendedSearch_returnsRows() throws Exception {
        when(suspendedQueries.search(any())).thenReturn(List.of(suspended(SUSPENDED_ID, null, null)));

        mvc.perform(get("/api/v1/suspended-payments/search").param("reviewStatus", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(SUSPENDED_ID));
    }

    @Test
    void suspendedDetails_foundAndMissing() throws Exception {
        when(suspendedQueries.get(PaymentId.parse(SUSPENDED_ID))).thenReturn(Optional.of(suspended(SUSPENDED_ID, null, null)));
        when(suspendedQueries.get(PaymentId.parse(MISSING_SUSPENDED_ID))).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/suspended-payments/" + SUSPENDED_ID)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/suspended-payments/" + MISSING_SUSPENDED_ID)).andExpect(status().isNotFound());
    }

    // moved from PaymentServiceUnitTest: rendering is a REST adapter concern (D-02)
    @Test
    void suspendedSearch_mapsAllFields() throws Exception {
        when(suspendedQueries.search(any())).thenReturn(List.of(
                suspended(SUSPENDED_ID, LocalDateTime.of(2026, 10, 14, 8, 0, 0), null)));

        mvc.perform(get("/api/v1/suspended-payments/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(SUSPENDED_ID))
                .andExpect(jsonPath("$[0].holdReason").value("AML_THRESHOLD"))
                .andExpect(jsonPath("$[0].heldAt").value("2026-10-14T08:00:00"))
                .andExpect(jsonPath("$[0].releasedAt").value(nullValue()));
    }

    // moved from PaymentServiceUnitTest (suspendedGetById_foundAndMissing): releasedAt rendering on details
    @Test
    void suspendedDetails_formatsReleasedAt() throws Exception {
        when(suspendedQueries.get(PaymentId.parse(SUSPENDED_ID))).thenReturn(Optional.of(
                suspended(SUSPENDED_ID, null, LocalDateTime.of(2026, 10, 13, 15, 45, 0))));

        mvc.perform(get("/api/v1/suspended-payments/" + SUSPENDED_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.releasedAt").value("2026-10-13T15:45:00"));
    }

    private static SuspendedPayment suspended(String id, LocalDateTime heldAt, LocalDateTime releasedAt) {
        return new SuspendedPayment(PaymentId.parse(id), Money.of(new BigDecimal("100.00"), "EUR"), "DE89", "ACME",
                "FR76", "Dupont", "AML_THRESHOLD", "CASE-1", "PENDING", heldAt, releasedAt, null);
    }

    // ---------------- inbound ----------------
    // The age is computed by InboundPaymentQueryService (see PaymentServiceUnitTest); the controller renders it.

    private static final String INBOUND_ID = "00000000-0000-0000-0000-000000000301";
    private static final String OTHER_INBOUND_ID = "00000000-0000-0000-0000-000000000302";
    private static final String MISSING_INBOUND_ID = "00000000-0000-0000-0000-000000000399";

    @Test
    void inboundSearch_computesAgeInDays() throws Exception {
        when(inboundQueries.search(any())).thenReturn(List.of(
                inboundView(INBOUND_ID, LocalDate.now().minusDays(2), null, 2L)));

        mvc.perform(get("/api/v1/inbound-payments/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ageInDays").value(2));
    }

    @Test
    void inboundSearch_futureOrMissingValueDate_ageInDaysNegativeOrNull() throws Exception {
        when(inboundQueries.search(any())).thenReturn(List.of(
                inboundView(INBOUND_ID, LocalDate.now().plusDays(5), null, -5L),
                inboundView(OTHER_INBOUND_ID, null, null, null)));

        mvc.perform(get("/api/v1/inbound-payments/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ageInDays").value(-5))
                .andExpect(jsonPath("$[1].ageInDays").value(nullValue()));
    }

    @Test
    void inboundDetails_foundAndMissing() throws Exception {
        when(inboundQueries.get(PaymentId.parse(INBOUND_ID))).thenReturn(Optional.of(
                inboundView(INBOUND_ID, null, null, null)));
        when(inboundQueries.get(PaymentId.parse(MISSING_INBOUND_ID))).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/inbound-payments/" + INBOUND_ID)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/inbound-payments/" + MISSING_INBOUND_ID)).andExpect(status().isNotFound());
    }

    // moved from PaymentRepositoryUnitTest.inboundSearch_appliesFiltersAndDefaultLimit (rendering, D-02)
    @Test
    void inboundSearch_formatsValueAndSettlementDates() throws Exception {
        when(inboundQueries.search(any())).thenReturn(List.of(
                inboundView(INBOUND_ID, LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15), 10L)));

        mvc.perform(get("/api/v1/inbound-payments/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].creditTransferRef").value("CTR-1"))
                .andExpect(jsonPath("$[0].valueDate").value("2026-10-15"))
                .andExpect(jsonPath("$[0].settlementDate").value("2026-10-15"));
    }

    // moved from PaymentRepositoryUnitTest.inboundFindById_foundAndMissing (rendering on details)
    @Test
    void inboundDetails_formatsDates() throws Exception {
        when(inboundQueries.get(PaymentId.parse(INBOUND_ID))).thenReturn(Optional.of(
                inboundView(INBOUND_ID, LocalDate.of(2026, 10, 17), null, 8L)));

        mvc.perform(get("/api/v1/inbound-payments/" + INBOUND_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valueDate").value("2026-10-17"))
                .andExpect(jsonPath("$.settlementDate").value(nullValue()));
    }

    private static InboundPaymentView inboundView(String id, LocalDate valueDate, LocalDate settlementDate,
                                                  Long ageInDays) {
        return new InboundPaymentView(new InboundPayment(PaymentId.parse(id), "CTR-1",
                Money.of(new BigDecimal("3400.00"), "EUR"), "DE89370400440532013000", "ACME", "BNPAFRPP", valueDate,
                settlementDate, "SETTLED", null), ageInDays);
    }

    // ---------------- outbound ----------------

    @TestConfiguration
    static class OutboundQueriesConfiguration {

        @Bean
        OutboundPaymentQueries outboundPaymentQueries(OutboundPaymentSearchRepository repository) {
            return new OutboundPaymentQueryService(repository, new SearchFeeEstimate(), new DetailFeeEstimate());
        }
    }

    @Test
    void outboundSearch_returnsRows() throws Exception {
        when(outboundRepository.search(any())).thenReturn(List.of(
                outbound("00000000-0000-0000-0000-000000000401", "1000.00", "OUR")));

        mvc.perform(get("/api/v1/outbound-payments/search").param("chargesType", "OUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("00000000-0000-0000-0000-000000000401"));
    }

    // moved from PaymentServiceUnitTest.outboundGetById_mapsRequestedExecutionDateAsIsoDate: that service method
    // was reachable from no endpoint; the same rendering and search fee are asserted on the search endpoint
    @Test
    void outboundSearch_mapsRequestedExecutionDateAsIsoDate() throws Exception {
        when(outboundRepository.search(any())).thenReturn(List.of(new OutboundPayment(
                PaymentId.parse("00000000-0000-0000-0000-000000000402"), null,
                Money.of(new BigDecimal("15600.75"), "EUR"), null, null, null, LocalDate.of(2026, 10, 17),
                new ChargesType("OUR"), "SENT", null)));

        mvc.perform(get("/api/v1/outbound-payments/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestedExecutionDate").value("2026-10-17"))
                .andExpect(jsonPath("$[0].estimatedFee").value(156.01));
    }

    @Test
    void outboundDetails_computesFeeFromController() throws Exception {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000401");
        when(outboundRepository.findById(id)).thenReturn(Optional.of(new OutboundPayment(id, null,
                Money.of(new BigDecimal("1000.00"), null), null, null, null, null, new ChargesType("OUR"), null,
                new AuditTimestamps(LocalDateTime.of(2026, 10, 16, 8, 0, 0), LocalDateTime.of(2026, 10, 16, 8, 1, 0)))));

        mvc.perform(get("/api/v1/outbound-payments/" + id.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedFee").value(20.00));
    }

    @ParameterizedTest(name = "[{index}] chargesType={0}, amount={1} -> detail fee {2}")
    @CsvSource({
            "OUR, 1000.00, 20.00",
            "OUR, 15600.75, 312.02",
            "SHA, 1000.00, 0.50",
            "BEN, 1000.00, 0",
            "XYZ, 1000.00, 0",
            ", 1000.00, 0"
    })
    void outboundDetails_feeEstimate_followsDetailFeeTable(String chargesType, String amount, double expectedFee)
            throws Exception {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000402");
        when(outboundRepository.findById(id)).thenReturn(Optional.of(outbound(id.value().toString(), amount, chargesType)));

        mvc.perform(get("/api/v1/outbound-payments/" + id.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedFee").value(expectedFee));
    }

    // D-01: a stored payment without amount can no longer be reconstituted (the JPA mapper raises this exception);
    // the response stays a 500
    @Test
    void outboundDetails_ourWithoutAmount_returns500() throws Exception {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000402");
        when(outboundRepository.findById(id)).thenThrow(new DomainInvariantViolationException("Money amount is mandatory"));

        mvc.perform(get("/api/v1/outbound-payments/" + id.value())).andExpect(status().isInternalServerError());
    }

    @Test
    void outboundDetails_returnsSnakeCasePayloadWithAuditTimestamps() throws Exception {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000401");
        when(outboundRepository.findById(id)).thenReturn(Optional.of(new OutboundPayment(id, "OUT-2026-9001",
                Money.of(new BigDecimal("780.00"), "EUR"), "DE89370400440532013000", "ACME GmbH", "BNPAFRPP",
                LocalDate.of(2026, 10, 16), new ChargesType("SHA"), "SENT",
                new AuditTimestamps(LocalDateTime.of(2026, 10, 16, 8, 0, 0), LocalDateTime.of(2026, 10, 16, 8, 1, 0)))));

        mvc.perform(get("/api/v1/outbound-payments/" + id.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.value().toString()))
                .andExpect(jsonPath("$.instruction_id").value("OUT-2026-9001"))
                .andExpect(jsonPath("$.debtor_iban").value("DE89370400440532013000"))
                .andExpect(jsonPath("$.debtor_name").value("ACME GmbH"))
                .andExpect(jsonPath("$.destination_bic").value("BNPAFRPP"))
                .andExpect(jsonPath("$.requested_execution_date").exists())
                .andExpect(jsonPath("$.charges_type").value("SHA"))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.created_at").exists())
                .andExpect(jsonPath("$.updated_at").exists())
                .andExpect(jsonPath("$.estimatedFee").value(0.50))
                .andExpect(jsonPath("$.instructionId").doesNotExist())
                .andExpect(jsonPath("$.estimated_fee").doesNotExist());
    }

    @Test
    void outboundDetails_malformedId_returns404WithoutQuerying() throws Exception {
        mvc.perform(get("/api/v1/outbound-payments/not-a-uuid")).andExpect(status().isNotFound());

        verifyNoInteractions(outboundRepository);
    }

    @Test
    void outboundDetails_missing_returns404() throws Exception {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000404");
        when(outboundRepository.findById(id)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/outbound-payments/" + id.value())).andExpect(status().isNotFound());
    }

    @Test
    void outboundDetails_dbFailure_returns500() throws Exception {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000401");
        when(outboundRepository.findById(id)).thenThrow(new RuntimeException("db down"));

        mvc.perform(get("/api/v1/outbound-payments/" + id.value())).andExpect(status().isInternalServerError());
    }

    private static OutboundPayment outbound(String id, String amount, String chargesType) {
        return new OutboundPayment(PaymentId.parse(id), null, Money.of(new BigDecimal(amount), null), null, null, null,
                null, new ChargesType(chargesType), null, null);
    }

    // ---------------- recalls ----------------

    private static final String RECALL_ID = "00000000-0000-0000-0000-000000000501";
    private static final String MISSING_RECALL_ID = "00000000-0000-0000-0000-000000000599";

    @Test
    void recallsSearch_returnsRows() throws Exception {
        when(recallQueries.search(any())).thenReturn(List.of(recall(RECALL_ID)));

        mvc.perform(get("/api/v1/recalls/search").param("recallType", "R-COR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(RECALL_ID));
    }

    @Test
    void recallsSearch_invalidType_returns400() throws Exception {
        when(recallQueries.search(any())).thenThrow(new InvalidSearchFilterException("Unknown recall type: NOPE"));

        mvc.perform(get("/api/v1/recalls/search").param("recallType", "NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown recall type: NOPE"));
    }

    @Test
    void recallsDetails_foundAndMissing() throws Exception {
        when(recallQueries.get(PaymentId.parse(RECALL_ID))).thenReturn(Optional.of(recall(RECALL_ID)));
        when(recallQueries.get(PaymentId.parse(MISSING_RECALL_ID))).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/recalls/" + RECALL_ID)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/recalls/" + MISSING_RECALL_ID)).andExpect(status().isNotFound());
    }

    // moved from PaymentServiceUnitTest: rendering is a REST adapter concern (D-02)
    @Test
    void recallSearch_mapsFieldsAndFormatsDates() throws Exception {
        when(recallQueries.search(any())).thenReturn(List.of(new Recall(
                PaymentId.parse(RECALL_ID), "RCL-1", "R-COR",
                new OriginalPaymentRef(PaymentId.parse("00000000-0000-0000-0000-000000000401"), "OUTBOUND"),
                "AC04", new BigDecimal("300.00"), "EUR", LocalDateTime.of(2026, 10, 16, 12, 0, 0),
                LocalDate.of(2026, 10, 18), "INITIATED", null)));

        mvc.perform(get("/api/v1/recalls/search").param("recallType", "r-cor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].initiatedAt").value("2026-10-16T12:00:00"))
                .andExpect(jsonPath("$[0].deadline").value("2026-10-18"))
                .andExpect(jsonPath("$[0].reasonCode").value("AC04"))
                .andExpect(jsonPath("$[0].originalPaymentId").value("00000000-0000-0000-0000-000000000401"));
    }

    private static Recall recall(String id) {
        return new Recall(PaymentId.parse(id), null, null, new OriginalPaymentRef(null, null), null, null, null,
                null, null, null, null);
    }
}
