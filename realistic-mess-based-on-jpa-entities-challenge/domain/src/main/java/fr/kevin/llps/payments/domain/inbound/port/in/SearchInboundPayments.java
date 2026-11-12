package fr.kevin.llps.payments.domain.inbound.port.in;

import java.util.List;

import fr.kevin.llps.payments.domain.inbound.InboundPaymentView;
import fr.kevin.llps.payments.domain.inbound.query.InboundPaymentSearchQuery;

public interface SearchInboundPayments {

    List<InboundPaymentView> search(InboundPaymentSearchQuery query);
}
