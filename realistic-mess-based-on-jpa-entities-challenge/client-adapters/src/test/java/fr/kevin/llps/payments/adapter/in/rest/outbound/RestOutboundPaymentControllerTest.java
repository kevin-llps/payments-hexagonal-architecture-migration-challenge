package fr.kevin.llps.payments.adapter.in.rest.outbound;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.kevin.llps.payments.domain.outbound.OutboundPayment;
import fr.kevin.llps.payments.domain.outbound.OutboundPaymentWithFee;
import fr.kevin.llps.payments.domain.outbound.port.in.GetOutboundPaymentDetails;
import fr.kevin.llps.payments.domain.outbound.port.in.SearchOutboundPayments;
import fr.kevin.llps.payments.domain.outbound.query.OutboundPaymentSearchQuery;
import fr.kevin.llps.payments.domain.sharedkernel.MalformedDateException;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.NegativeSearchLimitException;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RestOutboundPaymentController.class)
class RestOutboundPaymentControllerTest {

    private static final PaymentId seededId = PaymentId.parse("00000000-0000-0000-0000-000000000401");

    @TestConfiguration
    static class ParisClock {

        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-12T10:00:00Z"), ZoneId.of("Europe/Paris"));
        }
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SearchOutboundPayments searchOutboundPayments;
    @MockitoBean
    private GetOutboundPaymentDetails getOutboundPaymentDetails;

    @Test
    void should_return_the_found_rows_when_searching() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.outboundSearch_returnsRows; the id is now a UUID)
        when(searchOutboundPayments.search(any())).thenReturn(List.of(new OutboundPaymentWithFee(seededPayment(), new BigDecimal("0.30"))));

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/search").param("chargesType", "OUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("00000000-0000-0000-0000-000000000401"));
        verify(searchOutboundPayments).search(new OutboundPaymentSearchQuery(null, null, "OUR", null, null, null, null));
    }

    @Test
    void should_pass_every_raw_search_parameter_to_the_use_case() throws Exception {
        // GIVEN
        when(searchOutboundPayments.search(any())).thenReturn(List.of());

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/search")
                        .param("debtorIban", " DE89370400440532013000 ")
                        .param("destinationBic", "abna")
                        .param("chargesType", "sha")
                        .param("status", "sent")
                        .param("executionFrom", "2026-10-01")
                        .param("executionTo", "2026-10-31")
                        .param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
        verify(searchOutboundPayments).search(new OutboundPaymentSearchQuery(" DE89370400440532013000 ", "abna", "sha",
                "sent", "2026-10-01", "2026-10-31", 20));
    }

    @Test
    void should_render_search_rows_in_camel_case_with_the_search_fee() throws Exception {
        // GIVEN (BR-API-09: camelCase, no audit timestamps)
        when(searchOutboundPayments.search(any())).thenReturn(List.of(new OutboundPaymentWithFee(seededPayment(), new BigDecimal("0.30"))));

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/search"))
                .andExpect(status().isOk())
                .andExpect(content().string("[{\"id\":\"00000000-0000-0000-0000-000000000401\",\"instructionId\":\"OUT-2026-9001\","
                        + "\"amount\":780.00,\"currency\":\"EUR\",\"debtorIban\":\"DE89370400440532013000\",\"debtorName\":\"ACME GmbH\","
                        + "\"destinationBic\":\"ABNANL2A\",\"requestedExecutionDate\":\"2026-10-16\",\"chargesType\":\"SHA\","
                        + "\"status\":\"SENT\",\"estimatedFee\":0.30}]"));
    }

    @Test
    void should_render_the_details_as_the_legacy_snake_case_map_with_utc_timestamps() throws Exception {
        // GIVEN (BR-OUT-09, N-02: Europe/Paris 2026-10-16 08:00 -> 06:00 UTC)
        when(getOutboundPaymentDetails.getDetails(seededId)).thenReturn(Optional.of(new OutboundPaymentWithFee(seededPayment(), new BigDecimal("0.50"))));

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/00000000-0000-0000-0000-000000000401"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000401"))
                .andExpect(jsonPath("$.instruction_id").value("OUT-2026-9001"))
                .andExpect(jsonPath("$.amount").value(780.00))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.debtor_iban").value("DE89370400440532013000"))
                .andExpect(jsonPath("$.debtor_name").value("ACME GmbH"))
                .andExpect(jsonPath("$.destination_bic").value("ABNANL2A"))
                .andExpect(jsonPath("$.requested_execution_date").value("2026-10-16"))
                .andExpect(jsonPath("$.charges_type").value("SHA"))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.created_at").value("2026-10-16T06:00:00.000+00:00"))
                .andExpect(jsonPath("$.updated_at").value("2026-10-16T06:01:00.000+00:00"))
                .andExpect(jsonPath("$.estimatedFee").value(0.50));
    }

    @Test
    void should_keep_null_values_in_the_details_map() throws Exception {
        // GIVEN (SHA without amount: amount rendered null, fee 0.50)
        OutboundPayment noAmount = OutboundPayment.builder(seededId).chargesType("SHA").build();
        when(getOutboundPaymentDetails.getDetails(seededId)).thenReturn(Optional.of(new OutboundPaymentWithFee(noAmount, new BigDecimal("0.50"))));

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/00000000-0000-0000-0000-000000000401"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(13))
                .andExpect(jsonPath("$.amount").value(nullValue()))
                .andExpect(jsonPath("$.created_at").value(nullValue()))
                .andExpect(jsonPath("$.requested_execution_date").value(nullValue()))
                .andExpect(jsonPath("$.estimatedFee").value(0.50));
    }

    @Test
    void should_render_the_details_fee_given_by_the_use_case() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.outboundDetails_computesFeeFromController: OUR 1000.00 -> 20.00)
        OutboundPayment our = OutboundPayment.builder(seededId).money(Money.of(new BigDecimal("1000.00"), null)).chargesType("OUR").build();
        when(getOutboundPaymentDetails.getDetails(seededId)).thenReturn(Optional.of(new OutboundPaymentWithFee(our, new BigDecimal("20.00"))));

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/00000000-0000-0000-0000-000000000401"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedFee").value(20.00));
    }

    @Test
    void should_build_the_details_from_the_details_use_case_and_never_call_the_search() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.should_build_outbound_details_from_entity_manager_and_never_call_the_service)
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000403");
        OutboundPayment payment = OutboundPayment.builder(id)
                .instructionId("OUT-2026-9003")
                .money(Money.of(new BigDecimal("95.00"), null))
                .chargesType("BEN")
                .build();
        when(getOutboundPaymentDetails.getDetails(id)).thenReturn(Optional.of(new OutboundPaymentWithFee(payment, BigDecimal.ZERO)));

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/" + id.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instruction_id").value("OUT-2026-9003"))
                .andExpect(jsonPath("$.charges_type").value("BEN"))
                .andExpect(jsonPath("$.instructionId").doesNotExist());
        verify(getOutboundPaymentDetails).getDetails(id);
        verifyNoInteractions(searchOutboundPayments);
    }

    @Test
    void should_return_404_when_the_payment_is_missing() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.outboundDetails_missing_returns404)
        when(getOutboundPaymentDetails.getDetails(PaymentId.parse("00000000-0000-0000-0000-000000000404"))).thenReturn(Optional.empty());

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/00000000-0000-0000-0000-000000000404"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void should_return_404_without_querying_when_the_details_id_is_not_a_uuid() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.should_return_404_without_querying_when_outbound_details_id_is_not_a_uuid; C-02)
        String malformedId = "not-a-uuid";

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/" + malformedId))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
        verifyNoInteractions(getOutboundPaymentDetails, searchOutboundPayments);
    }

    @Test
    void should_return_404_without_trimming_a_padded_details_id() throws Exception {
        // GIVEN (BR-API-06: outbound details does not trim, so a padded id is not a UUID)
        String paddedId = " 00000000-0000-0000-0000-000000000401 ";

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/{id}", paddedId))
                .andExpect(status().isNotFound());
        verifyNoInteractions(getOutboundPaymentDetails);
    }

    @Test
    void should_return_400_bad_request_when_an_execution_bound_is_malformed() throws Exception {
        // GIVEN (BR-OUT-04, RestDomainExceptionHandler: MalformedDateException without message on the search endpoint)
        when(searchOutboundPayments.search(any())).thenThrow(new MalformedDateException(null));

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/search").param("executionFrom", "garbage"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"error\":\"Bad request\"}"));
    }

    @Test
    void should_return_400_with_the_number_format_message_when_a_date_field_is_not_numeric() throws Exception {
        // GIVEN (F-01, RestDomainExceptionHandler: MalformedDateException with message on the search endpoint)
        when(searchOutboundPayments.search(any())).thenThrow(new MalformedDateException("Error at index 2 in: \"20x6\""));

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/search").param("executionTo", "20x6-10-16"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"error\":\"Error at index 2 in: \\\"20x6\\\"\"}"));
    }

    @Test
    void should_return_400_with_the_legacy_message_when_the_limit_is_negative() throws Exception {
        // GIVEN (BR-SHR-03, RestDomainExceptionHandler: NegativeSearchLimitException on the search endpoint)
        when(searchOutboundPayments.search(any())).thenThrow(new NegativeSearchLimitException());

        // WHEN / THEN
        mvc.perform(get("/api/v1/outbound-payments/search").param("limit", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"error\":\"max-results cannot be negative\"}"));
    }

    private static OutboundPayment seededPayment() {
        return OutboundPayment.builder(seededId)
                .instructionId("OUT-2026-9001")
                .money(Money.of(new BigDecimal("780.00"), "EUR"))
                .debtorIban("DE89370400440532013000")
                .debtorName("ACME GmbH")
                .destinationBic("ABNANL2A")
                .requestedExecutionDate(LocalDate.of(2026, 10, 16))
                .chargesType("SHA")
                .status("SENT")
                .createdAt(LocalDateTime.of(2026, 10, 16, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 10, 16, 8, 1))
                .build();
    }
}
