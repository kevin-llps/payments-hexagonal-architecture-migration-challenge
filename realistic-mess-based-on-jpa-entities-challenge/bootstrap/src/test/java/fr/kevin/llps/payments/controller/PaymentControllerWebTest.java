package fr.kevin.llps.payments.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.kevin.llps.payments.adapter.in.rest.inbound.RestInboundPaymentController;
import fr.kevin.llps.payments.adapter.in.rest.instant.RestInstantPaymentController;
import fr.kevin.llps.payments.adapter.in.rest.outbound.RestOutboundPaymentController;
import fr.kevin.llps.payments.domain.outbound.FeeNotComputableException;
import fr.kevin.llps.payments.domain.outbound.port.in.GetOutboundPaymentDetails;
import fr.kevin.llps.payments.domain.outbound.port.in.SearchOutboundPayments;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.inbound.port.in.GetInboundPaymentDetails;
import fr.kevin.llps.payments.domain.inbound.port.in.SearchInboundPayments;
import fr.kevin.llps.payments.adapter.in.rest.recall.RestRecallController;
import fr.kevin.llps.payments.domain.instant.port.in.GetInstantPaymentDetails;
import fr.kevin.llps.payments.domain.instant.port.in.SearchInstantPayments;
import fr.kevin.llps.payments.adapter.in.rest.suspended.RestSuspendedPaymentController;
import fr.kevin.llps.payments.domain.recall.port.in.GetRecallDetails;
import fr.kevin.llps.payments.domain.recall.port.in.SearchRecalls;
import fr.kevin.llps.payments.domain.suspended.port.in.GetSuspendedPaymentDetails;
import fr.kevin.llps.payments.domain.suspended.port.in.SearchSuspendedPayments;


@WebMvcTest(controllers = {
        RestInstantPaymentController.class,
        RestSuspendedPaymentController.class,
        RestInboundPaymentController.class,
        RestOutboundPaymentController.class,
        RestRecallController.class
})
class PaymentControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SearchInstantPayments searchInstantPayments;
    @MockitoBean
    private GetInstantPaymentDetails getInstantPaymentDetails;
    @MockitoBean
    private SearchSuspendedPayments searchSuspendedPayments;
    @MockitoBean
    private GetSuspendedPaymentDetails getSuspendedPaymentDetails;
    @MockitoBean
    private SearchInboundPayments searchInboundPayments;
    @MockitoBean
    private GetInboundPaymentDetails getInboundPaymentDetails;
    @MockitoBean
    private SearchOutboundPayments searchOutboundPayments;
    @MockitoBean
    private GetOutboundPaymentDetails getOutboundPaymentDetails;
    @MockitoBean
    private SearchRecalls searchRecalls;
    @MockitoBean
    private GetRecallDetails getRecallDetails;

    // RestOutboundPaymentController renders details timestamps in the zone of the application Clock (BR-OUT-09).
    @TestConfiguration
    static class ParisClock {

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneId.of("Europe/Paris"));
        }
    }

    // ---------------- instant ----------------

    @Test
    void instantSearch_serviceFailure_returns500() throws Exception {
        when(searchInstantPayments.search(any())).thenThrow(new RuntimeException("boom"));

        mvc.perform(get("/api/v1/instant-payments/search"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    // ---------------- outbound ----------------

    @Test
    void outboundDetails_dbFailure_returns500() throws Exception {
        // remapped at IT-06: the details use case fails instead of EntityManager.find
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000401");
        when(getOutboundPaymentDetails.getDetails(id)).thenThrow(new RuntimeException("db down"));

        mvc.perform(get("/api/v1/outbound-payments/" + id.value())).andExpect(status().isInternalServerError());
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
        verifyNoInteractions(searchInstantPayments, getInstantPaymentDetails, searchSuspendedPayments,
                getSuspendedPaymentDetails, searchInboundPayments, getInboundPaymentDetails, searchOutboundPayments, getOutboundPaymentDetails, searchRecalls,
                getRecallDetails);
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
        verifyNoInteractions(searchInstantPayments, getInstantPaymentDetails, searchSuspendedPayments,
                getSuspendedPaymentDetails, searchInboundPayments, getInboundPaymentDetails, searchOutboundPayments, getOutboundPaymentDetails, searchRecalls,
                getRecallDetails);
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

    @Test
    void should_return_500_when_details_fee_is_computed_for_our_payment_without_amount() throws Exception {
        // GIVEN (remapped at IT-06: DetailsFeePolicy raises FeeNotComputableException, no dedicated handler -> generic 500)
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000402");
        when(getOutboundPaymentDetails.getDetails(id)).thenThrow(new FeeNotComputableException("OUR details fee needs an amount"));

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/" + id.value()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(GenericError));
    }

    // ---------------- characterization: Inbound age in days (BR-INB-07, BR-INB-08) ----------------
}
