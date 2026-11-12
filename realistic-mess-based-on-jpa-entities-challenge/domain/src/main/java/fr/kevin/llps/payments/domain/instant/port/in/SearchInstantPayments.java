package fr.kevin.llps.payments.domain.instant.port.in;

import java.util.List;

import fr.kevin.llps.payments.domain.instant.InstantPayment;
import fr.kevin.llps.payments.domain.instant.query.InstantPaymentSearchQuery;

public interface SearchInstantPayments {

    List<InstantPayment> search(InstantPaymentSearchQuery query);
}
