package fr.kevin.llps.payments.processing.outbound;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fee shown in outbound search results: OUR 1 %, BEN nothing, any other charges type a flat 0.30.
 */
public class SearchFeeEstimate implements OutboundFeeEstimate {

    private static final BigDecimal OUR_RATE = new BigDecimal("0.01");
    private static final String FLAT_FEE = "0.30";

    @Override
    public BigDecimal estimate(OutboundPayment payment) {
        if (payment.chargesType().isOur()) {
            return payment.amount().amount().multiply(OUR_RATE).setScale(2, RoundingMode.HALF_UP);
        }
        if (payment.chargesType().isBen()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(FLAT_FEE);
    }
}
