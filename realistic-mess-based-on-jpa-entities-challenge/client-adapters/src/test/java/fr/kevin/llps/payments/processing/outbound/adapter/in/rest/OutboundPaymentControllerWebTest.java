package fr.kevin.llps.payments.processing.outbound.adapter.in.rest;

import fr.kevin.llps.payments.processing.outbound.ChargesType;
import fr.kevin.llps.payments.processing.outbound.DetailFeeEstimate;
import fr.kevin.llps.payments.processing.outbound.OutboundPayment;
import fr.kevin.llps.payments.processing.outbound.OutboundPaymentQueryService;
import fr.kevin.llps.payments.processing.outbound.SearchFeeEstimate;
import fr.kevin.llps.payments.processing.outbound.port.in.OutboundPaymentQueries;
import fr.kevin.llps.payments.processing.outbound.port.out.OutboundPaymentSearchRepository;
import fr.kevin.llps.payments.shared.domain.AuditTimestamps;
import fr.kevin.llps.payments.shared.domain.DomainInvariantViolationException;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web slice of {@link OutboundPaymentController}; carried over from the legacy PaymentControllerWebTest.
 */
@WebMvcTest(controllers = OutboundPaymentController.class)
@Import(OutboundPaymentControllerWebTest.OutboundQueriesConfiguration.class)
class OutboundPaymentControllerWebTest {

    @Autowired
    private MockMvc mvc;

    // outbound: the real query service and both tariffs, only the repository port is mocked, so the fee tables
    // and the details contract stay asserted at the HTTP level
    @MockitoBean
    private OutboundPaymentSearchRepository outboundRepository;

    @TestConfiguration
    static class OutboundQueriesConfiguration {

        @Bean
        OutboundPaymentQueries outboundPaymentQueries(OutboundPaymentSearchRepository repository) {
            return new OutboundPaymentQueryService(repository, new SearchFeeEstimate(), new DetailFeeEstimate());
        }
    }

    @Test
    void outboundSearch_returnsRows() throws Exception {
        when(outboundRepository.search(any())).thenReturn(List.of(
                outbound("00000000-0000-0000-0000-000000000401", "1000.00", "OUR")));

        mvc.perform(get("/api/v1/outbound-payments/search").param("chargesType", "OUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("00000000-0000-0000-0000-000000000401"));
    }

    // moved from PaymentServiceUnitTest.outboundGetById_mapsRequestedExecutionDateAsIsoDate: that service method
    // was reachable from no endpoint; the same rendering and search fee are asserted on the search endpoint
    @Test
    void outboundSearch_mapsRequestedExecutionDateAsIsoDate() throws Exception {
        when(outboundRepository.search(any())).thenReturn(List.of(new OutboundPayment(
                PaymentId.parse("00000000-0000-0000-0000-000000000402"), null,
                Money.of(new BigDecimal("15600.75"), "EUR"), null, null, null, LocalDate.of(2026, 10, 17),
                new ChargesType("OUR"), "SENT", null)));

        mvc.perform(get("/api/v1/outbound-payments/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestedExecutionDate").value("2026-10-17"))
                .andExpect(jsonPath("$[0].estimatedFee").value(156.01));
    }

    @Test
    void outboundDetails_computesFeeFromController() throws Exception {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000401");
        when(outboundRepository.findById(id)).thenReturn(Optional.of(new OutboundPayment(id, null,
                Money.of(new BigDecimal("1000.00"), null), null, null, null, null, new ChargesType("OUR"), null,
                new AuditTimestamps(LocalDateTime.of(2026, 10, 16, 8, 0, 0), LocalDateTime.of(2026, 10, 16, 8, 1, 0)))));

        mvc.perform(get("/api/v1/outbound-payments/" + id.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedFee").value(20.00));
    }

    @ParameterizedTest(name = "[{index}] chargesType={0}, amount={1} -> detail fee {2}")
    @CsvSource({
            "OUR, 1000.00, 20.00",
            "OUR, 15600.75, 312.02",
            "SHA, 1000.00, 0.50",
            "BEN, 1000.00, 0",
            "XYZ, 1000.00, 0",
            ", 1000.00, 0"
    })
    void outboundDetails_feeEstimate_followsDetailFeeTable(String chargesType, String amount, double expectedFee)
            throws Exception {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000402");
        when(outboundRepository.findById(id)).thenReturn(Optional.of(outbound(id.value().toString(), amount, chargesType)));

        mvc.perform(get("/api/v1/outbound-payments/" + id.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estimatedFee").value(expectedFee));
    }

    // D-01: a stored payment without amount can no longer be reconstituted (the JPA mapper raises this exception);
    // the response stays a 500
    @Test
    void outboundDetails_ourWithoutAmount_returns500() throws Exception {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000402");
        when(outboundRepository.findById(id)).thenThrow(new DomainInvariantViolationException("Money amount is mandatory"));

        mvc.perform(get("/api/v1/outbound-payments/" + id.value())).andExpect(status().isInternalServerError());
    }

    @Test
    void outboundDetails_returnsSnakeCasePayloadWithAuditTimestamps() throws Exception {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000401");
        when(outboundRepository.findById(id)).thenReturn(Optional.of(new OutboundPayment(id, "OUT-2026-9001",
                Money.of(new BigDecimal("780.00"), "EUR"), "DE89370400440532013000", "ACME GmbH", "BNPAFRPP",
                LocalDate.of(2026, 10, 16), new ChargesType("SHA"), "SENT",
                new AuditTimestamps(LocalDateTime.of(2026, 10, 16, 8, 0, 0), LocalDateTime.of(2026, 10, 16, 8, 1, 0)))));

        mvc.perform(get("/api/v1/outbound-payments/" + id.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.value().toString()))
                .andExpect(jsonPath("$.instruction_id").value("OUT-2026-9001"))
                .andExpect(jsonPath("$.debtor_iban").value("DE89370400440532013000"))
                .andExpect(jsonPath("$.debtor_name").value("ACME GmbH"))
                .andExpect(jsonPath("$.destination_bic").value("BNPAFRPP"))
                .andExpect(jsonPath("$.requested_execution_date").exists())
                .andExpect(jsonPath("$.charges_type").value("SHA"))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.created_at").exists())
                .andExpect(jsonPath("$.updated_at").exists())
                .andExpect(jsonPath("$.estimatedFee").value(0.50))
                .andExpect(jsonPath("$.instructionId").doesNotExist())
                .andExpect(jsonPath("$.estimated_fee").doesNotExist());
    }

    @Test
    void outboundDetails_malformedId_returns404WithoutQuerying() throws Exception {
        mvc.perform(get("/api/v1/outbound-payments/not-a-uuid")).andExpect(status().isNotFound());

        verifyNoInteractions(outboundRepository);
    }

    @Test
    void outboundDetails_missing_returns404() throws Exception {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000404");
        when(outboundRepository.findById(id)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/outbound-payments/" + id.value())).andExpect(status().isNotFound());
    }

    @Test
    void outboundDetails_dbFailure_returns500() throws Exception {
        PaymentId id = PaymentId.parse("00000000-0000-0000-0000-000000000401");
        when(outboundRepository.findById(id)).thenThrow(new RuntimeException("db down"));

        mvc.perform(get("/api/v1/outbound-payments/" + id.value())).andExpect(status().isInternalServerError());
    }

    private static OutboundPayment outbound(String id, String amount, String chargesType) {
        return new OutboundPayment(PaymentId.parse(id), null, Money.of(new BigDecimal(amount), null), null, null, null,
                null, new ChargesType(chargesType), null, null);
    }
}
