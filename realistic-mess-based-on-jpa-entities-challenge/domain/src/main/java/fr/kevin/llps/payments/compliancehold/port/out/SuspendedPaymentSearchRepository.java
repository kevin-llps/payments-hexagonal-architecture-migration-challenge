package fr.kevin.llps.payments.compliancehold.port.out;

import fr.kevin.llps.payments.compliancehold.SuspendedPayment;
import fr.kevin.llps.payments.compliancehold.SuspendedPaymentSearchCriteria;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.util.List;
import java.util.Optional;

public interface SuspendedPaymentSearchRepository {

    List<SuspendedPayment> search(SuspendedPaymentSearchCriteria criteria);

    Optional<SuspendedPayment> findById(PaymentId id);
}
