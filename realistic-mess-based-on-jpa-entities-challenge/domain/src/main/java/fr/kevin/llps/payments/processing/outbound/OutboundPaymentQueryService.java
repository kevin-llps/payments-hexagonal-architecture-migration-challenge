package fr.kevin.llps.payments.processing.outbound;

import fr.kevin.llps.payments.processing.outbound.port.in.OutboundPaymentQueries;
import fr.kevin.llps.payments.processing.outbound.port.in.OutboundPaymentSearchQuery;
import fr.kevin.llps.payments.processing.outbound.port.in.PricedOutboundPayment;
import fr.kevin.llps.payments.processing.outbound.port.out.OutboundPaymentSearchRepository;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.util.List;
import java.util.Optional;

public class OutboundPaymentQueryService implements OutboundPaymentQueries {

    private final OutboundPaymentSearchRepository repository;
    private final OutboundFeeEstimate searchFee;
    private final OutboundFeeEstimate detailFee;

    public OutboundPaymentQueryService(OutboundPaymentSearchRepository repository,
                                       OutboundFeeEstimate searchFee,
                                       OutboundFeeEstimate detailFee) {
        this.repository = repository;
        this.searchFee = searchFee;
        this.detailFee = detailFee;
    }

    @Override
    public List<PricedOutboundPayment> search(OutboundPaymentSearchQuery query) {
        return repository.search(OutboundPaymentSearchCriteria.from(query.debtorIban(), query.destinationBic(),
                        query.chargesType(), query.status(), query.executionFrom(), query.executionTo(), query.limit()))
                .stream()
                .map(payment -> new PricedOutboundPayment(payment, searchFee.estimate(payment)))
                .toList();
    }

    @Override
    public Optional<PricedOutboundPayment> get(PaymentId id) {
        return repository.findById(id).map(payment -> new PricedOutboundPayment(payment, detailFee.estimate(payment)));
    }
}
