package fr.kevin.llps.payments.adapter.in.rest.suspended;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.NegativeSearchLimitException;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.suspended.SuspendedPayment;
import fr.kevin.llps.payments.domain.suspended.port.in.GetSuspendedPaymentDetails;
import fr.kevin.llps.payments.domain.suspended.port.in.SearchSuspendedPayments;
import fr.kevin.llps.payments.domain.suspended.query.SuspendedPaymentSearchQuery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RestSuspendedPaymentController.class)
class RestSuspendedPaymentControllerTest {

    private static final String releasedPaymentJson = "{\"id\":\"00000000-0000-0000-0000-000000000202\",\"amount\":450.10,"
            + "\"currency\":\"EUR\",\"debtorIban\":\"FR7630006000011234567890189\",\"debtorName\":\"SARL Dupont\","
            + "\"creditorIban\":\"DE02120300000000202051\",\"creditorName\":\"Jonas Weber\",\"holdReason\":\"AML_THRESHOLD\","
            + "\"complianceCaseRef\":\"CASE-2026-0043\",\"reviewStatus\":\"RELEASED\",\"heldAt\":\"2026-10-13T10:20:00\","
            + "\"releasedAt\":\"2026-10-13T15:45:00\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SearchSuspendedPayments searchSuspendedPayments;
    @MockitoBean
    private GetSuspendedPaymentDetails getSuspendedPaymentDetails;

    @Test
    void should_return_the_found_rows_when_searching() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.suspendedSearch_returnsRows; the id is now a UUID)
        when(searchSuspendedPayments.search(any())).thenReturn(List.of(pendingPayment()));

        // WHEN / THEN
        mvc.perform(get("/api/v1/suspended-payments/search").param("reviewStatus", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("00000000-0000-0000-0000-000000000201"));
        verify(searchSuspendedPayments).search(new SuspendedPaymentSearchQuery(null, null, "PENDING", null, null, null));
    }

    @Test
    void should_pass_every_raw_search_parameter_to_the_use_case() throws Exception {
        // GIVEN
        when(searchSuspendedPayments.search(any())).thenReturn(List.of());

        // WHEN / THEN
        mvc.perform(get("/api/v1/suspended-payments/search")
                        .param("debtorIban", " DE89 ")
                        .param("holdReason", "aml_threshold")
                        .param("reviewStatus", "pending")
                        .param("heldFrom", "2026-10-01T00:00:00")
                        .param("heldTo", "garbage")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
        verify(searchSuspendedPayments).search(new SuspendedPaymentSearchQuery(" DE89 ", "aml_threshold", "pending",
                "2026-10-01T00:00:00", "garbage", 5));
    }

    @Test
    void should_render_every_search_field_with_the_api_timestamp_format_and_explicit_nulls() throws Exception {
        // GIVEN (remapped from PaymentServiceUnitTest.suspendedSearch_mapsAllFields)
        when(searchSuspendedPayments.search(any())).thenReturn(List.of(pendingPayment()));

        // WHEN / THEN
        mvc.perform(get("/api/v1/suspended-payments/search"))
                .andExpect(status().isOk())
                .andExpect(content().string("[{\"id\":\"00000000-0000-0000-0000-000000000201\",\"amount\":100.00,"
                        + "\"currency\":\"EUR\",\"debtorIban\":\"DE89\",\"debtorName\":\"ACME\",\"creditorIban\":\"FR76\","
                        + "\"creditorName\":\"Dupont\",\"holdReason\":\"AML_THRESHOLD\",\"complianceCaseRef\":\"CASE-1\","
                        + "\"reviewStatus\":\"PENDING\",\"heldAt\":\"2026-10-14T08:00:00\",\"releasedAt\":null}]"));
    }

    @Test
    void should_render_details_with_the_same_fields_as_search() throws Exception {
        // GIVEN (remapped from PaymentServiceUnitTest.should_map_suspended_details_with_same_fields_as_search)
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000202");
        when(getSuspendedPaymentDetails.getDetails(id)).thenReturn(Optional.of(releasedPayment()));
        when(searchSuspendedPayments.search(any())).thenReturn(List.of(releasedPayment()));
        mvc.perform(get("/api/v1/suspended-payments/search")).andExpect(content().string("[" + releasedPaymentJson + "]"));

        // WHEN / THEN
        mvc.perform(get("/api/v1/suspended-payments/00000000-0000-0000-0000-000000000202"))
                .andExpect(status().isOk())
                .andExpect(content().string(releasedPaymentJson));
    }

    @Test
    void should_return_the_payment_details_when_found() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.suspendedDetails_foundAndMissing, found case; UUID id)
        when(getSuspendedPaymentDetails.getDetails(PaymentId.parse("00000000-0000-0000-0000-000000000202")))
                .thenReturn(Optional.of(releasedPayment()));

        // WHEN / THEN
        mvc.perform(get("/api/v1/suspended-payments/00000000-0000-0000-0000-000000000202"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000202"));
    }

    @Test
    void should_return_404_with_empty_body_when_the_payment_is_missing() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.suspendedDetails_foundAndMissing, missing case; UUID id)
        when(getSuspendedPaymentDetails.getDetails(any())).thenReturn(Optional.empty());

        // WHEN / THEN
        mvc.perform(get("/api/v1/suspended-payments/11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void should_trim_the_path_id_before_parsing_it() throws Exception {
        // GIVEN (BR-API-06)
        when(getSuspendedPaymentDetails.getDetails(PaymentId.parse("00000000-0000-0000-0000-000000000202")))
                .thenReturn(Optional.of(releasedPayment()));

        // WHEN / THEN
        mvc.perform(get("/api/v1/suspended-payments/{id}", " 00000000-0000-0000-0000-000000000202 "))
                .andExpect(status().isOk())
                .andExpect(content().string(releasedPaymentJson));
    }

    @Test
    void should_return_400_with_the_uuid_message_when_the_details_id_is_malformed() throws Exception {
        // GIVEN (BR-API-05, RestDomainExceptionHandler: InvalidPaymentIdException on the details endpoint)
        String malformedId = "xyz";

        // WHEN / THEN
        mvc.perform(get("/api/v1/suspended-payments/{id}", malformedId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"error\":\"Invalid UUID string: xyz\"}"));
        verifyNoInteractions(getSuspendedPaymentDetails);
    }

    @Test
    void should_return_400_with_the_legacy_message_when_the_search_limit_is_negative() throws Exception {
        // GIVEN (BR-SHR-03, RestDomainExceptionHandler: NegativeSearchLimitException on the search endpoint)
        when(searchSuspendedPayments.search(any())).thenThrow(new NegativeSearchLimitException());

        // WHEN / THEN
        mvc.perform(get("/api/v1/suspended-payments/search").param("limit", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"error\":\"max-results cannot be negative\"}"));
    }

    private static SuspendedPayment pendingPayment() {
        return SuspendedPayment.builder(PaymentId.parse("00000000-0000-0000-0000-000000000201"))
                .money(Money.of(new BigDecimal("100.00"), "EUR"))
                .debtorIban("DE89")
                .debtorName("ACME")
                .creditorIban("FR76")
                .creditorName("Dupont")
                .holdReason("AML_THRESHOLD")
                .complianceCaseRef("CASE-1")
                .reviewStatus("PENDING")
                .heldAt(LocalDateTime.of(2026, 10, 14, 8, 0))
                .build();
    }

    private static SuspendedPayment releasedPayment() {
        return SuspendedPayment.builder(PaymentId.of(UUID.fromString("00000000-0000-0000-0000-000000000202")))
                .money(Money.of(new BigDecimal("450.10"), "EUR"))
                .debtorIban("FR7630006000011234567890189")
                .debtorName("SARL Dupont")
                .creditorIban("DE02120300000000202051")
                .creditorName("Jonas Weber")
                .holdReason("AML_THRESHOLD")
                .complianceCaseRef("CASE-2026-0043")
                .reviewStatus("RELEASED")
                .heldAt(LocalDateTime.of(2026, 10, 13, 10, 20))
                .releasedAt(LocalDateTime.of(2026, 10, 13, 15, 45))
                .build();
    }
}
