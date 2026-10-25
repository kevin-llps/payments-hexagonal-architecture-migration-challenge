package fr.kevin.llps.payments.recall.adapter.in.rest;

import fr.kevin.llps.payments.recall.OriginalPaymentRef;
import fr.kevin.llps.payments.recall.Recall;
import fr.kevin.llps.payments.recall.port.in.RecallQueries;
import fr.kevin.llps.payments.shared.domain.InvalidSearchFilterException;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web slice of {@link RecallController}; carried over from the legacy PaymentControllerWebTest.
 */
@WebMvcTest(controllers = RecallController.class)
class RecallControllerWebTest {

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private RecallQueries recallQueries;
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
