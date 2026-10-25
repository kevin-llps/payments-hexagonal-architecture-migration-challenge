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
import java.util.List;
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
import fr.kevin.llps.payments.dto.InstantPaymentResponse;
import fr.kevin.llps.payments.dto.OutboundPaymentResponse;
import fr.kevin.llps.payments.dto.RecallResponse;
import fr.kevin.llps.payments.dto.SuspendedPaymentResponse;
import fr.kevin.llps.payments.entity.OutboundPayment;
import fr.kevin.llps.payments.service.InboundPaymentService;
import fr.kevin.llps.payments.service.InstantPaymentService;
import fr.kevin.llps.payments.service.OutboundPaymentService;
import fr.kevin.llps.payments.service.RecallService;
import fr.kevin.llps.payments.service.SuspendedPaymentService;

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
    private InstantPaymentService instantService;
    @MockitoBean
    private SuspendedPaymentService suspendedService;
    @MockitoBean
    private InboundPaymentService inboundService;
    @MockitoBean
    private OutboundPaymentService outboundService;
    @MockitoBean
    private RecallService recallService;
    @MockitoBean
    private EntityManagerFactory entityManagerFactory;
    @MockitoBean
    private EntityManager em;

    @BeforeEach
    void wireEntityManagerFactory() {
        lenient().when(entityManagerFactory.createEntityManager()).thenReturn(em);
    }

    // ---------------- instant ----------------

    @Test
    void instantSearch_returnsRows() throws Exception {
        InstantPaymentResponse response = new InstantPaymentResponse();
        response.id = "1";
        when(instantService.search(any())).thenReturn(List.of(response));

        mvc.perform(get("/api/v1/instant-payments/search").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("1"));
    }

    @Test
    void instantSearch_serviceFailure_returns500() throws Exception {
        when(instantService.search(any())).thenThrow(new RuntimeException("boom"));

        mvc.perform(get("/api/v1/instant-payments/search"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void instantDetails_foundAndMissing() throws Exception {
        InstantPaymentResponse response = new InstantPaymentResponse();
        response.id = "1";
        when(instantService.getById("1")).thenReturn(response);
        when(instantService.getById("missing")).thenReturn(null);

        mvc.perform(get("/api/v1/instant-payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("1"));
        mvc.perform(get("/api/v1/instant-payments/missing"))
                .andExpect(status().isNotFound());
    }

    // ---------------- suspended ----------------

    @Test
    void suspendedSearch_returnsRows() throws Exception {
        SuspendedPaymentResponse response = new SuspendedPaymentResponse();
        response.id = "s1";
        when(suspendedService.search(any())).thenReturn(List.of(response));

        mvc.perform(get("/api/v1/suspended-payments/search").param("reviewStatus", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("s1"));
    }

    @Test
    void suspendedDetails_foundAndMissing() throws Exception {
        SuspendedPaymentResponse response = new SuspendedPaymentResponse();
        response.id = "s1";
        when(suspendedService.getById("s1")).thenReturn(response);
        when(suspendedService.getById("missing")).thenReturn(null);

        mvc.perform(get("/api/v1/suspended-payments/s1")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/suspended-payments/missing")).andExpect(status().isNotFound());
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

    @Test
    void recallsSearch_returnsRows() throws Exception {
        RecallResponse response = new RecallResponse();
        response.id = "r1";
        when(recallService.search(any())).thenReturn(List.of(response));

        mvc.perform(get("/api/v1/recalls/search").param("recallType", "R-COR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("r1"));
    }

    @Test
    void recallsSearch_invalidType_returns400() throws Exception {
        when(recallService.search(any())).thenThrow(new IllegalArgumentException("Unknown recall type: NOPE"));

        mvc.perform(get("/api/v1/recalls/search").param("recallType", "NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unknown recall type: NOPE"));
    }

    @Test
    void recallsDetails_foundAndMissing() throws Exception {
        RecallResponse response = new RecallResponse();
        response.id = "r1";
        when(recallService.getById("r1")).thenReturn(response);
        when(recallService.getById("missing")).thenReturn(null);

        mvc.perform(get("/api/v1/recalls/r1")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/recalls/missing")).andExpect(status().isNotFound());
    }
}
