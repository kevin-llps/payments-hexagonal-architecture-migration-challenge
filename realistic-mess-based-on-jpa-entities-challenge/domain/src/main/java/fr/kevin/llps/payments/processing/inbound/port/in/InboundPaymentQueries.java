package fr.kevin.llps.payments.processing.inbound.port.in;

import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.util.List;
import java.util.Optional;

public interface InboundPaymentQueries {

    List<InboundPaymentView> search(InboundPaymentSearchQuery query);

    Optional<InboundPaymentView> get(PaymentId id);
}
