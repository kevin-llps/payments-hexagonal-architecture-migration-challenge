package fr.kevin.llps.payments.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.kevin.llps.payments.dto.InboundPaymentResponse;
import fr.kevin.llps.payments.dto.OutboundPaymentResponse;
import fr.kevin.llps.payments.compliancehold.SuspendedPayment;
import fr.kevin.llps.payments.compliancehold.adapter.in.rest.SuspendedPaymentController;
import fr.kevin.llps.payments.compliancehold.port.in.SuspendedPaymentQueries;
import fr.kevin.llps.payments.entity.OutboundPayment;
import fr.kevin.llps.payments.processing.instant.InstantPayment;
import fr.kevin.llps.payments.processing.instant.adapter.in.rest.InstantPaymentController;
import fr.kevin.llps.payments.processing.instant.port.in.InstantPaymentQueries;
import fr.kevin.llps.payments.recall.adapter.in.rest.RecallController;
import fr.kevin.llps.payments.recall.port.in.RecallQueries;
import fr.kevin.llps.payments.recall.OriginalPaymentRef;
import fr.kevin.llps.payments.recall.Recall;
import fr.kevin.llps.payments.service.InboundPaymentService;
import fr.kevin.llps.payments.service.OutboundPaymentService;
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
class PaymentControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private InstantPaymentQueries instantQueries;
    @MockitoBean
    private SuspendedPaymentQueries suspendedQueries;
    @MockitoBean
    private InboundPaymentService inboundService;
    @MockitoBean
    private OutboundPaymentService outboundService;
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

    @Test
    void inboundSearch_computesAgeInDays() throws Exception {
        InboundPaymentResponse response = new InboundPaymentResponse();
        response.id = "i1";
        response.valueDate = LocalDate.now().minusDays(2).toString();
        when(inboundService.search(any())).thenReturn(List.of(response));

        mvc.perform(get("/api/v1/inbound-payments/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ageInDays").value(2));
    }

    @Test
    void inboundSearch_futureOrMissingValueDate_ageInDaysNegativeOrNull() throws Exception {
        InboundPaymentResponse future = new InboundPaymentResponse();
        future.id = "i1";
        future.valueDate = LocalDate.now().plusDays(5).toString();
        InboundPaymentResponse undated = new InboundPaymentResponse();
        undated.id = "i2";
        when(inboundService.search(any())).thenReturn(List.of(future, undated));

        mvc.perform(get("/api/v1/inbound-payments/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ageInDays").value(-5))
                .andExpect(jsonPath("$[1].ageInDays").value(nullValue()));
    }

    @Test
    void inboundDetails_foundAndMissing() throws Exception {
        InboundPaymentResponse response = new InboundPaymentResponse();
        response.id = "i1";
        when(inboundService.getById("i1")).thenReturn(response);
        when(inboundService.getById("missing")).thenReturn(null);

        mvc.perform(get("/api/v1/inbound-payments/i1")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/inbound-payments/missing")).andExpect(status().isNotFound());
    }

    // ---------------- outbound ----------------

    @Test
    void outboundSearch_returnsRows() throws Exception {
        OutboundPaymentResponse response = new OutboundPaymentResponse();
        response.id = "o1";
        when(outboundService.search(any())).thenReturn(List.of(response));

        mvc.perform(get("/api/v1/outbound-payments/search").param("chargesType", "OUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("o1"));
    }

    @Test
    void outboundDetails_computesFeeFromController() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000401");
        OutboundPayment payment = new OutboundPayment();
        payment.id = id;
        payment.amount = new BigDecimal("1000.00");
        payment.chargesType = "OUR";
        payment.createdAt = Timestamp.valueOf("2026-10-16 08:00:00");
        payment.updatedAt = Timestamp.valueOf("2026-10-16 08:01:00");
        when(em.find(eq(OutboundPayment.class), eq(id))).thenReturn(payment);

        mvc.perform(get("/api/v1/outbound-payments/" + id))
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
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000402");
        OutboundPayment payment = new OutboundPayment();
        payment.id = id;
        payment.amount = new BigDecimal(amount);
        payment.chargesType = chargesType;
        when(em.find(eq(OutboundPayment.class), eq(id))).thenReturn(payment);

        mvc.perform(get("/api/v1/outbound-payments/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedFee").value(expectedFee));
    }

    @Test
    void outboundDetails_ourWithoutAmount_returns500() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000402");
        OutboundPayment payment = new OutboundPayment();
        payment.id = id;
        payment.chargesType = "OUR";
        when(em.find(eq(OutboundPayment.class), eq(id))).thenReturn(payment);

        mvc.perform(get("/api/v1/outbound-payments/" + id)).andExpect(status().isInternalServerError());
    }

    @Test
    void outboundDetails_returnsSnakeCasePayloadWithAuditTimestamps() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000401");
        OutboundPayment payment = new OutboundPayment();
        payment.id = id;
        payment.instructionId = "OUT-2026-9001";
        payment.amount = new BigDecimal("780.00");
        payment.currency = "EUR";
        payment.debtorIban = "DE89370400440532013000";
        payment.debtorName = "ACME GmbH";
        payment.destinationBic = "BNPAFRPP";
        payment.requestedExecutionDate = java.sql.Date.valueOf("2026-10-16");
        payment.chargesType = "SHA";
        payment.status = "SENT";
        payment.createdAt = Timestamp.valueOf("2026-10-16 08:00:00");
        payment.updatedAt = Timestamp.valueOf("2026-10-16 08:01:00");
        when(em.find(eq(OutboundPayment.class), eq(id))).thenReturn(payment);

        mvc.perform(get("/api/v1/outbound-payments/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
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

        verifyNoInteractions(em);
    }

    @Test
    void outboundDetails_missing_returns404() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000404");
        when(em.find(eq(OutboundPayment.class), eq(id))).thenReturn(null);

        mvc.perform(get("/api/v1/outbound-payments/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void outboundDetails_dbFailure_returns500() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000401");
        when(em.find(eq(OutboundPayment.class), eq(id))).thenThrow(new RuntimeException("db down"));

        mvc.perform(get("/api/v1/outbound-payments/" + id)).andExpect(status().isInternalServerError());
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
