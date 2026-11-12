package fr.kevin.llps.payments.domain.outbound;

import java.util.List;

import fr.kevin.llps.payments.domain.outbound.port.in.SearchOutboundPayments;
import fr.kevin.llps.payments.domain.outbound.port.out.LoadOutboundPayments;
import fr.kevin.llps.payments.domain.outbound.query.OutboundPaymentSearchCriteria;
import fr.kevin.llps.payments.domain.outbound.query.OutboundPaymentSearchQuery;

public final class SearchOutboundPaymentsService implements SearchOutboundPayments {

    private final LoadOutboundPayments loadOutboundPayments;
    private final SearchFeePolicy searchFeePolicy;

    public SearchOutboundPaymentsService(LoadOutboundPayments loadOutboundPayments, SearchFeePolicy searchFeePolicy) {
        this.loadOutboundPayments = loadOutboundPayments;
        this.searchFeePolicy = searchFeePolicy;
    }

    @Override
    public List<OutboundPaymentWithFee> search(OutboundPaymentSearchQuery query) {
        OutboundPaymentSearchCriteria criteria = OutboundPaymentSearchCriteria.from(query.debtorIban(),
                query.destinationBic(), query.chargesType(), query.status(), query.executionFrom(),
                query.executionTo(), query.limit());
        return loadOutboundPayments.search(criteria).stream()
                .map(payment -> new OutboundPaymentWithFee(payment, searchFeePolicy.estimate(payment)))
                .toList();
    }
}
