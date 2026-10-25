package fr.kevin.llps.payments.processing.inbound.adapter.in.rest;

import fr.kevin.llps.payments.processing.inbound.InboundPayment;
import fr.kevin.llps.payments.processing.inbound.port.in.InboundPaymentQueries;
import fr.kevin.llps.payments.processing.inbound.port.in.InboundPaymentView;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web slice of {@link InboundPaymentController}; carried over from the legacy PaymentControllerWebTest.
 */
@WebMvcTest(controllers = InboundPaymentController.class)
class InboundPaymentControllerWebTest {

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private InboundPaymentQueries inboundQueries;

    // The age is computed by InboundPaymentQueryService (see PaymentServiceUnitTest); the controller renders it.

    private static final String INBOUND_ID = "00000000-0000-0000-0000-000000000301";
    private static final String OTHER_INBOUND_ID = "00000000-0000-0000-0000-000000000302";
    private static final String MISSING_INBOUND_ID = "00000000-0000-0000-0000-000000000399";

    @Test
    void inboundSearch_computesAgeInDays() throws Exception {
        when(inboundQueries.search(any())).thenReturn(List.of(
                inboundView(INBOUND_ID, LocalDate.now().minusDays(2), null, 2L)));

        mvc.perform(get("/api/v1/inbound-payments/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ageInDays").value(2));
    }

    @Test
    void inboundSearch_futureOrMissingValueDate_ageInDaysNegativeOrNull() throws Exception {
        when(inboundQueries.search(any())).thenReturn(List.of(
                inboundView(INBOUND_ID, LocalDate.now().plusDays(5), null, -5L),
                inboundView(OTHER_INBOUND_ID, null, null, null)));

        mvc.perform(get("/api/v1/inbound-payments/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ageInDays").value(-5))
                .andExpect(jsonPath("$[1].ageInDays").value(nullValue()));
    }

    // moved from PaymentRepositoryUnitTest.inboundSearch_appliesFiltersAndDefaultLimit (rendering, D-02)
    @Test
    void inboundSearch_formatsValueAndSettlementDates() throws Exception {
        when(inboundQueries.search(any())).thenReturn(List.of(
                inboundView(INBOUND_ID, LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 15), 10L)));

        mvc.perform(get("/api/v1/inbound-payments/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].creditTransferRef").value("CTR-1"))
                .andExpect(jsonPath("$[0].valueDate").value("2026-10-15"))
                .andExpect(jsonPath("$[0].settlementDate").value("2026-10-15"));
    }

    @Test
    void inboundDetails_foundAndMissing() throws Exception {
        when(inboundQueries.get(PaymentId.parse(INBOUND_ID))).thenReturn(Optional.of(
                inboundView(INBOUND_ID, null, null, null)));
        when(inboundQueries.get(PaymentId.parse(MISSING_INBOUND_ID))).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/inbound-payments/" + INBOUND_ID)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/inbound-payments/" + MISSING_INBOUND_ID)).andExpect(status().isNotFound());
    }

    // moved from PaymentRepositoryUnitTest.inboundFindById_foundAndMissing (rendering on details)
    @Test
    void inboundDetails_formatsDates() throws Exception {
        when(inboundQueries.get(PaymentId.parse(INBOUND_ID))).thenReturn(Optional.of(
                inboundView(INBOUND_ID, LocalDate.of(2026, 10, 17), null, 8L)));

        mvc.perform(get("/api/v1/inbound-payments/" + INBOUND_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valueDate").value("2026-10-17"))
                .andExpect(jsonPath("$.settlementDate").value(nullValue()));
    }

    private static InboundPaymentView inboundView(String id, LocalDate valueDate, LocalDate settlementDate,
                                                  Long ageInDays) {
        return new InboundPaymentView(new InboundPayment(PaymentId.parse(id), "CTR-1",
                Money.of(new BigDecimal("3400.00"), "EUR"), "DE89370400440532013000", "ACME", "BNPAFRPP", valueDate,
                settlementDate, "SETTLED", null), ageInDays);
    }
}
