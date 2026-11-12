package fr.kevin.llps.payments.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.kevin.llps.payments.adapter.in.rest.recall.RestRecallController;
import fr.kevin.llps.payments.adapter.in.rest.suspended.RestSuspendedPaymentController;
import fr.kevin.llps.payments.domain.recall.port.in.GetRecallDetails;
import fr.kevin.llps.payments.domain.recall.port.in.SearchRecalls;
import fr.kevin.llps.payments.domain.suspended.port.in.GetSuspendedPaymentDetails;
import fr.kevin.llps.payments.domain.suspended.port.in.SearchSuspendedPayments;
import fr.kevin.llps.payments.dto.InboundPaymentResponse;
import fr.kevin.llps.payments.dto.InstantPaymentResponse;
import fr.kevin.llps.payments.dto.OutboundPaymentResponse;
import fr.kevin.llps.payments.entity.OutboundPayment;
import fr.kevin.llps.payments.service.InboundPaymentService;
import fr.kevin.llps.payments.service.InstantPaymentService;
import fr.kevin.llps.payments.service.OutboundPaymentService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@WebMvcTest(controllers = {
        InstantPaymentController.class,
        RestSuspendedPaymentController.class,
        InboundPaymentController.class,
        OutboundPaymentController.class,
        RestRecallController.class
})
class PaymentControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private InstantPaymentService instantService;
    @MockitoBean
    private SearchSuspendedPayments searchSuspendedPayments;
    @MockitoBean
    private GetSuspendedPaymentDetails getSuspendedPaymentDetails;
    @MockitoBean
    private InboundPaymentService inboundService;
    @MockitoBean
    private OutboundPaymentService outboundService;
    @MockitoBean
    private SearchRecalls searchRecalls;
    @MockitoBean
    private GetRecallDetails getRecallDetails;
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

    // ---------------- characterization: REST error contract (BR-API-01, BR-API-03, BR-API-08) ----------------

    private static final String GenericError = "Something went wrong, contact support with the trace id";

    @ParameterizedTest(name = "[{index}] GET {0}?limit=abc -> 500")
    @ValueSource(strings = {
            "/api/v1/instant-payments/search",
            "/api/v1/suspended-payments/search",
            "/api/v1/inbound-payments/search",
            "/api/v1/outbound-payments/search",
            "/api/v1/recalls/search"
    })
    void should_return_500_with_generic_error_when_limit_is_not_an_integer(String path) throws Exception {
        // GIVEN
        String nonIntegerLimit = "abc";

        // WHEN / THEN
        mvc.perform(get(path).param("limit", nonIntegerLimit))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(GenericError));
        verifyNoInteractions(instantService, searchSuspendedPayments, getSuspendedPaymentDetails, inboundService,
                outboundService, searchRecalls, getRecallDetails);
    }

    @ParameterizedTest(name = "[{index}] POST {0} -> 500")
    @ValueSource(strings = {
            "/api/v1/instant-payments/search",
            "/api/v1/suspended-payments/search",
            "/api/v1/inbound-payments/search",
            "/api/v1/outbound-payments/search",
            "/api/v1/recalls/search"
    })
    void should_return_500_with_generic_error_when_http_method_is_not_get(String path) throws Exception {
        // GIVEN
        String unsupportedMethodPath = path;

        // WHEN / THEN
        mvc.perform(post(unsupportedMethodPath))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(GenericError));
        verifyNoInteractions(instantService, searchSuspendedPayments, getSuspendedPaymentDetails, inboundService,
                outboundService, searchRecalls, getRecallDetails);
    }

    @Test
    void should_return_400_with_exception_message_when_service_rejects_instant_iban() throws Exception {
        // GIVEN
        when(instantService.search(any())).thenThrow(new IllegalArgumentException("Invalid debtor IBAN"));

        // WHEN / THEN
        mvc.perform(get("/api/v1/instant-payments/search").param("debtorIban", "DE1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid debtor IBAN"));
    }

    @Test
    void should_return_generic_error_body_when_service_fails() throws Exception {
        // GIVEN
        when(searchSuspendedPayments.search(any())).thenThrow(new IllegalStateException("boom"));

        // WHEN / THEN
        mvc.perform(get("/api/v1/suspended-payments/search"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(GenericError));
    }

    // ---------------- characterization: Outbound details fee policy (BR-OUT-07, BR-OUT-08, BR-API-05) ----------------

    @ParameterizedTest(name = "[{index}] {1} on {0} -> details fee {2}")
    @CsvSource(value = {
            "1000.00, OUR, 20.00",
            "15600.75, OUR, 312.02",
            "0.25, OUR, 0.01",
            "780.00, SHA, 0.50",
            "95.00, BEN, 0",
            "95.00, XYZ, 0",
            "95.00, NULL, 0"
    }, nullValues = "NULL")
    void should_compute_details_fee_with_controller_fee_table(String amount, String chargesType, String expectedFee)
            throws Exception {
        // GIVEN
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000402");
        OutboundPayment payment = new OutboundPayment();
        payment.id = id;
        payment.amount = new BigDecimal(amount);
        payment.chargesType = chargesType;
        when(em.find(eq(OutboundPayment.class), eq(id))).thenReturn(payment);

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedFee").value(new BigDecimal(expectedFee).doubleValue()));
    }

    @Test
    void should_return_500_when_details_fee_is_computed_for_our_payment_without_amount() throws Exception {
        // GIVEN
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000402");
        OutboundPayment payment = new OutboundPayment();
        payment.id = id;
        payment.amount = null;
        payment.chargesType = "OUR";
        when(em.find(eq(OutboundPayment.class), eq(id))).thenReturn(payment);

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/" + id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(GenericError));
    }

    @Test
    void should_return_fixed_details_fee_when_sha_payment_has_no_amount() throws Exception {
        // GIVEN
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000401");
        OutboundPayment payment = new OutboundPayment();
        payment.id = id;
        payment.amount = null;
        payment.chargesType = "SHA";
        when(em.find(eq(OutboundPayment.class), eq(id))).thenReturn(payment);

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedFee").value(0.50))
                .andExpect(jsonPath("$.amount").value(nullValue()));
    }

    @Test
    void should_return_404_without_querying_when_outbound_details_id_is_not_a_uuid() throws Exception {
        // GIVEN
        String malformedId = "not-a-uuid";

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/" + malformedId))
                .andExpect(status().isNotFound());
        verify(em, never()).find(eq(OutboundPayment.class), any());
        verifyNoInteractions(outboundService);
    }

    @Test
    void should_build_outbound_details_from_entity_manager_and_never_call_the_service() throws Exception {
        // GIVEN
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000403");
        OutboundPayment payment = new OutboundPayment();
        payment.id = id;
        payment.instructionId = "OUT-2026-9003";
        payment.amount = new BigDecimal("95.00");
        payment.chargesType = "BEN";
        when(em.find(eq(OutboundPayment.class), eq(id))).thenReturn(payment);

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instruction_id").value("OUT-2026-9003"))
                .andExpect(jsonPath("$.charges_type").value("BEN"))
                .andExpect(jsonPath("$.instructionId").doesNotExist());
        verify(outboundService, never()).getById(anyString());
        verifyNoInteractions(outboundService);
    }

    // ---------------- characterization: Inbound age in days (BR-INB-07, BR-INB-08) ----------------

    @Test
    void should_return_negative_age_in_days_in_search_when_value_date_is_in_the_future() throws Exception {
        // GIVEN
        InboundPaymentResponse response = new InboundPaymentResponse();
        response.id = "i1";
        response.valueDate = LocalDate.now().plusDays(4).toString();
        when(inboundService.search(any())).thenReturn(List.of(response));

        // WHEN / THEN
        mvc.perform(get("/api/v1/inbound-payments/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ageInDays").value(-4));
    }

    @Test
    void should_return_null_age_in_days_in_search_when_value_date_is_null() throws Exception {
        // GIVEN
        InboundPaymentResponse response = new InboundPaymentResponse();
        response.id = "i1";
        response.valueDate = null;
        when(inboundService.search(any())).thenReturn(List.of(response));

        // WHEN / THEN
        mvc.perform(get("/api/v1/inbound-payments/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ageInDays").value(nullValue()));
    }

    @Test
    void should_return_details_age_in_days_as_computed_by_service_without_recomputing_it() throws Exception {
        // GIVEN
        InboundPaymentResponse response = new InboundPaymentResponse();
        response.id = "i1";
        response.valueDate = LocalDate.now().minusDays(2).toString();
        response.ageInDays = 99L;
        when(inboundService.getById("i1")).thenReturn(response);

        // WHEN / THEN
        mvc.perform(get("/api/v1/inbound-payments/i1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ageInDays").value(99));
    }
}
