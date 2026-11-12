package fr.kevin.llps.payments.domain.outbound;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * Fee shown by the outbound search (BR-OUT-06): no amount gives 0; {@code OUR} 1 % rounded half-up to 2 decimals;
 * {@code BEN} 0; anything else 0.30. Intentionally differs from {@link DetailsFeePolicy} (BR-OUT-08, C-01).
 */
public final class SearchFeePolicy {

    private static final BigDecimal OUR_RATE = new BigDecimal("0.01");
    private static final BigDecimal DEFAULT_FEE = new BigDecimal("0.30");
    private static final int FEE_SCALE = 2;

    public BigDecimal estimate(OutboundPayment payment) {
        Optional<BigDecimal> amount = payment.money().amount();
        if (amount.isEmpty()) {
            return BigDecimal.ZERO;
        }
        ChargesType chargesType = new ChargesType(payment.chargesType().orElse(null));
        if (chargesType.is(ChargesType.OUR)) {
            return amount.get().multiply(OUR_RATE).setScale(FEE_SCALE, RoundingMode.HALF_UP);
        }
        if (chargesType.is(ChargesType.BEN)) {
            return BigDecimal.ZERO;
        }
        return DEFAULT_FEE;
    }
}
