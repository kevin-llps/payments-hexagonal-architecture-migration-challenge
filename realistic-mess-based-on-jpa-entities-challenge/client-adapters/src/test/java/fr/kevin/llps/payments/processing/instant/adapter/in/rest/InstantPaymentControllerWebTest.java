package fr.kevin.llps.payments.processing.instant.adapter.in.rest;

import fr.kevin.llps.payments.processing.instant.InstantPayment;
import fr.kevin.llps.payments.processing.instant.port.in.InstantPaymentQueries;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web slice of {@link InstantPaymentController}; carried over from the legacy PaymentControllerWebTest.
 */
@WebMvcTest(controllers = InstantPaymentController.class)
class InstantPaymentControllerWebTest {

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private InstantPaymentQueries instantQueries;
    private static final String INSTANT_ID = "00000000-0000-0000-0000-000000000101";
    private static final String MISSING_INSTANT_ID = "00000000-0000-0000-0000-000000000199";

    @Test
    void instantSearch_returnsRows() throws Exception {
        when(instantQueries.search(any())).thenReturn(List.of(instant(INSTANT_ID, null)));

        mvc.perform(get("/api/v1/instant-payments/search").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(INSTANT_ID));
    }

    @Test
    void instantSearch_serviceFailure_returns500() throws Exception {
        when(instantQueries.search(any())).thenThrow(new RuntimeException("boom"));

        mvc.perform(get("/api/v1/instant-payments/search"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    // moved from PaymentRepositoryUnitTest.instantSearch_appliesAllFilters_andFormatsExecutionDatetime:
    // rendering is a REST adapter concern (D-02)
    @Test
    void instantSearch_formatsExecutionDatetime() throws Exception {
        when(instantQueries.search(any())).thenReturn(List.of(
                instant(INSTANT_ID, LocalDateTime.of(2026, 10, 15, 9, 15, 0))));

        mvc.perform(get("/api/v1/instant-payments/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].endToEndId").value("E2E-T1"))
                .andExpect(jsonPath("$[0].amount").value(1500.00))
                .andExpect(jsonPath("$[0].executionDatetime").value("2026-10-15T09:15:00"))
                .andExpect(jsonPath("$[0].scheme").value("SCT INST"));
    }

    @Test
    void instantDetails_foundAndMissing() throws Exception {
        when(instantQueries.get(PaymentId.parse(INSTANT_ID))).thenReturn(Optional.of(instant(INSTANT_ID, null)));
        when(instantQueries.get(PaymentId.parse(MISSING_INSTANT_ID))).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/instant-payments/" + INSTANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(INSTANT_ID));
        mvc.perform(get("/api/v1/instant-payments/" + MISSING_INSTANT_ID))
                .andExpect(status().isNotFound());
    }

    // moved from PaymentRepositoryUnitTest.instantFindById_foundAndMissing (rendering on details)
    @Test
    void instantDetails_formatsExecutionDatetime() throws Exception {
        when(instantQueries.get(PaymentId.parse(INSTANT_ID))).thenReturn(Optional.of(
                instant(INSTANT_ID, LocalDateTime.of(2026, 10, 15, 9, 15, 0))));

        mvc.perform(get("/api/v1/instant-payments/" + INSTANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executionDatetime").value("2026-10-15T09:15:00"));
    }

    private static InstantPayment instant(String id, LocalDateTime executionDatetime) {
        return new InstantPayment(PaymentId.parse(id), "E2E-T1", "uetr-E2E-T1", Money.of(new BigDecimal("1500.00"), "EUR"),
                "DE89370400440532013000", "ACME", "FR7630006000011234567890189", "Dupont", executionDatetime,
                "COMPLETED", "SCT INST", null);
    }
}
