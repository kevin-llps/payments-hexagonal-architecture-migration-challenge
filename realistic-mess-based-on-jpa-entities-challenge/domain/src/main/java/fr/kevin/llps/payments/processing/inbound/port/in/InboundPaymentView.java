package fr.kevin.llps.payments.processing.inbound.port.in;

import fr.kevin.llps.payments.processing.inbound.InboundPayment;

/**
 * An inbound payment with its age computed for today.
 */
public record InboundPaymentView(InboundPayment payment, Long ageInDays) {
}
