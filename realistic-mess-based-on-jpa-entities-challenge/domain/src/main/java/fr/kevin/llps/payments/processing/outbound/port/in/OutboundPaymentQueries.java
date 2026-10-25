package fr.kevin.llps.payments.processing.outbound.port.in;

import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.util.List;
import java.util.Optional;

public interface OutboundPaymentQueries {

    /**
     * Matching payments priced with the search tariff.
     */
    List<PricedOutboundPayment> search(OutboundPaymentSearchQuery query);

    /**
     * The payment priced with the details tariff.
     */
    Optional<PricedOutboundPayment> get(PaymentId id);
}
