package fr.kevin.llps.payments.domain.inbound.port.out;

import java.util.List;
import java.util.Optional;

import fr.kevin.llps.payments.domain.inbound.InboundPayment;
import fr.kevin.llps.payments.domain.inbound.query.InboundPaymentSearchCriteria;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

public interface LoadInboundPayments {

    /** Matching payments ordered by value date then creation time, both descending, at most the criteria limit. */
    List<InboundPayment> search(InboundPaymentSearchCriteria criteria);

    Optional<InboundPayment> findById(PaymentId id);
}
