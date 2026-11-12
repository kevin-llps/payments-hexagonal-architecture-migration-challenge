package fr.kevin.llps.payments.domain.suspended.port.in;

import java.util.Optional;

import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.suspended.SuspendedPayment;

public interface GetSuspendedPaymentDetails {

    Optional<SuspendedPayment> getDetails(PaymentId id);
}
