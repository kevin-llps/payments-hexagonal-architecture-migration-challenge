package fr.kevin.llps.payments.processing.outbound;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fee shown on outbound payment details: OUR 2 %, SHA a flat 0.50, any other charges type nothing.
 */
public class DetailFeeEstimate implements OutboundFeeEstimate {

    private static final BigDecimal OUR_RATE = new BigDecimal("0.02");
    private static final String SHA_FEE = "0.50";

    @Override
    public BigDecimal estimate(OutboundPayment payment) {
        if (payment.chargesType().isOur()) {
            return payment.amount().amount().multiply(OUR_RATE).setScale(2, RoundingMode.HALF_UP);
        }
        if (payment.chargesType().isSha()) {
            return new BigDecimal(SHA_FEE);
        }
        return BigDecimal.ZERO;
    }
}
