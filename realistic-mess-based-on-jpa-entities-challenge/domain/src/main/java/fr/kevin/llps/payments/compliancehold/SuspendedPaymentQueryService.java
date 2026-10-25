package fr.kevin.llps.payments.compliancehold;

import fr.kevin.llps.payments.compliancehold.port.in.SuspendedPaymentQueries;
import fr.kevin.llps.payments.compliancehold.port.in.SuspendedPaymentSearchQuery;
import fr.kevin.llps.payments.compliancehold.port.out.SuspendedPaymentSearchRepository;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.util.List;
import java.util.Optional;

public class SuspendedPaymentQueryService implements SuspendedPaymentQueries {

    private final SuspendedPaymentSearchRepository repository;

    public SuspendedPaymentQueryService(SuspendedPaymentSearchRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SuspendedPayment> search(SuspendedPaymentSearchQuery query) {
        return repository.search(SuspendedPaymentSearchCriteria.from(query.debtorIban(), query.holdReason(),
                query.reviewStatus(), query.heldFrom(), query.heldTo(), query.limit()));
    }

    @Override
    public Optional<SuspendedPayment> get(PaymentId id) {
        return repository.findById(id);
    }
}
