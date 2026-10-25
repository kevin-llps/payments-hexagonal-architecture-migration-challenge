package fr.kevin.llps.payments.processing.instant.port.in;

import fr.kevin.llps.payments.processing.instant.InstantPayment;
import fr.kevin.llps.payments.shared.domain.PaymentId;

import java.util.List;
import java.util.Optional;

public interface InstantPaymentQueries {

    List<InstantPayment> search(InstantPaymentSearchQuery query);

    Optional<InstantPayment> get(PaymentId id);
}
