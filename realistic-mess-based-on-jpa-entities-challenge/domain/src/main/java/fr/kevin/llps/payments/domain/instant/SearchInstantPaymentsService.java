package fr.kevin.llps.payments.domain.instant;

import java.util.List;

import fr.kevin.llps.payments.domain.instant.port.in.SearchInstantPayments;
import fr.kevin.llps.payments.domain.instant.port.out.LoadInstantPayments;
import fr.kevin.llps.payments.domain.instant.query.InstantPaymentSearchCriteria;
import fr.kevin.llps.payments.domain.instant.query.InstantPaymentSearchQuery;

public final class SearchInstantPaymentsService implements SearchInstantPayments {

    private final LoadInstantPayments loadInstantPayments;

    public SearchInstantPaymentsService(LoadInstantPayments loadInstantPayments) {
        this.loadInstantPayments = loadInstantPayments;
    }

    @Override
    public List<InstantPayment> search(InstantPaymentSearchQuery query) {
        InstantPaymentSearchCriteria criteria = InstantPaymentSearchCriteria.from(query.endToEndId(),
                query.debtorIban(), query.creditorIban(), query.status(), query.currency(), query.executionFrom(),
                query.executionTo(), query.limit());
        return loadInstantPayments.search(criteria);
    }
}
