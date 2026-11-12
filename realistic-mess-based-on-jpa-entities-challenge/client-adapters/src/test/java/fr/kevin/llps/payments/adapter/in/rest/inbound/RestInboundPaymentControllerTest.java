package fr.kevin.llps.payments.adapter.in.rest.inbound;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.kevin.llps.payments.domain.inbound.InboundPayment;
import fr.kevin.llps.payments.domain.inbound.InboundPaymentView;
import fr.kevin.llps.payments.domain.inbound.port.in.GetInboundPaymentDetails;
import fr.kevin.llps.payments.domain.inbound.port.in.SearchInboundPayments;
import fr.kevin.llps.payments.domain.inbound.query.InboundPaymentSearchQuery;
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

@WebMvcTest(RestInboundPaymentController.class)
class RestInboundPaymentControllerTest {

    private static final PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000621");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SearchInboundPayments searchInboundPayments;
    @MockitoBean
    private GetInboundPaymentDetails getInboundPaymentDetails;

    @Test
    void should_pass_every_raw_search_parameter_to_the_use_case() throws Exception {
        // GIVEN
        when(searchInboundPayments.search(any())).thenReturn(List.of());

        // WHEN / THEN
        mvc.perform(get("/api/v1/inbound-payments/search")
                        .param("creditorIban", " DE89370400440532013000 ")
                        .param("originBic", "bnpa")
                        .param("status", "settled")
                        .param("currency", "eur")
                        .param("valueDateFrom", "2026-10-01")
                        .param("valueDateTo", "2026-10-31")
                        .param("limit", "201"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
        verify(searchInboundPayments).search(new InboundPaymentSearchQuery(" DE89370400440532013000 ", "bnpa", "settled",
                "eur", "2026-10-01", "2026-10-31", 201));
    }

    @Test
    void should_render_every_field_with_the_api_date_format_and_the_age_in_days() throws Exception {
        // GIVEN (rendering of PaymentRepositoryUnitTest.inboundSearch_appliesFiltersAndDefaultLimit / inboundFindById_foundAndMissing)
        when(searchInboundPayments.search(any())).thenReturn(List.of(new InboundPaymentView(fullPayment(), 28L)));

        // WHEN / THEN
        mvc.perform(get("/api/v1/inbound-payments/search"))
                .andExpect(status().isOk())
                .andExpect(content().string("[{\"id\":\"00000000-0000-0000-0000-000000000621\",\"creditTransferRef\":\"CTR-1\","
                        + "\"amount\":3400.00,\"currency\":\"EUR\",\"creditorIban\":\"DE89370400440532013000\","
                        + "\"creditorName\":\"ACME\",\"originBic\":\"BNPAFRPP\",\"valueDate\":\"2026-10-15\","
                        + "\"settlementDate\":\"2026-10-15\",\"status\":\"SETTLED\",\"ageInDays\":28}]"));
    }

    @ParameterizedTest(name = "{0}: ageInDays {1} rendered as is")
    @CsvSource(nullValues = "NULL", value = {
            "two days old (inboundSearch_computesAgeInDays),                                            2",
            "future value date (should_return_negative_age_in_days_in_search_when_value_date_is_in_the_future), -4",
            "no value date (should_return_null_age_in_days_in_search_when_value_date_is_null),          NULL"
    })
    void should_render_the_age_in_days_given_by_the_search_use_case(String testCase, Long ageInDays) throws Exception {
        // GIVEN (the computation moved to the use case; the REST adapter renders it without recomputing it)
        when(searchInboundPayments.search(any())).thenReturn(List.of(new InboundPaymentView(fullPayment(), ageInDays)));

        // WHEN / THEN
        mvc.perform(get("/api/v1/inbound-payments/search"))
                .andExpect(status().isOk())
                .andExpect(ageInDays == null ? jsonPath("$[0].ageInDays").value(nullValue())
                        : jsonPath("$[0].ageInDays").value(ageInDays));
    }

    @Test
    void should_return_details_age_in_days_as_computed_by_the_use_case_without_recomputing_it() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.should_return_details_age_in_days_as_computed_by_service_without_recomputing_it; UUID id)
        when(getInboundPaymentDetails.getDetails(id)).thenReturn(Optional.of(new InboundPaymentView(fullPayment(), 99L)));

        // WHEN / THEN
        mvc.perform(get("/api/v1/inbound-payments/00000000-0000-0000-0000-000000000621"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ageInDays").value(99));
    }

    @Test
    void should_return_the_payment_details_when_found() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.inboundDetails_foundAndMissing, found case; UUID id)
        when(getInboundPaymentDetails.getDetails(id)).thenReturn(Optional.of(new InboundPaymentView(fullPayment(), null)));

        // WHEN / THEN
        mvc.perform(get("/api/v1/inbound-payments/00000000-0000-0000-0000-000000000621"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditTransferRef").value("CTR-1"));
    }

    @Test
    void should_return_404_with_empty_body_when_the_payment_is_missing() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.inboundDetails_foundAndMissing, missing case; UUID id)
        when(getInboundPaymentDetails.getDetails(any())).thenReturn(Optional.empty());

        // WHEN / THEN
        mvc.perform(get("/api/v1/inbound-payments/11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void should_trim_the_path_id_before_parsing_it() throws Exception {
        // GIVEN (remapped from PaymentRepositoryUnitTest.should_trim_details_id_before_parsing_it: same padded id)
        PaymentId trimmed = PaymentId.parse("00000000-0000-0000-0000-000000000686");
        when(getInboundPaymentDetails.getDetails(trimmed)).thenReturn(Optional.of(new InboundPaymentView(
                InboundPayment.builder(trimmed).creditTransferRef("CTR-TRIM").build(), null)));

        // WHEN / THEN
        mvc.perform(get("/api/v1/inbound-payments/{id}", "  00000000-0000-0000-0000-000000000686 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creditTransferRef").value("CTR-TRIM"));
    }

    @Test
    void should_return_400_bad_request_when_a_date_bound_is_malformed() throws Exception {
        // GIVEN (BR-API-07, RestDomainExceptionHandler: MalformedDateException without message on the search endpoint)
        when(searchInboundPayments.search(any())).thenThrow(new MalformedDateException(null));

        // WHEN / THEN
        mvc.perform(get("/api/v1/inbound-payments/search").param("valueDateFrom", "garbage"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"error\":\"Bad request\"}"));
    }

    @Test
    void should_return_400_with_the_number_format_message_when_a_date_field_is_not_numeric() throws Exception {
        // GIVEN (F-01, RestDomainExceptionHandler: MalformedDateException with message on the search endpoint)
        when(searchInboundPayments.search(any())).thenThrow(new MalformedDateException("Error at index 1 in: \"1x\""));

        // WHEN / THEN
        mvc.perform(get("/api/v1/inbound-payments/search").param("valueDateFrom", "2026-1x-05"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"error\":\"Error at index 1 in: \\\"1x\\\"\"}"));
    }

    @Test
    void should_return_400_with_the_legacy_message_when_the_limit_is_negative() throws Exception {
        // GIVEN (BR-SHR-03, RestDomainExceptionHandler: NegativeSearchLimitException on the search endpoint)
        when(searchInboundPayments.search(any())).thenThrow(new NegativeSearchLimitException());

        // WHEN / THEN
        mvc.perform(get("/api/v1/inbound-payments/search").param("limit", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"error\":\"max-results cannot be negative\"}"));
    }

    @Test
    void should_return_400_with_the_uuid_message_when_the_details_id_is_malformed() throws Exception {
        // GIVEN (BR-API-05, RestDomainExceptionHandler: InvalidPaymentIdException on the details endpoint)
        String malformedId = "xyz";

        // WHEN / THEN
        mvc.perform(get("/api/v1/inbound-payments/{id}", malformedId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"error\":\"Invalid UUID string: xyz\"}"));
        verifyNoInteractions(getInboundPaymentDetails);
    }

    private static InboundPayment fullPayment() {
        return InboundPayment.builder(id)
                .creditTransferRef("CTR-1")
                .money(Money.of(new BigDecimal("3400.00"), "EUR"))
                .creditorIban("DE89370400440532013000")
                .creditorName("ACME")
                .originBic("BNPAFRPP")
                .valueDate(LocalDate.of(2026, 10, 15))
                .settlementDate(LocalDate.of(2026, 10, 15))
                .status("SETTLED")
                .build();
    }
}
