package fr.kevin.llps.payments.domain.outbound.port.in;

import java.util.List;

import fr.kevin.llps.payments.domain.outbound.OutboundPaymentWithFee;
import fr.kevin.llps.payments.domain.outbound.query.OutboundPaymentSearchQuery;

public interface SearchOutboundPayments {

    /** Matching payments with the search fee (BR-OUT-06). */
    List<OutboundPaymentWithFee> search(OutboundPaymentSearchQuery query);
}
