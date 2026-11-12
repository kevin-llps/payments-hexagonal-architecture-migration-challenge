package fr.kevin.llps.payments.domain.outbound;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fee shown by the outbound details (BR-OUT-07): {@code OUR} 2 % rounded half-up to 2 decimals (no amount: not
 * computable); {@code SHA} 0.50; anything else 0. Intentionally differs from {@link SearchFeePolicy} (BR-OUT-08, C-01).
 */
public final class DetailsFeePolicy {

    private static final BigDecimal OUR_RATE = new BigDecimal("0.02");
    private static final BigDecimal SHA_FEE = new BigDecimal("0.50");
    private static final int FEE_SCALE = 2;
    private static final String NO_AMOUNT_MESSAGE = "OUR details fee needs an amount";

    public BigDecimal estimate(OutboundPayment payment) {
        ChargesType chargesType = new ChargesType(payment.chargesType().orElse(null));
        if (chargesType.is(ChargesType.OUR)) {
            BigDecimal amount = payment.money().amount()
                    .orElseThrow(() -> new FeeNotComputableException(NO_AMOUNT_MESSAGE));
            return amount.multiply(OUR_RATE).setScale(FEE_SCALE, RoundingMode.HALF_UP);
        }
        if (chargesType.is(ChargesType.SHA)) {
            return SHA_FEE;
        }
        return BigDecimal.ZERO;
    }
}
