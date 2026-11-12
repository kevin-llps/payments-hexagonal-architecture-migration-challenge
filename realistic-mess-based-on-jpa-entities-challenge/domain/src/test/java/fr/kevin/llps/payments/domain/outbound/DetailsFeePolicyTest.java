package fr.kevin.llps.payments.domain.outbound;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import fr.kevin.llps.payments.domain.sharedkernel.Money;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class DetailsFeePolicyTest {

    private final DetailsFeePolicy detailsFeePolicy = new DetailsFeePolicy();

    @ParameterizedTest(name = "[{index}] {1} on {0} -> details fee {2}")
    @CsvSource(value = {
            "1000.00, OUR, 20.00",
            "15600.75, OUR, 312.02",
            "0.25, OUR, 0.01",
            "780.00, SHA, 0.50",
            "95.00, BEN, 0",
            "95.00, XYZ, 0",
            "95.00, NULL, 0"
    }, nullValues = "NULL")
    void should_compute_details_fee_with_controller_fee_table(String amount, String chargesType, String expectedFee) {
        // GIVEN (remapped from PaymentControllerWebTest.should_compute_details_fee_with_controller_fee_table, same rows)
        OutboundPayment payment = payment(amount, chargesType);

        // WHEN
        BigDecimal actual = detailsFeePolicy.estimate(payment);

        // THEN
        assertThat(actual).isNotNull().isEqualByComparingTo(expectedFee);
    }

    @Test
    void should_return_fixed_details_fee_when_sha_payment_has_no_amount() {
        // GIVEN (remapped from PaymentControllerWebTest.should_return_fixed_details_fee_when_sha_payment_has_no_amount)
        OutboundPayment payment = payment(null, "SHA");

        // WHEN
        BigDecimal actual = detailsFeePolicy.estimate(payment);

        // THEN
        assertThat(actual).isEqualTo(new BigDecimal("0.50"));
    }

    @Test
    void should_not_compute_the_details_fee_of_an_our_payment_without_amount() {
        // GIVEN (remapped from PaymentControllerWebTest.should_return_500_when_details_fee_is_computed_for_our_payment_without_amount, domain part)
        OutboundPayment payment = payment(null, "OUR");

        // WHEN
        Throwable actual = catchThrowable(() -> detailsFeePolicy.estimate(payment));

        // THEN
        assertThat(actual).isInstanceOf(FeeNotComputableException.class);
    }

    @Test
    void should_keep_details_fee_scale_of_two_for_our_payments() {
        // GIVEN (BR-OUT-07 exact BigDecimal)
        OutboundPayment payment = payment("15600.75", "OUR");

        // WHEN
        BigDecimal actual = detailsFeePolicy.estimate(payment);

        // THEN
        assertThat(actual).isEqualTo(new BigDecimal("312.02"));
    }

    private static OutboundPayment payment(String amount, String chargesType) {
        return OutboundPayment.builder(PaymentId.parse("00000000-0000-0000-0000-000000000402"))
                .money(Money.of(amount == null ? null : new BigDecimal(amount), "EUR"))
                .chargesType(chargesType)
                .build();
    }
}
