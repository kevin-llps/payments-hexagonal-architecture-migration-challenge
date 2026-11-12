package fr.kevin.llps.payments.domain.suspended.port.out;

import java.util.List;
import java.util.Optional;

import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.suspended.SuspendedPayment;
import fr.kevin.llps.payments.domain.suspended.query.SuspendedPaymentSearchCriteria;

public interface LoadSuspendedPayments {

    /** Matching payments ordered by {@code heldAt} descending, at most the criteria limit. */
    List<SuspendedPayment> search(SuspendedPaymentSearchCriteria criteria);

    Optional<SuspendedPayment> findById(PaymentId id);
}
