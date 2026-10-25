package fr.kevin.llps.payments.processing.instant;

import fr.kevin.llps.payments.processing.instant.port.in.InstantPaymentQueries;
import fr.kevin.llps.payments.processing.instant.port.in.InstantPaymentSearchQuery;
import fr.kevin.llps.payments.processing.instant.port.out.InstantPaymentSearchRepository;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.util.List;
import java.util.Optional;

public class InstantPaymentQueryService implements InstantPaymentQueries {

    private final InstantPaymentSearchRepository repository;

    public InstantPaymentQueryService(InstantPaymentSearchRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<InstantPayment> search(InstantPaymentSearchQuery query) {
        return repository.search(InstantPaymentSearchCriteria.from(query.endToEndId(), query.debtorIban(),
                query.creditorIban(), query.status(), query.currency(), query.executionFrom(), query.executionTo(),
                query.limit()));
    }

    @Override
    public Optional<InstantPayment> get(PaymentId id) {
        return repository.findById(id);
    }
}
