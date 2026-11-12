package fr.kevin.llps.payments.domain.outbound;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;

class SearchFeePolicyTest {

    private final SearchFeePolicy searchFeePolicy = new SearchFeePolicy();

    @ParameterizedTest(name = "[{index}] {1} on {0} -> search fee {2}")
    @CsvSource(value = {
            "1000.00, OUR, 10.00",
            "15600.75, OUR, 156.01",
            "0.50, OUR, 0.01",
            "780.00, SHA, 0.30",
            "95.00, BEN, 0",
            "95.00, XYZ, 0.30",
            "95.00, NULL, 0.30",
            "NULL, OUR, 0",
            "NULL, NULL, 0",
            "1000.00, SHA, 0.30",
            "1000.00, BEN, 0.00",
            "NULL, SHA, 0.00"
    }, nullValues = "NULL")
    void should_compute_search_fee_with_service_fee_table(String amount, String chargesType, String expectedFee) {
        // GIVEN (remapped from PaymentServiceUnitTest.should_compute_search_fee_with_service_fee_table, same 9 rows,
        // plus the SHA / BEN / no-amount rows of outboundSearch_computesFeePerChargesType)
        OutboundPayment payment = payment(amount, chargesType);

        // WHEN
        BigDecimal actual = searchFeePolicy.estimate(payment);

        // THEN
        assertThat(actual).isNotNull().isEqualByComparingTo(expectedFee);
    }

    @Test
    void should_keep_search_fee_scale_of_two_for_our_payments() {
        // GIVEN (remapped from PaymentServiceUnitTest.should_keep_search_fee_scale_of_two_for_our_payments)
        OutboundPayment payment = payment("15600.75", "OUR");

        // WHEN
        BigDecimal actual = searchFeePolicy.estimate(payment);

        // THEN
        assertThat(actual).isEqualTo(new BigDecimal("156.01"));
    }

    @Test
    void should_return_the_exact_default_fee_for_other_charges_types() {
        // GIVEN (BR-OUT-06: 0.30 rendered with scale 2, as the legacy constant)
        OutboundPayment payment = payment("780.00", "SHA");

        // WHEN
        BigDecimal actual = searchFeePolicy.estimate(payment);

        // THEN
        assertThat(actual).isEqualTo(new BigDecimal("0.30"));
    }

    private static OutboundPayment payment(String amount, String chargesType) {
        return OutboundPayment.builder(PaymentId.parse("00000000-0000-0000-0000-000000000415"))
                .money(Money.of(amount == null ? null : new BigDecimal(amount), "EUR"))
                .chargesType(chargesType)
                .status("SENT")
                .build();
    }
}
