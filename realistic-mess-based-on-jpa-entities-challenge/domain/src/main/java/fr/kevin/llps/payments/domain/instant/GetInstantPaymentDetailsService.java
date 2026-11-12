package fr.kevin.llps.payments.domain.instant;

import java.util.Optional;

import fr.kevin.llps.payments.domain.instant.port.in.GetInstantPaymentDetails;
import fr.kevin.llps.payments.domain.instant.port.out.LoadInstantPayments;
import fr.kevin.llps.payments.domain.sharedkernel.PaymentId;

public final class GetInstantPaymentDetailsService implements GetInstantPaymentDetails {

    private final LoadInstantPayments loadInstantPayments;

    public GetInstantPaymentDetailsService(LoadInstantPayments loadInstantPayments) {
        this.loadInstantPayments = loadInstantPayments;
    }

    @Override
    public Optional<InstantPayment> getDetails(PaymentId id) {
        return loadInstantPayments.findById(id);
    }
}
