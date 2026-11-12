package fr.kevin.llps.payments.adapter.in.rest.recall;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.kevin.llps.payments.domain.recall.OriginalPaymentReference;
import fr.kevin.llps.payments.domain.recall.Recall;
import fr.kevin.llps.payments.domain.recall.UnknownRecallTypeException;
import fr.kevin.llps.payments.domain.recall.port.in.GetRecallDetails;
import fr.kevin.llps.payments.domain.recall.port.in.SearchRecalls;
import fr.kevin.llps.payments.domain.recall.query.RecallSearchQuery;
import fr.kevin.llps.payments.domain.sharedkernel.InvalidPaymentIdException;
import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RestRecallController.class)
class RestRecallControllerTest {

    private static final String recallJson = "{\"id\":\"00000000-0000-0000-0000-000000000501\",\"recallReference\":\"RCL-1\","
            + "\"recallType\":\"R-COR\",\"originalPaymentId\":\"00000000-0000-0000-0000-000000000401\","
            + "\"originalPaymentType\":\"OUTBOUND\",\"reasonCode\":\"AC04\",\"amount\":300.00,\"currency\":\"EUR\","
            + "\"initiatedAt\":\"2026-10-16T12:00:00\",\"deadline\":\"2026-10-18\",\"status\":\"INITIATED\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SearchRecalls searchRecalls;
    @MockitoBean
    private GetRecallDetails getRecallDetails;

    @Test
    void should_return_the_found_rows_when_searching() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.recallsSearch_returnsRows; the id is now a UUID)
        when(searchRecalls.search(any())).thenReturn(List.of(fullRecall()));

        // WHEN / THEN
        mvc.perform(get("/api/v1/recalls/search").param("recallType", "R-COR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("00000000-0000-0000-0000-000000000501"));
        verify(searchRecalls).search(new RecallSearchQuery("R-COR", null, null, null, null, null, null, null));
    }

    @Test
    void should_pass_every_raw_search_parameter_including_the_ignored_limit() throws Exception {
        // GIVEN
        when(searchRecalls.search(any())).thenReturn(List.of());

        // WHEN / THEN
        mvc.perform(get("/api/v1/recalls/search")
                        .param("recallType", "r-cor")
                        .param("originalPaymentId", " 00000000-0000-0000-0000-000000000401 ")
                        .param("originalPaymentType", "outbound")
                        .param("reasonCode", "ac04")
                        .param("status", "initiated")
                        .param("initiatedFrom", "2026-10-01")
                        .param("initiatedTo", "garbage")
                        .param("limit", "-5"))
                .andExpect(status().isOk())
                .andExpect(content().string("[]"));
        verify(searchRecalls).search(new RecallSearchQuery("r-cor", " 00000000-0000-0000-0000-000000000401 ",
                "outbound", "ac04", "initiated", "2026-10-01", "garbage", -5));
    }

    @Test
    void should_render_every_field_with_the_api_formats() throws Exception {
        // GIVEN (remapped from PaymentServiceUnitTest.recallSearch_mapsFieldsAndFormatsDates)
        when(searchRecalls.search(any())).thenReturn(List.of(fullRecall()));

        // WHEN / THEN
        mvc.perform(get("/api/v1/recalls/search").param("recallType", "r-cor"))
                .andExpect(status().isOk())
                .andExpect(content().string("[" + recallJson + "]"));
    }

    @Test
    void should_render_absent_amount_currency_and_original_payment_as_null() throws Exception {
        // GIVEN (BR-SHR-09: recalls may have no amount nor currency)
        when(getRecallDetails.getDetails(PaymentId.parse("00000000-0000-0000-0000-000000000685")))
                .thenReturn(Optional.of(Recall.builder(PaymentId.parse("00000000-0000-0000-0000-000000000685")).build()));

        // WHEN / THEN
        mvc.perform(get("/api/v1/recalls/00000000-0000-0000-0000-000000000685"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"id\":\"00000000-0000-0000-0000-000000000685\",\"recallReference\":null,"
                        + "\"recallType\":null,\"originalPaymentId\":null,\"originalPaymentType\":null,\"reasonCode\":null,"
                        + "\"amount\":null,\"currency\":null,\"initiatedAt\":null,\"deadline\":null,\"status\":null}"));
    }

    @Test
    void should_return_400_with_the_raw_type_when_the_recall_type_is_unknown() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.recallsSearch_invalidType_returns400; RestDomainExceptionHandler)
        when(searchRecalls.search(any())).thenThrow(new UnknownRecallTypeException("Unknown recall type: NOPE"));

        // WHEN / THEN
        mvc.perform(get("/api/v1/recalls/search").param("recallType", "NOPE"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"error\":\"Unknown recall type: NOPE\"}"));
    }

    @Test
    void should_return_400_with_the_uuid_message_when_the_original_payment_id_filter_is_malformed() throws Exception {
        // GIVEN (BR-RCL-03, RestDomainExceptionHandler: InvalidPaymentIdException on the search endpoint)
        when(searchRecalls.search(any())).thenThrow(new InvalidPaymentIdException("Invalid UUID string: xyz"));

        // WHEN / THEN
        mvc.perform(get("/api/v1/recalls/search").param("originalPaymentId", "xyz"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"error\":\"Invalid UUID string: xyz\"}"));
    }

    @Test
    void should_return_the_recall_details_when_found() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.recallsDetails_foundAndMissing, found case; UUID id)
        when(getRecallDetails.getDetails(PaymentId.parse("00000000-0000-0000-0000-000000000501")))
                .thenReturn(Optional.of(fullRecall()));

        // WHEN / THEN
        mvc.perform(get("/api/v1/recalls/00000000-0000-0000-0000-000000000501"))
                .andExpect(status().isOk())
                .andExpect(content().string(recallJson));
    }

    @Test
    void should_return_404_with_empty_body_when_the_recall_is_missing() throws Exception {
        // GIVEN (remapped from PaymentControllerWebTest.recallsDetails_foundAndMissing, missing case; UUID id)
        when(getRecallDetails.getDetails(any())).thenReturn(Optional.empty());

        // WHEN / THEN
        mvc.perform(get("/api/v1/recalls/11111111-1111-1111-1111-111111111111"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void should_trim_the_path_id_before_parsing_it() throws Exception {
        // GIVEN (BR-API-06)
        when(getRecallDetails.getDetails(PaymentId.parse("00000000-0000-0000-0000-000000000501")))
                .thenReturn(Optional.of(fullRecall()));

        // WHEN / THEN
        mvc.perform(get("/api/v1/recalls/{id}", " 00000000-0000-0000-0000-000000000501 "))
                .andExpect(status().isOk())
                .andExpect(content().string(recallJson));
    }

    @Test
    void should_return_400_with_the_uuid_message_when_the_details_id_is_malformed() throws Exception {
        // GIVEN (BR-API-05, RestDomainExceptionHandler: InvalidPaymentIdException on the details endpoint)
        String malformedId = "xyz";

        // WHEN / THEN
        mvc.perform(get("/api/v1/recalls/{id}", malformedId))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("{\"error\":\"Invalid UUID string: xyz\"}"));
        verifyNoInteractions(getRecallDetails);
    }

    private static Recall fullRecall() {
        return Recall.builder(PaymentId.parse("00000000-0000-0000-0000-000000000501"))
                .recallReference("RCL-1")
                .recallType("R-COR")
                .originalPayment(OriginalPaymentReference.of(PaymentId.parse("00000000-0000-0000-0000-000000000401"), "OUTBOUND"))
                .reasonCode("AC04")
                .money(Money.of(new BigDecimal("300.00"), "EUR"))
                .initiatedAt(LocalDateTime.of(2026, 10, 16, 12, 0))
                .deadline(LocalDate.of(2026, 10, 18))
                .status("INITIATED")
                .build();
    }
}
