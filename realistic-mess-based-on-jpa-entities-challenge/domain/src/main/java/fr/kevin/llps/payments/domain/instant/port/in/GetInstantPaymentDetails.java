package fr.kevin.llps.payments.domain.instant.port.in;

import java.util.Optional;

import fr.kevin.llps.payments.domain.instant.InstantPayment;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

public interface GetInstantPaymentDetails {

    Optional<InstantPayment> getDetails(PaymentId id);
}
