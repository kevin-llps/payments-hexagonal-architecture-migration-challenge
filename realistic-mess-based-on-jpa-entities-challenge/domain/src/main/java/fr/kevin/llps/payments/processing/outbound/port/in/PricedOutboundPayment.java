package fr.kevin.llps.payments.processing.outbound.port.in;

import fr.kevin.llps.payments.processing.outbound.OutboundPayment;

import java.math.BigDecimal;

/**
 * An outbound payment with the fee estimated by the tariff of the use case (search or details).
 */
public record PricedOutboundPayment(OutboundPayment payment, BigDecimal estimatedFee) {
}
