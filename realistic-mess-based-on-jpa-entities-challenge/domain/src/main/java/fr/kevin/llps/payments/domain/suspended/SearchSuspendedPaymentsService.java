package fr.kevin.llps.payments.domain.suspended;

import java.util.List;

import fr.kevin.llps.payments.domain.suspended.port.in.SearchSuspendedPayments;
import fr.kevin.llps.payments.domain.suspended.port.out.LoadSuspendedPayments;
import fr.kevin.llps.payments.domain.suspended.query.SuspendedPaymentSearchCriteria;
import fr.kevin.llps.payments.domain.suspended.query.SuspendedPaymentSearchQuery;

public final class SearchSuspendedPaymentsService implements SearchSuspendedPayments {

    private final LoadSuspendedPayments loadSuspendedPayments;

    public SearchSuspendedPaymentsService(LoadSuspendedPayments loadSuspendedPayments) {
        this.loadSuspendedPayments = loadSuspendedPayments;
    }

    @Override
    public List<SuspendedPayment> search(SuspendedPaymentSearchQuery query) {
        SuspendedPaymentSearchCriteria criteria = SuspendedPaymentSearchCriteria.from(query.debtorIban(),
                query.holdReason(), query.reviewStatus(), query.heldFrom(), query.heldTo(), query.limit());
        return loadSuspendedPayments.search(criteria);
    }
}
