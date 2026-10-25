package fr.kevin.llps.payments.compliancehold.adapter.in.rest;

import fr.kevin.llps.payments.compliancehold.SuspendedPayment;
import fr.kevin.llps.payments.compliancehold.port.in.SuspendedPaymentQueries;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web slice of {@link SuspendedPaymentController}; carried over from the legacy PaymentControllerWebTest.
 */
@WebMvcTest(controllers = SuspendedPaymentController.class)
class SuspendedPaymentControllerWebTest {

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private SuspendedPaymentQueries suspendedQueries;
    private static final String SUSPENDED_ID = "00000000-0000-0000-0000-000000000201";
    private static final String MISSING_SUSPENDED_ID = "00000000-0000-0000-0000-000000000299";

    @Test
    void suspendedSearch_returnsRows() throws Exception {
        when(suspendedQueries.search(any())).thenReturn(List.of(suspended(SUSPENDED_ID, null, null)));

        mvc.perform(get("/api/v1/suspended-payments/search").param("reviewStatus", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(SUSPENDED_ID));
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

    @Test
    void suspendedDetails_foundAndMissing() throws Exception {
        when(suspendedQueries.get(PaymentId.parse(SUSPENDED_ID))).thenReturn(Optional.of(suspended(SUSPENDED_ID, null, null)));
        when(suspendedQueries.get(PaymentId.parse(MISSING_SUSPENDED_ID))).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/suspended-payments/" + SUSPENDED_ID)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/suspended-payments/" + MISSING_SUSPENDED_ID)).andExpect(status().isNotFound());
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
}
