package fr.kevin.llps.payments.domain.suspended;

import java.util.Optional;

import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;
import fr.kevin.llps.payments.domain.suspended.port.in.GetSuspendedPaymentDetails;
import fr.kevin.llps.payments.domain.suspended.port.out.LoadSuspendedPayments;

public final class GetSuspendedPaymentDetailsService implements GetSuspendedPaymentDetails {

    private final LoadSuspendedPayments loadSuspendedPayments;

    public GetSuspendedPaymentDetailsService(LoadSuspendedPayments loadSuspendedPayments) {
        this.loadSuspendedPayments = loadSuspendedPayments;
    }

    @Override
    public Optional<SuspendedPayment> getDetails(PaymentId id) {
        return loadSuspendedPayments.findById(id);
    }
}
