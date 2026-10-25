package fr.kevin.llps.payments.processing.instant.port.out;

import fr.kevin.llps.payments.processing.instant.InstantPayment;
import fr.kevin.llps.payments.processing.instant.InstantPaymentSearchCriteria;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.util.List;
import java.util.Optional;

public interface InstantPaymentSearchRepository {

    List<InstantPayment> search(InstantPaymentSearchCriteria criteria);

    Optional<InstantPayment> findById(PaymentId id);
}
