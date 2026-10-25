package fr.kevin.llps.payments.processing.outbound;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static fr.kevin.llps.payments.processing.outbound.SearchFeeEstimateTest.payment;
import static org.assertj.core.api.Assertions.assertThat;

class DetailFeeEstimateTest {

    private final DetailFeeEstimate detailFee = new DetailFeeEstimate();

    @ParameterizedTest(name = "[{index}] OUR {0} -> {1}")
    @CsvSource({"1000.00, 20.00", "15600.75, 312.02", "0.25, 0.01"})
    void estimate_should_charge_two_percent_rounded_half_up_for_our(String amount, String expectedFee) {
        // GIVEN / WHEN
        BigDecimal fee = detailFee.estimate(payment(amount, "OUR"));

        // THEN
        assertThat(fee).isEqualTo(new BigDecimal(expectedFee));
    }

    @Test
    void estimate_should_charge_a_flat_0_50_for_sha() {
        // GIVEN / WHEN
        BigDecimal fee = detailFee.estimate(payment("1000.00", "SHA"));

        // THEN the exact legacy value (scale 2)
        assertThat(fee).isEqualTo(new BigDecimal("0.50"));
    }

    @ParameterizedTest(name = "[{index}] chargesType=\"{0}\"")
    @CsvSource({"BEN", "XYZ", "sha", "''", ","})
    void estimate_should_charge_nothing_for_any_other_charges_type(String chargesType) {
        // GIVEN / WHEN
        BigDecimal fee = detailFee.estimate(payment("1000.00", chargesType));

        // THEN the exact legacy value (scale 0)
        assertThat(fee).isEqualTo(BigDecimal.ZERO);
    }
}
