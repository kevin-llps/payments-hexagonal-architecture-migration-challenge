package fr.kevin.llps.payments.domain.outbound.port.in;

import java.util.Optional;

import fr.kevin.llps.payments.domain.outbound.OutboundPaymentWithFee;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

public interface GetOutboundPaymentDetails {

    /** The payment with the details fee (BR-OUT-07). */
    Optional<OutboundPaymentWithFee> getDetails(PaymentId id);
}
