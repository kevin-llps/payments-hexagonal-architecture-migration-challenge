package fr.kevin.llps.payments.processing.outbound;

import fr.kevin.llps.payments.shared.domain.Money;
import fr.kevin.llps.payments.shared.domain.PaymentId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SearchFeeEstimateTest {

    private final SearchFeeEstimate searchFee = new SearchFeeEstimate();

    @ParameterizedTest(name = "[{index}] OUR {0} -> {1}")
    @CsvSource({"1000.00, 10.00", "1234.56, 12.35", "0.50, 0.01", "15600.75, 156.01"})
    void estimate_should_charge_one_percent_rounded_half_up_for_our(String amount, String expectedFee) {
        // GIVEN / WHEN
        BigDecimal fee = searchFee.estimate(payment(amount, "OUR"));

        // THEN
        assertThat(fee).isEqualTo(new BigDecimal(expectedFee));
    }

    @Test
    void estimate_should_charge_nothing_for_ben() {
        // GIVEN / WHEN
        BigDecimal fee = searchFee.estimate(payment("1000.00", "BEN"));

        // THEN the exact legacy value (scale 0)
        assertThat(fee).isEqualTo(BigDecimal.ZERO);
    }

    @ParameterizedTest(name = "[{index}] chargesType=\"{0}\"")
    @CsvSource({"SHA", "XYZ", "our", "''", "'null'", ","})
    void estimate_should_charge_a_flat_0_30_for_any_other_charges_type(String chargesType) {
        // GIVEN / WHEN
        BigDecimal fee = searchFee.estimate(payment("1000.00", chargesType));

        // THEN the exact legacy value (scale 2)
        assertThat(fee).isEqualTo(new BigDecimal("0.30"));
    }

    static OutboundPayment payment(String amount, String chargesType) {
        return new OutboundPayment(PaymentId.parse("00000000-0000-0000-0000-000000000401"), "OUT-1",
                Money.of(new BigDecimal(amount), "EUR"), null, null, null, null, new ChargesType(chargesType), "SENT",
                null);
    }
}
