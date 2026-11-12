package fr.kevin.llps.payments.adapter.in.rest.instant;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.kevin.llps.payments.domain.instant.InstantPayment;
import fr.kevin.llps.payments.domain.instant.InvalidIbanFilterException;
import fr.kevin.llps.payments.domain.instant.port.in.GetInstantPaymentDetails;
import fr.kevin.llps.payments.domain.instant.port.in.SearchInstantPayments;
import fr.kevin.llps.payments.domain.instant.query.InstantPaymentSearchQuery;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.NegativeSearchLimitException;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RestInstantPaymentController.class)
class RestInstantPaymentControllerTest {

    private static final String paymentJson = "{\"id\":\"00000000-0000-0000-0000-000000000601\",\"endToEndId\":\"E2E-T1\","
            + "\"uetr\":\"uetr-E2E-T1\",\"amount\":1500.00,\"currency\":\"EUR\",\"debtorIban\":\"DE89370400440532013000\","
            + "\"debtorName\":\"ACME\",\"creditorIban\":\"FR7630006000011234567890189\",\"creditorName\":\"Dupont\","
            + "\"executionDatetime\":\"2026-10-15T09:15:00\",\"status\":\"COMPLETED\",\"scheme\":\"SCT INST\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SearchInstantPayments searchInstantPayments;
    @MockitoBean
    private GetInstantPaymentDetails getInstantPaymentDetails;

    @Test
    void should_return_the_found_rows_when_searching() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.instantSearch_returnsRows; the id is now a UUID)
        when(searchInstantPayments.search(any())).thenReturn(List.of(fullPayment()));

        // WHEN / THEN
        mvc.perform(get("/api/v1/instant-payments/search").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("00000000-0000-0000-0000-000000000601"));
        verify(searchInstantPayments).search(new InstantPaymentSearchQuery(null, null, null, "COMPLETED", null, null, null, null));
    }

    @Test
    void should_pass_every_raw_search_parameter_to_the_use_case() throws Exception {
        // GIVEN
        when(searchInstantPayments.search(any())).thenReturn(List.of());

        // WHEN / THEN
        mvc.perform(get("/api/v1/instant-payments/search")
                        .param("endToEndId", " E2E-T1 ")
                        .param("debtorIban", "de89")
                        .param("creditorIban", "fr76")
                        .param("status", "completed")
                        .param("currency", "eur")
                        .param("executionFrom", "2026-10-01T00:00:00")
                        .param("executionTo", "2026-10-31")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
        verify(searchInstantPayments).search(new InstantPaymentSearchQuery(" E2E-T1 ", "de89", "fr76", "completed", "eur",
                "2026-10-01T00:00:00", "2026-10-31", 10));
    }

    @Test
    void should_render_every_field_with_the_api_timestamp_format() throws Exception {
        // GIVEN (remapped from the rendering assertions of PaymentRepositoryUnitTest.instantSearch_appliesAllFilters_andFormatsExecutionDatetime)
        when(searchInstantPayments.search(any())).thenReturn(List.of(fullPayment()));

        // WHEN / THEN
        mvc.perform(get("/api/v1/instant-payments/search"))
                .andExpect(status().isOk())
                .andExpect(content().string("[" + paymentJson + "]"));
    }

    @ParameterizedTest(name = "{0}: GET {1} gives 400 \"{2}\"")
    @CsvSource(delimiter = '|', value = {
            "invalid debtor IBAN (remapped should_return_400_with_exception_message_when_service_rejects_instant_iban) | DE1 | Invalid debtor IBAN",
            "invalid creditor IBAN | X | Invalid creditor IBAN"
    })
    void should_return_400_with_the_message_when_an_iban_filter_is_invalid(String testCase, String iban,
            String message) throws Exception {
        // GIVEN (RestDomainExceptionHandler: InvalidIbanFilterException on the search endpoint)
        when(searchInstantPayments.search(any())).thenThrow(new InvalidIbanFilterException(message));

        // WHEN / THEN
        mvc.perform(get("/api/v1/instant-payments/search").param("debtorIban", iban))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(message));
    }

    @Test
    void should_return_400_with_the_legacy_message_when_the_limit_is_negative() throws Exception {
        // GIVEN (BR-SHR-03, RestDomainExceptionHandler: NegativeSearchLimitException on the search endpoint)
        when(searchInstantPayments.search(any())).thenThrow(new NegativeSearchLimitException());

        // WHEN / THEN
        mvc.perform(get("/api/v1/instant-payments/search").param("limit", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"error\":\"max-results cannot be negative\"}"));
    }

    @Test
    void should_return_the_payment_details_when_found() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.instantDetails_foundAndMissing, found case; UUID id)
        when(getInstantPaymentDetails.getDetails(PaymentId.parse("00000000-0000-0000-0000-000000000601")))
                .thenReturn(Optional.of(fullPayment()));

        // WHEN / THEN
        mvc.perform(get("/api/v1/instant-payments/00000000-0000-0000-0000-000000000601"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000601"))
                .andExpect(content().string(paymentJson));
    }

    @Test
    void should_return_404_with_empty_body_when_the_payment_is_missing() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.instantDetails_foundAndMissing, missing case; UUID id)
        when(getInstantPaymentDetails.getDetails(any())).thenReturn(Optional.empty());

        // WHEN / THEN
        mvc.perform(get("/api/v1/instant-payments/11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void should_trim_the_path_id_before_parsing_it() throws Exception {
        // GIVEN (BR-API-06)
        when(getInstantPaymentDetails.getDetails(PaymentId.parse("00000000-0000-0000-0000-000000000601")))
                .thenReturn(Optional.of(fullPayment()));

        // WHEN / THEN
        mvc.perform(get("/api/v1/instant-payments/{id}", " 00000000-0000-0000-0000-000000000601 "))
                .andExpect(status().isOk())
                .andExpect(content().string(paymentJson));
    }

    @Test
    void should_return_400_with_the_uuid_message_when_the_details_id_is_malformed() throws Exception {
        // GIVEN (BR-API-05, RestDomainExceptionHandler: InvalidPaymentIdException on the details endpoint)
        String malformedId = "xyz";

        // WHEN / THEN
        mvc.perform(get("/api/v1/instant-payments/{id}", malformedId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"error\":\"Invalid UUID string: xyz\"}"));
        verifyNoInteractions(getInstantPaymentDetails);
    }

    private static InstantPayment fullPayment() {
        return InstantPayment.builder(PaymentId.parse("00000000-0000-0000-0000-000000000601"))
                .endToEndId("E2E-T1")
                .uetr("uetr-E2E-T1")
                .money(Money.of(new BigDecimal("1500.00"), "EUR"))
                .debtorIban("DE89370400440532013000")
                .debtorName("ACME")
                .creditorIban("FR7630006000011234567890189")
                .creditorName("Dupont")
                .executionDatetime(LocalDateTime.of(2026, 10, 15, 9, 15))
                .status("COMPLETED")
                .scheme("SCT INST")
                .build();
    }
}
