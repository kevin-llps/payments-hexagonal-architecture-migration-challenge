package fr.kevin.llps.payments.processing.inbound;

import fr.kevin.llps.payments.processing.inbound.port.in.InboundPaymentQueries;
import fr.kevin.llps.payments.processing.inbound.port.in.InboundPaymentSearchQuery;
import fr.kevin.llps.payments.processing.inbound.port.in.InboundPaymentView;
import fr.kevin.llps.payments.processing.inbound.port.out.InboundPaymentSearchRepository;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class InboundPaymentQueryService implements InboundPaymentQueries {

    private final InboundPaymentSearchRepository repository;
    private final Clock clock;

    public InboundPaymentQueryService(InboundPaymentSearchRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public List<InboundPaymentView> search(InboundPaymentSearchQuery query) {
        LocalDate today = LocalDate.now(clock);
        return repository.search(InboundPaymentSearchCriteria.from(query.creditorIban(), query.originBic(),
                        query.status(), query.currency(), query.valueDateFrom(), query.valueDateTo(), query.limit()))
                .stream()
                .map(payment -> new InboundPaymentView(payment, payment.ageInDays(today)))
                .toList();
    }

    @Override
    public Optional<InboundPaymentView> get(PaymentId id) {
        LocalDate today = LocalDate.now(clock);
        return repository.findById(id).map(payment -> new InboundPaymentView(payment, payment.ageInDays(today)));
    }
}
