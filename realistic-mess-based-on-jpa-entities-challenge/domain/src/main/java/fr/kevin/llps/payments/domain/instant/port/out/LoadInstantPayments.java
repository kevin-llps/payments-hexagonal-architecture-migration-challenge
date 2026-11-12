package fr.kevin.llps.payments.domain.instant.port.out;

import java.util.List;
import java.util.Optional;

import fr.kevin.llps.payments.domain.instant.InstantPayment;
import fr.kevin.llps.payments.domain.instant.query.InstantPaymentSearchCriteria;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

public interface LoadInstantPayments {

    /** Matching payments ordered by {@code executionDatetime} descending, at most the criteria limit. */
    List<InstantPayment> search(InstantPaymentSearchCriteria criteria);

    Optional<InstantPayment> findById(PaymentId id);
}
