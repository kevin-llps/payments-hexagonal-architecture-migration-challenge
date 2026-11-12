package fr.kevin.llps.payments.domain.inbound.port.in;

import java.util.Optional;

import fr.kevin.llps.payments.domain.inbound.InboundPaymentView;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

public interface GetInboundPaymentDetails {

    Optional<InboundPaymentView> getDetails(PaymentId id);
}
