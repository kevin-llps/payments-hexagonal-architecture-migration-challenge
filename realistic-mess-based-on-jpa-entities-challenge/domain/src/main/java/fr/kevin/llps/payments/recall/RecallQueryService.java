package fr.kevin.llps.payments.recall;

import fr.kevin.llps.payments.recall.port.in.RecallQueries;
import fr.kevin.llps.payments.recall.port.in.RecallSearchQuery;
import fr.kevin.llps.payments.recall.port.out.RecallSearchRepository;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.util.List;
import java.util.Optional;

public class RecallQueryService implements RecallQueries {

    private final RecallSearchRepository repository;

    public RecallQueryService(RecallSearchRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Recall> search(RecallSearchQuery query) {
        return repository.search(RecallSearchCriteria.from(query.recallType(), query.originalPaymentId(),
                query.originalPaymentType(), query.reasonCode(), query.status(),
                query.initiatedFrom(), query.initiatedTo(), query.limit()));
    }

    @Override
    public Optional<Recall> get(PaymentId id) {
        return repository.findById(id);
    }
}
