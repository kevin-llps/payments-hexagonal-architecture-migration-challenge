package fr.kevin.llps.payments.domain.outbound.port.out;

import java.util.List;
import java.util.Optional;

import fr.kevin.llps.payments.domain.outbound.OutboundPayment;
import fr.kevin.llps.payments.domain.outbound.query.OutboundPaymentSearchCriteria;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

public interface LoadOutboundPayments {

    /** Matching payments ordered by requested execution date descending, at most the criteria limit. */
    List<OutboundPayment> search(OutboundPaymentSearchCriteria criteria);

    Optional<OutboundPayment> findById(PaymentId id);
}
