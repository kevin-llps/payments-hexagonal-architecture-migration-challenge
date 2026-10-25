package fr.kevin.llps.payments.processing.outbound;

import fr.kevin.llps.payments.processing.outbound.port.in.OutboundPaymentSearchQuery;
import fr.kevin.llps.payments.processing.outbound.port.in.PricedOutboundPayment;
import fr.kevin.llps.payments.processing.outbound.port.out.OutboundPaymentSearchRepository;
import fr.kevin.llps.payments.shared.domain.DomainInvariantViolationException;
import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Characterization tests of the legacy service behavior (Phase 2), carried over from the legacy
 * PaymentServiceUnitTest and re-targeted to {@link OutboundPaymentQueryService}.
 */
@ExtendWith(MockitoExtension.class)
class OutboundPaymentQueryServiceCharacterizationTest {

    @Mock
    private OutboundPaymentSearchRepository outboundRepository;
    private OutboundPaymentQueryService outboundQueries;

    @BeforeEach
    void wireOutboundQueries() {
        outboundQueries = new OutboundPaymentQueryService(outboundRepository, new SearchFeeEstimate(),
                new DetailFeeEstimate());
    }

    @Test
    void outboundSearch_computesFeePerChargesType() {
        OutboundPayment our = outboundPayment("00000000-0000-0000-0000-000000000411", "1000.00", "OUR");
        OutboundPayment sha = outboundPayment("00000000-0000-0000-0000-000000000412", "1000.00", "SHA");
        OutboundPayment ben = outboundPayment("00000000-0000-0000-0000-000000000413", "1000.00", "BEN");
        when(outboundRepository.search(any())).thenReturn(List.of(our, sha, ben));

        List<PricedOutboundPayment> result = outboundQueries.search(outboundQuery());

        assertThat(result.get(0).estimatedFee()).isEqualByComparingTo("10.00");
        assertThat(result.get(1).estimatedFee()).isEqualByComparingTo("0.30");
        assertThat(result.get(2).estimatedFee()).isEqualByComparingTo("0.00");
    }

    // D-01 (approved): replaces the null-amount row of outboundSearch_computesFeePerChargesType and the
    // "OUR, , 0.00" row of outboundSearch_feeEstimate_followsSearchFeeTable — a payment without amount is rejected
    @Test
    void outboundSearch_paymentWithoutAmount_isRejected() {
        assertThatThrownBy(() -> outboundPayment("00000000-0000-0000-0000-000000000414", null, "SHA"))
                .isInstanceOf(DomainInvariantViolationException.class);
    }

    @ParameterizedTest(name = "[{index}] chargesType={0}, amount={1} -> search fee {2}")
    @CsvSource({
            "OUR, 1000.00, 10.00",
            "OUR, 1234.56, 12.35",
            "OUR, 0.50, 0.01",
            "OUR, 15600.75, 156.01",
            "BEN, 1000.00, 0.00",
            "SHA, 1000.00, 0.30",
            "XYZ, 1000.00, 0.30",
            "'', 1000.00, 0.30",
            ", 1000.00, 0.30"
    })
    void outboundSearch_feeEstimate_followsSearchFeeTable(String chargesType, String amount, String expectedFee) {
        when(outboundRepository.search(any())).thenReturn(List.of(
                outboundPayment("00000000-0000-0000-0000-000000000415", amount, chargesType)));

        PricedOutboundPayment response = outboundQueries.search(outboundQuery()).get(0);

        assertThat(response.estimatedFee()).isEqualByComparingTo(expectedFee);
    }

    // outboundGetById_mapsRequestedExecutionDateAsIsoDate: OutboundPaymentService.getById was reachable from no
    // endpoint and was not migrated (approved); its date rendering and 156.01 search fee are asserted by
    // PaymentControllerWebTest.outboundSearch_mapsRequestedExecutionDateAsIsoDate and the "OUR, 15600.75" row above.

    // details are priced with the detail tariff, as the details endpoint always was
    @Test
    void outboundGetById_foundAndMissing() {
        PaymentId found = PaymentId.parse("00000000-0000-0000-0000-000000000401");
        PaymentId missing = PaymentId.parse("00000000-0000-0000-0000-000000000499");
        when(outboundRepository.findById(found))
                .thenReturn(Optional.of(outboundPayment(found.value().toString(), "10.00", "SHA")));
        when(outboundRepository.findById(missing)).thenReturn(Optional.empty());

        assertThat(outboundQueries.get(found)).isPresent();
        assertThat(outboundQueries.get(missing)).isEmpty();
    }

    private static OutboundPaymentSearchQuery outboundQuery() {
        return new OutboundPaymentSearchQuery(null, null, null, null, null, null, null);
    }

    private static OutboundPayment outboundPayment(String id, String amount, String chargesType) {
        return new OutboundPayment(PaymentId.parse(id), null,
                Money.of(amount == null ? null : new BigDecimal(amount), "EUR"), null, null, null, null,
                new ChargesType(chargesType), "SENT", null);
    }
}
