package fr.kevin.llps.payments.processing.outbound;

import java.math.BigDecimal;

/**
 * A tariff estimating the fee of an outbound payment. Two tariffs coexist on purpose (search vs details, C-01).
 */
public interface OutboundFeeEstimate {

    BigDecimal estimate(OutboundPayment payment);
}
